package pennplanner.algorithm;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import pennplanner.model.MajorData;
import pennplanner.model.MinorData;
import pennplanner.model.RawCourse;
import pennplanner.model.RuleEntry;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataLoader {

    private static final Gson GSON = new Gson();

    public static Map<String, RawCourse> loadCourses(Path path) throws IOException {
        try (Reader r = Files.newBufferedReader(path)) {
            Type listType = new TypeToken<List<RawCourse>>() {}.getType();
            List<RawCourse> list = GSON.fromJson(r, listType);
            Map<String, RawCourse> map = new LinkedHashMap<>();
            for (RawCourse c : list) {
                map.put(c.getId(), c);
            }
            return map;
        }
    }

    public static Map<String, MajorData> loadMajors(Path path) throws IOException {
        try (Reader r = Files.newBufferedReader(path)) {
            Type listType = new TypeToken<List<RuleEntry>>() {}.getType();
            List<RuleEntry> entries = GSON.fromJson(r, listType);
            Map<String, List<RuleEntry>> grouped = new LinkedHashMap<>();
            for (RuleEntry e : entries) {
                String name = e.getName();
                if (name == null) continue;
                grouped.computeIfAbsent(name, k -> new ArrayList<>()).add(e);
            }
            Map<String, MajorData> result = new LinkedHashMap<>();
            for (Map.Entry<String, List<RuleEntry>> entry : grouped.entrySet()) {
                MajorData md = buildMajorData(entry.getKey(), entry.getValue());
                result.put(md.getId(), md);
            }
            return result;
        }
    }

    public static Map<String, MinorData> loadMinors(Path path) throws IOException {
        try (Reader r = Files.newBufferedReader(path)) {
            Type listType = new TypeToken<List<RuleEntry>>() {}.getType();
            List<RuleEntry> entries = GSON.fromJson(r, listType);
            Map<String, List<RuleEntry>> grouped = new LinkedHashMap<>();
            for (RuleEntry e : entries) {
                String name = e.getName();
                if (name == null) continue;
                grouped.computeIfAbsent(name, k -> new ArrayList<>()).add(e);
            }
            Map<String, MinorData> result = new LinkedHashMap<>();
            for (Map.Entry<String, List<RuleEntry>> entry : grouped.entrySet()) {
                MinorData md = buildMinorData(entry.getKey(), entry.getValue());
                result.put(md.getId(), md);
            }
            return result;
        }
    }

    private static MajorData buildMajorData(String name, List<RuleEntry> rules) {
        // Use reflection-free approach: set fields via a subclass trick isn't needed —
        // MajorData exposes setters via constructor-like factory here instead.
        // We use a simple anonymous approach via Gson to populate the object.
        String json = GSON.toJson(Map.of("id", name, "rules", rules));
        return GSON.fromJson(json, MajorData.class);
    }

    private static MinorData buildMinorData(String name, List<RuleEntry> rules) {
        String json = GSON.toJson(Map.of("id", name, "rules", rules));
        return GSON.fromJson(json, MinorData.class);
    }
}
