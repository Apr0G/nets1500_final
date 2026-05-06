package pennplanner.algorithm;

import pennplanner.model.Course;
import pennplanner.model.RawCourse;

import java.util.*;

public class CourseGraph {

    private final Map<String, Course> nodes = new LinkedHashMap<>();
    private final Map<String, List<String>> prereqList = new HashMap<>();
    private final Map<String, List<String>> dependentList = new HashMap<>();
    private final Map<String, Set<String>> equivalenceMap = new HashMap<>();
    private final Map<String, Integer> levels = new HashMap<>();

    public void addCourse(RawCourse raw) {
        Course course = new Course(raw);
        String id = course.getId();
        nodes.put(id, course);
        prereqList.putIfAbsent(id, new ArrayList<>(raw.getPrerequisites()));
        dependentList.putIfAbsent(id, new ArrayList<>());

        Set<String> equivSet = new HashSet<>(course.getEquivalentCourses());
        equivSet.add(id);
        equivalenceMap.put(id, equivSet);

        for (String prereq : raw.getPrerequisites()) {
            dependentList.computeIfAbsent(prereq, k -> new ArrayList<>()).add(id);
        }
    }

    public void mergeEquivalence() {
        for (Map.Entry<String, Course> entry : nodes.entrySet()) {
            String id = entry.getKey();
            for (String equiv : entry.getValue().getEquivalentCourses()) {
                if (nodes.containsKey(equiv)) {
                    equivalenceMap.get(id).add(equiv);
                    equivalenceMap.computeIfAbsent(equiv, k -> new HashSet<>()).add(id);
                    equivalenceMap.get(equiv).add(id);
                }
            }
        }
        for (Set<String> group : equivalenceMap.values()) {
            group.retainAll(nodes.keySet());
        }
    }

    public boolean isPrereqSatisfied(String prereqId, Map<String, Integer> scheduledIn) {
        if (scheduledIn.containsKey(prereqId)) return true;
        Set<String> equivs = equivalenceMap.getOrDefault(prereqId, Set.of());
        for (String eq : equivs) {
            if (scheduledIn.containsKey(eq)) return true;
        }
        return false;
    }

    public boolean equivalentAlreadyScheduled(String courseId, Map<String, Integer> scheduledIn) {
        Set<String> equivs = equivalenceMap.getOrDefault(courseId, Set.of());
        for (String eq : equivs) {
            if (!eq.equals(courseId) && scheduledIn.containsKey(eq)) return true;
        }
        return false;
    }

    public void computeLevels() {
        Map<String, Integer> memo = new HashMap<>();
        for (String id : nodes.keySet()) {
            dfsLevel(id, memo, new HashSet<>());
        }
        levels.putAll(memo);
    }

    private int dfsLevel(String id, Map<String, Integer> memo, Set<String> visiting) {
        if (memo.containsKey(id)) return memo.get(id);
        if (visiting.contains(id)) return 0;
        visiting.add(id);
        int max = 0;
        for (String prereq : prereqList.getOrDefault(id, List.of())) {
            if (nodes.containsKey(prereq)) {
                max = Math.max(max, dfsLevel(prereq, memo, visiting) + 1);
            }
        }
        visiting.remove(id);
        memo.put(id, max);
        return max;
    }

    public Map<String, Integer> computeInDegrees() {
        Map<String, Integer> inDeg = new LinkedHashMap<>();
        for (String id : nodes.keySet()) inDeg.put(id, 0);
        for (Map.Entry<String, List<String>> e : prereqList.entrySet()) {
            for (String prereq : e.getValue()) {
                if (inDeg.containsKey(prereq)) {
                    inDeg.merge(e.getKey(), 1, Integer::sum);
                }
            }
        }
        return inDeg;
    }

    public int getLevel(String id) { return levels.getOrDefault(id, 0); }
    public Course getCourse(String id) { return nodes.get(id); }
    public Map<String, Course> getNodes() { return nodes; }
    public List<String> getPrereqs(String id) { return prereqList.getOrDefault(id, List.of()); }
    public List<String> getDependents(String id) { return dependentList.getOrDefault(id, List.of()); }
    public Set<String> getEquivalents(String id) { return equivalenceMap.getOrDefault(id, Set.of()); }
    public int size() { return nodes.size(); }
}
