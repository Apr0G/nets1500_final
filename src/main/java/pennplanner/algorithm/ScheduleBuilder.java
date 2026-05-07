package pennplanner.algorithm;

import pennplanner.model.*;

import java.util.*;

public class ScheduleBuilder {

    private final Map<String, RawCourse> allCourses;
    private final Map<String, MajorData> allMajors;
    private final Map<String, MinorData> allMinors;
    private final StudentProfile profile;

    public ScheduleBuilder(Map<String, RawCourse> allCourses,
                           Map<String, MajorData> allMajors,
                           Map<String, MinorData> allMinors,
                           StudentProfile profile) {
        this.allCourses = allCourses;
        this.allMajors = allMajors;
        this.allMinors = allMinors;
        this.profile = profile;
    }

    public Schedule build() {
        List<String> warnings = new ArrayList<>();

        MajorData major = allMajors.get(profile.getMajorId());
        if (major == null) {
            throw new IllegalArgumentException("Unknown major: " + profile.getMajorId());
        }

        TfIdfEngine tfidf = new TfIdfEngine(allCourses.values());
        ChoiceGroupResolver resolver = new ChoiceGroupResolver(allCourses, profile, tfidf);

        // userExcluded: only what the student explicitly excluded — used for cascade blocking
        Set<String> userExcluded = new HashSet<>(profile.getExcludedCourseIds());

        // excluded: also drops graduate-level courses (number starts with 5+) from consideration
        Set<String> excluded = new HashSet<>(userExcluded);
        for (String id : allCourses.keySet()) {
            if (isGraduateCourse(id)) excluded.add(id);
        }

        // p0: major required courses (REQUIRED_ALL) + picks from CHOOSE_ONE / CHOOSE_N
        // usedInMajor prevents a course from satisfying two sections of the same program
        Set<String> p0 = new LinkedHashSet<>();
        Set<String> usedInMajor = new HashSet<>();
        for (RuleEntry rule : major.getRules()) {
            if (!rule.isActionable()) continue;
            if (rule.isRequiredAll()) {
                for (String id : rule.getOptions()) {
                    if (!excluded.contains(id)) {
                        p0.add(id);
                        usedInMajor.add(id);
                    }
                }
            } else {
                // CHOOSE_ONE or CHOOSE_N: exclude courses already claimed by an earlier rule in this major
                Set<String> programExcluded = new HashSet<>(excluded);
                programExcluded.addAll(usedInMajor);
                Set<String> picks = resolver.resolve(rule, programExcluded);
                p0.addAll(picks);
                usedInMajor.addAll(picks);
            }
        }

        // p1: minor required courses (REQUIRED_ALL from minors) + picks from CHOOSE rules
        // Each minor tracks its own usedInMinor set — cross-program sharing is allowed
        Set<String> p1 = new LinkedHashSet<>();
        for (String minorId : profile.getMinorIds()) {
            MinorData minor = allMinors.get(minorId);
            if (minor == null) {
                warnings.add("Minor '" + minorId + "' not found in minors.json — skipping.");
                continue;
            }
            Set<String> usedInMinor = new HashSet<>();
            double minorCuUsed = 0.0;
            for (RuleEntry rule : minor.getRules()) {
                if (!rule.isActionable()) continue;
                if (rule.isRequiredAll()) {
                    for (String id : rule.getOptions()) {
                        if (excluded.contains(id)) continue;
                        if (!p0.contains(id)) {
                            double cu = allCourses.containsKey(id) ? allCourses.get(id).getCreditUnits() : 1.0;
                            if (minorCuUsed + cu > 6.0) continue;
                            p1.add(id);
                            minorCuUsed += cu;
                        }
                        usedInMinor.add(id);
                    }
                } else {
                    Set<String> programExcluded = new HashSet<>(excluded);
                    programExcluded.addAll(usedInMinor);
                    Set<String> picks = resolver.resolve(rule, programExcluded);
                    for (String id : picks) {
                        if (!p0.contains(id)) {
                            double cu = allCourses.containsKey(id) ? allCourses.get(id).getCreditUnits() : 1.0;
                            if (minorCuUsed + cu > 6.0) continue;
                            p1.add(id);
                            minorCuUsed += cu;
                        }
                        usedInMinor.add(id);
                    }
                }
            }
        }

        // p2: explicitly desired electives not already covered
        Set<String> p2 = new LinkedHashSet<>();
        for (String id : profile.getDesiredElectiveIds()) {
            if (!p0.contains(id) && !p1.contains(id) && !excluded.contains(id)) {
                p2.add(id);
            }
        }

        Set<String> mustSchedule = new LinkedHashSet<>();
        mustSchedule.addAll(p0);
        mustSchedule.addAll(p1);
        mustSchedule.addAll(p2);

        Set<String> graphIds = new LinkedHashSet<>(mustSchedule);
        collectPrereqs(mustSchedule, excluded, graphIds, warnings);

        // Build graph first so cascade blocking can traverse dependents
        CourseGraph graph = new CourseGraph();
        for (String id : graphIds) {
            RawCourse raw = allCourses.get(id);
            if (raw != null) graph.addCourse(raw);
        }
        graph.mergeEquivalence();

        // Cascade-block only on user-explicit exclusions (not auto-filtered grad courses)
        Set<String> cascadeBlocked = cascadeBlock(userExcluded, graph, warnings);
        for (String blocked : cascadeBlocked) {
            graphIds.remove(blocked);
        }

        // Compute cuSoFar after blocking so interest-matched top-up is accurate
        double cuSoFar = 0.0;
        for (String id : graphIds) {
            RawCourse r = allCourses.get(id);
            if (r != null) cuSoFar += r.getCreditUnits();
        }

        double targetCUs = profile.getTargetCUs();
        double remaining = targetCUs - cuSoFar;
        if (remaining > 0) {
            int topN = (int) Math.ceil(remaining);
            addInterestMatched(topN, graphIds, excluded, tfidf, warnings);
            // Add any newly interest-matched courses into the graph
            for (String id : graphIds) {
                if (graph.getCourse(id) == null) {
                    RawCourse raw = allCourses.get(id);
                    if (raw != null) graph.addCourse(raw);
                }
            }
            graph.mergeEquivalence();
        }

        Set<String> noSeason = new HashSet<>();
        for (String id : graphIds) {
            Course c = graph.getCourse(id);
            if (c != null && !c.isOfferedFall() && !c.isOfferedSpring()) {
                noSeason.add(id);
                warnings.add("Course " + id + " has no offering season listed — treated as available every semester.");
            }
        }
        for (String id : graphIds) {
            Course c = graph.getCourse(id);
            if (c != null && c.isNotOfferedEveryYear()) {
                warnings.add("Course " + id + " is not offered every year — scheduling may not reflect actual availability.");
            }
        }

        graph.computeLevels();

        List<String> topoOrder;
        try {
            topoOrder = TopologicalSorter.sort(graph);
        } catch (IllegalStateException e) {
            topoOrder = new ArrayList<>(graphIds);
        }

        // Courses with "senior" in name or description are restricted to 4th year (semester index >= 6)
        Set<String> seniorCourses = new HashSet<>();
        for (String id : graphIds) {
            RawCourse raw = allCourses.get(id);
            if (raw == null) continue;
            String combined = (raw.getName() + " " + raw.getDescription()).toLowerCase();
            if (combined.contains("senior")) seniorCourses.add(id);
        }

        Set<String> priority = new LinkedHashSet<>();
        priority.addAll(p0);
        priority.addAll(p1);
        priority.addAll(p2);

        return greedyFill(topoOrder, graph, priority, noSeason, seniorCourses, warnings);
    }

