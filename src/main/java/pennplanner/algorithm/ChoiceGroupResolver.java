package pennplanner.algorithm;

import pennplanner.model.RawCourse;
import pennplanner.model.RuleEntry;
import pennplanner.model.StudentProfile;

import java.util.*;

public class ChoiceGroupResolver {

    private final Map<String, RawCourse> allCourses;
    private final StudentProfile profile;
    private final TfIdfEngine tfidf;

    public ChoiceGroupResolver(Map<String, RawCourse> allCourses, StudentProfile profile, TfIdfEngine tfidf) {
        this.allCourses = allCourses;
        this.profile = profile;
        this.tfidf = tfidf;
    }

    public Set<String> resolve(RuleEntry rule, Set<String> excluded) {
        int needed = rule.isChooseOne() ? 1 : rule.getCoursesNeeded();
        List<String> options = rule.getOptions();

        List<ScoredCourse> candidates = new ArrayList<>();
        Set<String> desiredSet = new HashSet<>(profile.getDesiredElectiveIds());

        double maxTfidf = 0.0;
        Map<String, Double> rawTfidf = new LinkedHashMap<>();
        for (String courseId : options) {
            if (excluded.contains(courseId)) continue;
            RawCourse raw = allCourses.get(courseId);
            if (raw == null) continue;
            double t = tfidf.score(raw, profile.getInterests());
            rawTfidf.put(courseId, t);
            if (t > maxTfidf) maxTfidf = t;
        }

        for (String courseId : options) {
            if (excluded.contains(courseId)) continue;
            RawCourse raw = allCourses.get(courseId);
            if (raw == null) continue;

            double score = 0.0;
            if (desiredSet.contains(courseId)) score += 2.0;
            double normalizedTfidf = maxTfidf > 0 ? rawTfidf.getOrDefault(courseId, 0.0) / maxTfidf : 0.0;
            score += normalizedTfidf;

            candidates.add(new ScoredCourse(courseId, score));
        }

        candidates.sort(Comparator.comparingDouble(ScoredCourse::score).reversed());

        Set<String> picks = new LinkedHashSet<>();
        for (int i = 0; i < Math.min(needed, candidates.size()); i++) {
            picks.add(candidates.get(i).id());
        }
        return picks;
    }

    private record ScoredCourse(String id, double score) {}
}
