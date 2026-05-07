import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.*;

public class ProgramScraper {

    public static class ProgramInfo {
        String name;
        String url;

        public ProgramInfo(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    public static List<ProgramInfo> scrapeUndergraduateMinorsAndCertificates() {
        List<ProgramInfo> programs = new ArrayList<>();

        try {
            Document doc = Jsoup.connect("https://catalog.upenn.edu/undergraduate/programs/").get();

            Elements links = doc.select("a[href]");

            for (Element link : links) {
                String text = link.text().trim();
                String absUrl = link.absUrl("href");

                if (!absUrl.contains("/undergraduate/programs/")) {
                    continue;
                }

                if (!isWantedProgram(text)) {
                    continue;
                }

                String name = cleanProgramName(text);

                boolean alreadyAdded = false;
                for (ProgramInfo program : programs) {
                    if (program.url.equals(absUrl)) {
                        alreadyAdded = true;
                        break;
                    }
                }

                if (!alreadyAdded) {
                    programs.add(new ProgramInfo(name, absUrl));
                }
            }

        } catch (Exception e) {
            System.out.println("Could not scrape undergraduate programs page.");
            System.out.println(e.getMessage());
        }

        return programs;
    }

    private static boolean isWantedProgram(String text) {
        String lower = text.toLowerCase();

        boolean isMinor = lower.contains("minor");
        boolean isCertificate = lower.contains("certificate");

        boolean isMajorDegree =
                lower.contains(" ba")
                        || lower.contains(" bs")
                        || lower.contains(" bse")
                        || lower.contains(" bas")
                        || lower.contains(" bachelor");

        return (isMinor || isCertificate) && !isMajorDegree;
    }

    private static String cleanProgramName(String text) {
        return text
                .replace("Undergraduate", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static Set<String> scrapeRequiredCourseIds(String programUrl) {
        Set<String> courseIds = new LinkedHashSet<>();

        try {
            Document doc = Jsoup.connect(programUrl).get();

            Elements requirementTables = doc.select("table.sc_courselist");

            for (Element table : requirementTables) {
                String tableText = table.text();

                Pattern pattern = Pattern.compile("([A-Z]{2,5})\\s*(\\d{4})");
                Matcher matcher = pattern.matcher(tableText);

                while (matcher.find()) {
                    String courseId = matcher.group(1) + " " + matcher.group(2);
                    courseIds.add(courseId);
                }
            }

        } catch (Exception e) {
            System.out.println("Could not scrape program page: " + programUrl);
            System.out.println(e.getMessage());
        }

        return courseIds;
    }
}