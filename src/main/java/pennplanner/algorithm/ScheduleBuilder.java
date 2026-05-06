package pennplanner.algorithm;

import pennplanner.model.*;

import java.util.*;

public class ScheduleBuilder {

    private static final int MAX_SEMESTERS = 16;

    private final Map<String, RawCourse> allCourses;
    private final Map<String, MajorData> allMajors;
    private final StudentProfile profile;

    public ScheduleBuilder(Map<String, RawCourse> allCourses,
                           Map<String, MajorData> allMajors,
                           StudentProfile profile) {
        this.allCourses = allCourses;
        this.allMajors = allMajors;
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

        Set<String> excluded = new HashSet<>(profile.getExcludedCourseIds());

        Set<String> p0 = new LinkedHashSet<>();
        for (String id : major.getRequiredCourses()) {
            if (!excluded.contains(id)) p0.add(id);
        }

        Set<String> choiceGroupPicks = new LinkedHashSet<>();
        for (ChoiceGroup cg : major.getChoiceGroups()) {
            Set<String> picks = resolver.resolve(cg, excluded);
            choiceGroupPicks.addAll(picks);
        }
        p0.addAll(choiceGroupPicks);

        Set<String> p1 = new LinkedHashSet<>();
        for (String minorId : profile.getMinorIds()) {
            for (Map.Entry<String, RawCourse> entry : allCourses.entrySet()) {
                String courseId = entry.getKey();
                RawCourse raw = entry.getValue();
                if (p0.contains(courseId) || excluded.contains(courseId)) continue;
                if (raw.getRequiredFor().contains(minorId)) {
                    p1.add(courseId);
                }
            }
        }

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

        double cuSoFar = 0.0;
        for (String id : graphIds) {
            RawCourse r = allCourses.get(id);
            if (r != null) cuSoFar += r.getCreditUnits();
        }

        double targetCUs = profile.getTargetCUs();
        double remaining = targetCUs - cuSoFar;
        if (remaining > 0) {
            double avgCU = 1.0;
            int topN = (int) Math.ceil(remaining / avgCU);
            addInterestMatched(topN, graphIds, excluded, tfidf, warnings);
        }

        CourseGraph graph = new CourseGraph();
        for (String id : graphIds) {
            RawCourse raw = allCourses.get(id);
            if (raw != null) graph.addCourse(raw);
        }
        graph.mergeEquivalence();

        Set<String> cascadeBlocked = cascadeBlock(excluded, graph, warnings);
        for (String blocked : cascadeBlocked) {
            graphIds.remove(blocked);
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
            warnings.add("Cycle detected: " + e.getMessage());
            topoOrder = new ArrayList<>(graphIds);
        }

        Set<String> priority = new LinkedHashSet<>();
        priority.addAll(p0);
        priority.addAll(p1);
        priority.addAll(p2);

        return greedyFill(topoOrder, graph, priority, noSeason, warnings);
    }

    private void collectPrereqs(Set<String> seeds, Set<String> excluded, Set<String> accumulator, List<String> warnings) {
        Deque<String> queue = new ArrayDeque<>(seeds);
        while (!queue.isEmpty()) {
            String id = queue.poll();
            RawCourse raw = allCourses.get(id);
            if (raw == null) {
                warnings.add("Course " + id + " referenced but not found in courses.json — skipping.");
                continue;
            }
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
                                List<String> warnings) {
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

                int earliestSem = graph.getLevel(id);

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

                if (!scheduled.contains(id)) {
                    if (schedule.getSemesters().size() < MAX_SEMESTERS) {
                        Semester last = schedule.getSemesters().get(schedule.getSemesters().size() - 1);
                        String nextSeason = last.getSeason().equals("Fall") ? "Spring" : "Fall";
                        int nextYear = last.getSeason().equals("Fall") ? last.getYear() + 1 : last.getYear();
                        schedule.addSemester(new Semester(schedule.getSemesters().size(), nextSeason, nextYear));
                        progress = true;
                    }
                }
            }
        }

        Set<String> unplaced = new LinkedHashSet<>();
        for (String id : topoOrder) {
            if (!scheduled.contains(id)) unplaced.add(id);
        }
        for (String id : unplaced) {
            Course c = graph.getCourse(id);
            if (c != null && !graph.equivalentAlreadyScheduled(id, scheduledIn)) {
                if (priority.contains(id)) {
                    schedule.addWarning("Required/desired course " + id + " could not be scheduled (prereq cycle or season conflict).");
                }
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

    private record ScoredCandidate(String id, double score) {}
}
