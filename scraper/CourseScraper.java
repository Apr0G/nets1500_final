import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.*;

public class CourseScraper {

    public static List<String> scrapeAllSubjectUrls() {
        List<String> urls = new ArrayList<>();

        try {
            Document doc = Jsoup.connect("https://catalog.upenn.edu/courses/").get();
            Elements links = doc.select("a[href^=/courses/]");

            for (Element link : links) {
                String absUrl = link.absUrl("href");

                if (!absUrl.equals("https://catalog.upenn.edu/courses/")
                        && !urls.contains(absUrl)) {
                    urls.add(absUrl);
                }
            }

        } catch (Exception e) {
            System.out.println("Could not scrape course index.");
            System.out.println(e.getMessage());
        }

        return urls;
    }

    public static List<RawCourse> scrapeDepartment(String url) {
        List<RawCourse> courses = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(url).get();
            Elements courseBlocks = doc.select(".courseblock");

            for (Element block : courseBlocks) {
                String titleText = block.select(".courseblocktitle").text();
                String fullText = block.text();

                if (titleText.isEmpty()) {
                    continue;
                }

                String courseId = extractCourseId(titleText);

                if (courseId.equals("UNKNOWN")) {
                    continue;
                }

                String courseName = extractCourseName(titleText, courseId);
                double creditUnits = extractCreditUnits(fullText);

                List<String> prerequisites = extractLineCourses(fullText, "Prerequisite");
                List<String> corequisites = extractLineCourses(fullText, "Corequisite");
                List<String> mutuallyExclusive = extractLineCourses(fullText, "Mutually Exclusive");
                List<String> alsoOfferedAs = extractLineCourses(fullText, "Also Offered As");

                boolean offeredInFall = detectOfferedInFall(block);
                boolean offeredInSpring = detectOfferedInSpring(block);
                boolean notOfferedEveryYear = detectNotOfferedEveryYear(fullText);

                String description = extractDescription(block, courseId, courseName);

                RawCourse course = new RawCourse(
                        courseId,
                        courseName,
                        creditUnits,
                        prerequisites,
                        corequisites,
                        mutuallyExclusive,
                        alsoOfferedAs,
                        offeredInFall,
                        offeredInSpring,
                        notOfferedEveryYear,
                        description,
                        new ArrayList<>()
                );

                courses.add(course);
            }

        } catch (Exception e) {
            System.out.println("Error scraping: " + url);
            System.out.println(e.getMessage());
        }

        return courses;
    }

    private static String extractCourseId(String titleText) {
        Pattern pattern = Pattern.compile("([A-Z]{2,5})\\s*(\\d{4})");
        Matcher matcher = pattern.matcher(titleText);

        if (matcher.find()) {
            return matcher.group(1) + " " + matcher.group(2);
        }

        return "UNKNOWN";
    }

    private static String extractCourseName(String titleText, String courseId) {
        String name = titleText.replaceFirst(Pattern.quote(courseId), "");

        name = name.replaceAll("^[-.:\\s]+", "");
        name = name.replaceAll("\\d+(\\.\\d+)?\\s*Course Unit(s)?", "");
        name = name.replaceAll("Course Unit(s)?", "");
        name = name.trim();

        return name;
    }

    private static double extractCreditUnits(String fullText) {
        Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*Course Unit(s)?");
        Matcher matcher = pattern.matcher(fullText);

        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }

        return 1.0;
    }

    private static List<String> extractLineCourses(String fullText, String label) {
        List<String> courses = new ArrayList<>();

        String[] labels = {
                "Also Offered As",
                "Mutually Exclusive",
                "Prerequisite",
                "Corequisite",
                "Equivalent Course",
                "Activity",
                "Course Unit",
                "Fall or Spring",
                "Fall",
                "Spring",
                "Summer",
                "Not Offered Every Year"
        };

        int start = fullText.indexOf(label + ":");

        if (start == -1) {
            return courses;
        }

        start = start + label.length() + 1;
        int end = fullText.length();

        for (String otherLabel : labels) {
            if (otherLabel.equals(label)) {
                continue;
            }

            int otherIndex = fullText.indexOf(otherLabel + ":", start);

            if (otherIndex != -1 && otherIndex < end) {
                end = otherIndex;
            }
        }

        String line = fullText.substring(start, end);

        Pattern pattern = Pattern.compile("([A-Z]{2,5})\\s*(\\d{4})");
        Matcher matcher = pattern.matcher(line);

        while (matcher.find()) {
            String course = matcher.group(1) + " " + matcher.group(2);

            if (!courses.contains(course)) {
                courses.add(course);
            }
        }

        return courses;
    }

    private static boolean detectOfferedInFall(Element block) {
        String offering = extractOfferingLine(block);

        return offering.equals("Fall") || offering.equals("Fall or Spring");
    }

    private static boolean detectOfferedInSpring(Element block) {
        String offering = extractOfferingLine(block);

        return offering.equals("Spring") || offering.equals("Fall or Spring");
    }

    private static String extractOfferingLine(Element block) {
        Elements elements = block.select("p, div, span");

        for (Element element : elements) {
            String text = element.text().trim();

            if (text.equals("Fall or Spring")) {
                return "Fall or Spring";
            }

            if (text.equals("Fall")) {
                return "Fall";
            }

            if (text.equals("Spring")) {
                return "Spring";
            }
        }

        String html = block.html();

        if (html.contains(">Fall or Spring<")) {
            return "Fall or Spring";
        }

        if (html.contains(">Fall<")) {
            return "Fall";
        }

        if (html.contains(">Spring<")) {
            return "Spring";
        }

        return "";
    }

    private static boolean detectNotOfferedEveryYear(String fullText) {
        return fullText.contains("Not Offered Every Year");
    }

    private static String extractDescription(Element block, String courseId, String courseName) {
        String fullText = block.text();
        String description = fullText;

        if (description.startsWith(courseId)) {
            description = description.substring(courseId.length()).trim();
        }

        if (description.startsWith(courseName)) {
            description = description.substring(courseName.length()).trim();
        }

        String[] stopMarkers = {
                "Fall or Spring",
                "Not Offered Every Year",
                "Also Offered As:",
                "Mutually Exclusive:",
                "Prerequisite:",
                "Corequisite:",
                "Equivalent Course:",
                "Activity:",
                "Course Unit"
        };

        int stopIndex = description.length();

        for (String marker : stopMarkers) {
            int index = description.indexOf(marker);

            if (index != -1 && index < stopIndex) {
                stopIndex = index;
            }
        }

        return description.substring(0, stopIndex).trim();
    }
}