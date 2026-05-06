package pennplanner.algorithm;

import pennplanner.model.ChoiceGroup;
import pennplanner.model.RawCourse;
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

    public Set<String> resolve(ChoiceGroup group, Set<String> excluded) {
        List<ScoredCourse> candidates = new ArrayList<>();
        Set<String> minorSet = new HashSet<>(profile.getMinorIds());
        Set<String> desiredSet = new HashSet<>(profile.getDesiredElectiveIds());

        double maxTfidf = 0.0;
        Map<String, Double> rawTfidf = new LinkedHashMap<>();
        for (String courseId : group.getCourses()) {
            if (excluded.contains(courseId)) continue;
            RawCourse raw = allCourses.get(courseId);
            if (raw == null) continue;
            double t = tfidf.score(raw, profile.getInterests());
            rawTfidf.put(courseId, t);
            if (t > maxTfidf) maxTfidf = t;
        }

        for (String courseId : group.getCourses()) {
            if (excluded.contains(courseId)) continue;
            RawCourse raw = allCourses.get(courseId);
            if (raw == null) continue;

            double score = 0.0;
            for (String req : raw.getRequiredFor()) {
                if (minorSet.contains(req)) { score += 3.0; break; }
            }
            if (desiredSet.contains(courseId)) score += 2.0;
            double normalizedTfidf = maxTfidf > 0 ? rawTfidf.get(courseId) / maxTfidf : 0.0;
            score += normalizedTfidf;

            candidates.add(new ScoredCourse(courseId, score));
        }

        candidates.sort(Comparator.comparingDouble(ScoredCourse::score).reversed());

        Set<String> picks = new LinkedHashSet<>();
        for (int i = 0; i < Math.min(group.getCount(), candidates.size()); i++) {
            picks.add(candidates.get(i).id());
        }
        return picks;
    }

    private record ScoredCourse(String id, double score) {}
}
