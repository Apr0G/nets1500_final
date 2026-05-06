package pennplanner.algorithm;

import pennplanner.model.RawCourse;

import java.util.*;

public class TfIdfEngine {

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "and", "or", "of", "in", "to", "for", "with",
            "on", "at", "by", "from", "as", "is", "are", "was", "be", "been",
            "this", "that", "it", "its", "we", "i", "you", "he", "she", "they",
            "not", "no", "but", "if", "so", "up", "do", "into", "how", "what",
            "which", "will", "can", "has", "have", "had", "more", "their", "than"
    );

    private final Map<String, Double> idf = new HashMap<>();
    private final int corpusSize;

    public TfIdfEngine(Collection<RawCourse> corpus) {
        this.corpusSize = corpus.size();
        Map<String, Integer> docFreq = new HashMap<>();
        for (RawCourse course : corpus) {
            Set<String> tokens = new HashSet<>(tokenize(course.getDescription()));
            tokens.addAll(tokenize(course.getName()));
            for (String token : tokens) {
                docFreq.merge(token, 1, Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> e : docFreq.entrySet()) {
            idf.put(e.getKey(), Math.log((1.0 + corpusSize) / (1.0 + e.getValue())) + 1.0);
        }
    }

    public double score(RawCourse course, List<String> interests) {
        if (interests == null || interests.isEmpty()) return 0.0;

        List<String> docTokens = new ArrayList<>(tokenize(course.getDescription()));
        docTokens.addAll(tokenize(course.getName()));
        Map<String, Long> tf = new HashMap<>();
        for (String t : docTokens) {
            tf.merge(t, 1L, Long::sum);
        }
        if (docTokens.isEmpty()) return 0.0;

        double score = 0.0;
        for (String interest : interests) {
            for (String token : tokenize(interest)) {
                long freq = tf.getOrDefault(token, 0L);
                double termTf = (double) freq / docTokens.size();
                double termIdf = idf.getOrDefault(token, Math.log(1.0 + corpusSize) + 1.0);
                score += termTf * termIdf;
            }
        }
        return score;
    }

    public static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> tokens = new ArrayList<>();
        for (String word : text.toLowerCase().split("[^a-z0-9]+")) {
            if (word.length() > 2 && !STOPWORDS.contains(word)) {
                tokens.add(word);
            }
        }
        return tokens;
    }
}
