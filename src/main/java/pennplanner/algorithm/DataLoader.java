package pennplanner.algorithm;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import pennplanner.model.MajorData;
import pennplanner.model.RawCourse;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
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
            Type listType = new TypeToken<List<MajorData>>() {}.getType();
            List<MajorData> list = GSON.fromJson(r, listType);
            Map<String, MajorData> map = new LinkedHashMap<>();
            for (MajorData m : list) {
                map.put(m.getId(), m);
            }
            return map;
        }
    }
}
