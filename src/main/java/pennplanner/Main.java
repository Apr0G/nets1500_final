package pennplanner;

import pennplanner.algorithm.DataLoader;
import pennplanner.algorithm.ScheduleBuilder;
import pennplanner.model.MajorData;
import pennplanner.model.RawCourse;
import pennplanner.model.Schedule;
import pennplanner.model.StudentProfile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {

    public static void main(String[] args) throws IOException {
        Path coursesPath = Path.of("courses.json");
        Path majorsPath  = Path.of("majors.json");

        Map<String, RawCourse> courses = DataLoader.loadCourses(coursesPath);
        Map<String, MajorData> majors  = DataLoader.loadMajors(majorsPath);

        System.out.println("Loaded " + courses.size() + " courses and " + majors.size() + " majors.");

        StudentProfile profile = new StudentProfile(
            "NETS",
            List.of("NETS Minor", "CIS Minor"),
            List.of("CIS 5450", "CIS 5500", "CIS 4600"),
            Set.of(),
            List.of("machine learning", "distributed systems", "data science"),
            2.0,
            8,
            true,
            2024,
            "SEAS"
        );

        ScheduleBuilder builder = new ScheduleBuilder(courses, majors, profile);
        Schedule schedule = builder.build();

        System.out.println(schedule);
    }
}
