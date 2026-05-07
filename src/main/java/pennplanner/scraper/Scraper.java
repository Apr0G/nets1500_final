package pennplanner.scraper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.*;

public class Scraper {

    public static void run() {
        System.out.println("=== Scraper starting ===");
        System.out.println("Working directory: " + new File("").getAbsolutePath());

        List<String> subjectUrls = CourseScraper.scrapeAllSubjectUrls();
        System.out.println("Found " + subjectUrls.size() + " subject pages.");

        List<RawCourse> allCourses = new ArrayList<>();
        for (String url : subjectUrls) {
            System.out.println("Scraping courses: " + url);
            List<RawCourse> departmentCourses = CourseScraper.scrapeDepartment(url);
            allCourses.addAll(departmentCourses);
            System.out.println("  Added " + departmentCourses.size() + " courses.");
        }
        System.out.println("Total courses scraped: " + allCourses.size());

        expandAllPrerequisites(allCourses);

        MajorScraper.setCourseCatalog(allCourses);

        saveToJSON(allCourses, "courses.json");

        List<MajorRequirement> allMajorRequirements = scrapeEngineeringMajorRequirements();
        saveToJSON(allMajorRequirements, "majors.json");

        System.out.println("=== Scraper done ===");
    }

    private static List<MajorRequirement> scrapeEngineeringMajorRequirements() {
        List<MajorScraper.MajorInfo> engineeringMajors = MajorScraper.scrapeEngineeringMajorLinks();
        List<MajorRequirement> allMajorRequirements = new ArrayList<>();

        System.out.println("Found " + engineeringMajors.size() + " Engineering majors.");
        for (MajorScraper.MajorInfo major : engineeringMajors) {
            System.out.println("Scraping Engineering major: " + major.name);
            List<MajorRequirement> reqs = MajorScraper.scrapeMajorRequirements(major);
            allMajorRequirements.addAll(reqs);
            System.out.println("  Found " + reqs.size() + " requirement rules.");
        }

        return allMajorRequirements;
    }

    private static void expandAllPrerequisites(List<RawCourse> courses) {
        Map<String, RawCourse> courseMap = new HashMap<>();
        for (RawCourse course : courses) {
            courseMap.put(course.getCourseId(), course);
        }
        for (RawCourse course : courses) {
            Set<String> allPrereqs = new LinkedHashSet<>();
            collectPrerequisites(course, courseMap, allPrereqs, new HashSet<>());
            course.setPrerequisites(new ArrayList<>(allPrereqs));
        }
    }

    private static void collectPrerequisites(RawCourse course,
                                             Map<String, RawCourse> courseMap,
                                             Set<String> allPrereqs,
                                             Set<String> visited) {
        for (String prereqId : course.getPrerequisites()) {
            if (visited.contains(prereqId)) continue;
            visited.add(prereqId);
            allPrereqs.add(prereqId);
            RawCourse prereqCourse = courseMap.get(prereqId);
            if (prereqCourse != null) {
                collectPrerequisites(prereqCourse, courseMap, allPrereqs, visited);
            }
        }
    }

    private static void saveToJSON(Object object, String filename) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter writer = new FileWriter(filename);
            gson.toJson(object, writer);
            writer.close();
            System.out.println("Saved: " + filename);
        } catch (IOException e) {
            System.out.println("Could not save " + filename + ": " + e.getMessage());
        }
    }
}
