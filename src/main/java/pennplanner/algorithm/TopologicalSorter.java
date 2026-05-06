package pennplanner.algorithm;

import java.util.*;

public class TopologicalSorter {

    public static List<String> sort(CourseGraph graph) {
        Map<String, Integer> inDeg = graph.computeInDegrees();
        Queue<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> e : inDeg.entrySet()) {
            if (e.getValue() == 0) queue.add(e.getKey());
        }

        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            result.add(cur);
            for (String dep : graph.getDependents(cur)) {
                if (!inDeg.containsKey(dep)) continue;
                int updated = inDeg.merge(dep, -1, Integer::sum);
                if (updated == 0) queue.add(dep);
            }
        }

        if (result.size() != graph.size()) {
            throw new IllegalStateException(
                "Cycle detected in prerequisite graph. Courses involved: " +
                findCycleCourses(graph, result)
            );
        }
        return result;
    }

    private static List<String> findCycleCourses(CourseGraph graph, List<String> sorted) {
        Set<String> visited = new HashSet<>(sorted);
        List<String> cyclic = new ArrayList<>();
        for (String id : graph.getNodes().keySet()) {
            if (!visited.contains(id)) cyclic.add(id);
        }
        return cyclic;
    }
}
