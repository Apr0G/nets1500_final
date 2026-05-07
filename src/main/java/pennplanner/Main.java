package pennplanner;

import pennplanner.algorithm.DataLoader;
import pennplanner.algorithm.ScheduleBuilder;
import pennplanner.model.MajorData;
import pennplanner.model.MinorData;
import pennplanner.model.RawCourse;
import pennplanner.model.Schedule;
import pennplanner.model.StudentProfile;
import pennplanner.scraper.Scraper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {

    public static void main(String[] args) throws IOException {
        Scraper.run();

        Path coursesPath = Path.of("courses.json");
        Path majorsPath  = Path.of("majors.json");
        Path minorsPath  = Path.of("minors.json");

        Map<String, RawCourse> courses = DataLoader.loadCourses(coursesPath);
        Map<String, MajorData> majors  = DataLoader.loadMajors(majorsPath);
        Map<String, MinorData> minors  = DataLoader.loadMinors(minorsPath);

        System.out.println("Loaded " + courses.size() + " courses, "
            + majors.size() + " majors, " + minors.size() + " minors.");

        StudentProfile profile = new StudentProfile(
            "Computer Science, BSE",
            List.of(),
            List.of(),
            Set.of(),
            List.of("machine learning", "distributed systems", "data science"),
            5.0,
            8,
            true,
            2024,
            "SEAS"
        );

        ScheduleBuilder builder = new ScheduleBuilder(courses, majors, minors, profile);
        Schedule schedule = builder.build();

        System.out.println(schedule);
    }
}