    private void collectPrereqs(Set<String> seeds, Set<String> excluded, Set<String> accumulator, List<String> warnings) {
        Deque<String> queue = new ArrayDeque<>(seeds);
        while (!queue.isEmpty()) {
            String id = queue.poll();
            RawCourse raw = allCourses.get(id);
            if (raw == null) continue;
            for (String prereq : raw.getPrerequisites()) {
                if (!excluded.contains(prereq) && !accumulator.contains(prereq)) {
                    accumulator.add(prereq);
                    queue.add(prereq);
                }
            }
        }
    }

    private void addInterestMatched(int topN, Set<String> graphIds, Set<String> excluded,
                                    TfIdfEngine tfidf, List<String> warnings) {
        List<ScoredCandidate> candidates = new ArrayList<>();
        for (Map.Entry<String, RawCourse> entry : allCourses.entrySet()) {
            String id = entry.getKey();
            RawCourse raw = entry.getValue();
            if (graphIds.contains(id) || excluded.contains(id)) continue;
            if (!allPrereqsReachable(raw, graphIds)) continue;
            double score = tfidf.score(raw, profile.getInterests());
            candidates.add(new ScoredCandidate(id, score));
        }
        candidates.sort(Comparator.comparingDouble(ScoredCandidate::score).reversed());
        for (int i = 0; i < Math.min(topN, candidates.size()); i++) {
            graphIds.add(candidates.get(i).id());
        }
    }

    private boolean allPrereqsReachable(RawCourse raw, Set<String> graphIds) {
        for (String prereqId : raw.getPrerequisites()) {
            if (graphIds.contains(prereqId)) continue;
            boolean satisfiedByEquiv = false;
            RawCourse prereqRaw = allCourses.get(prereqId);
            if (prereqRaw != null) {
                Set<String> equivs = new HashSet<>(prereqRaw.getMutuallyExclusive());
                equivs.addAll(prereqRaw.getAlsoOfferedAs());
                for (String eq : equivs) {
                    if (graphIds.contains(eq)) { satisfiedByEquiv = true; break; }
                }
            }
            if (!satisfiedByEquiv) return false;
        }
        return true;
    }

    private Set<String> cascadeBlock(Set<String> excluded, CourseGraph graph, List<String> warnings) {
        Set<String> blocked = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>(excluded);
        while (!queue.isEmpty()) {
            String id = queue.poll();
            for (String dep : graph.getDependents(id)) {
                if (!blocked.contains(dep) && !excluded.contains(dep)) {
                    blocked.add(dep);
                    warnings.add("Course " + dep + " blocked because its prerequisite " + id + " is excluded.");
                    queue.add(dep);
                }
            }
        }
        return blocked;
    }

    private Schedule greedyFill(List<String> topoOrder, CourseGraph graph,
                                Set<String> priority, Set<String> noSeason,
                                Set<String> seniorCourses, List<String> warnings) {
        Schedule schedule = new Schedule();
        for (String w : warnings) schedule.addWarning(w);

        int semCount = Math.max(profile.getTotalSemesters(), 1);
        boolean isFall = profile.isStartInFall();
        int year = profile.getStartYear();
        for (int i = 0; i < semCount; i++) {
            schedule.addSemester(new Semester(i, isFall ? "Fall" : "Spring", year));
            if (isFall) year++;
            isFall = !isFall;
        }

        Map<String, Integer> scheduledIn = new LinkedHashMap<>();
        Set<String> scheduled = new LinkedHashSet<>();

        boolean progress = true;
        while (progress) {
            progress = false;
            for (String id : topoOrder) {
                if (scheduled.contains(id)) continue;
                Course course = graph.getCourse(id);
                if (course == null) continue;

                if (graph.equivalentAlreadyScheduled(id, scheduledIn)) {
                    scheduled.add(id);
                    continue;
                }

                boolean prereqsMet = true;
                for (String prereq : graph.getPrereqs(id)) {
                    if (!graph.isPrereqSatisfied(prereq, scheduledIn)) {
                        prereqsMet = false;
                        break;
                    }
                }
                if (!prereqsMet) continue;

                // Senior courses cannot be placed before semester index 6 (4th year)
                int earliestSem = Math.max(graph.getLevel(id), seniorCourses.contains(id) ? 6 : 0);

                // First pass: respect CU limit
                for (int s = earliestSem; s < schedule.getSemesters().size(); s++) {
                    Semester sem = schedule.getSemesters().get(s);
                    if (!courseAvailable(course, sem.getSeason(), noSeason)) continue;
                    if (sem.getTotalCUs() + course.getCreditUnits() > profile.getMaxCUsPerSemester()) continue;
                    if (prereqScheduledSameOrLater(course, graph, scheduledIn, s)) continue;

                    sem.addCourse(course);
                    scheduledIn.put(id, s);
                    scheduled.add(id);
                    progress = true;
                    break;
                }

                // Second pass: relax CU limit to avoid extending beyond target semesters
                if (!scheduled.contains(id)) {
                    for (int s = earliestSem; s < schedule.getSemesters().size(); s++) {
                        Semester sem = schedule.getSemesters().get(s);
                        if (!courseAvailable(course, sem.getSeason(), noSeason)) continue;
                        if (prereqScheduledSameOrLater(course, graph, scheduledIn, s)) continue;

                        sem.addCourse(course);
                        scheduledIn.put(id, s);
                        scheduled.add(id);
                        progress = true;
                        break;
                    }
                }

                // No extension — hard cap at totalSemesters
            }
        }

        return schedule;
    }

    private boolean courseAvailable(Course course, String season, Set<String> noSeason) {
        if (noSeason.contains(course.getId())) return true;
        return (season.equals("Fall") && course.isOfferedFall())
            || (season.equals("Spring") && course.isOfferedSpring());
    }

    private boolean prereqScheduledSameOrLater(Course course, CourseGraph graph,
                                               Map<String, Integer> scheduledIn, int targetSem) {
        for (String prereq : graph.getPrereqs(course.getId())) {
            Integer prereqSem = scheduledIn.get(prereq);
            if (prereqSem == null) {
                boolean foundEquiv = false;
                for (String eq : graph.getEquivalents(prereq)) {
                    Integer eqSem = scheduledIn.get(eq);
                    if (eqSem != null && eqSem < targetSem) { foundEquiv = true; break; }
                }
                if (!foundEquiv) return true;
            } else if (prereqSem >= targetSem) {
                return true;
            }
        }
        return false;
    }

    /** Returns true if the course number starts with 5 or higher (graduate level). */
    private static boolean isGraduateCourse(String courseId) {
        int space = courseId.lastIndexOf(' ');
        return space >= 0 && space + 1 < courseId.length() && courseId.charAt(space + 1) >= '5';
    }

    private record ScoredCandidate(String id, double score) {}
}
