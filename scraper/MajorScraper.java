import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.*;

public class MajorScraper {

    public static class MajorInfo {
        String name;
        String url;

        public MajorInfo(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    private static List<RawCourse> courseCatalog = new ArrayList<>();

    private static final Set<String> ENGINEERING_SUBJECTS = new HashSet<>(Arrays.asList(
            "BE",
            "CBE",
            "CIS",
            "DATA",
            "DATS",
            "ENGR",
            "ESE",
            "IPD",
            "MEAM",
            "MSE",
            "NANO",
            "NETS",
            "ROBO",
            "TCOM"
    ));

    private static final Set<String> NON_ENGINEERING_SUBJECTS = new HashSet<>(Arrays.asList(
            "EAS",
            "ENM",
            "CIT",
            "MCIT"
    ));

    private static final Set<String> ENGINEERING_EXCLUDED_COURSES = new HashSet<>(Arrays.asList(
            "BE 5130",

            "CIS 1050",
            "CIS 1060",
            "CIS 1070",
            "CIS 1250",
            "CIS 1600",
            "CIS 2610",
            "CIS 3333",
            "CIS 4230",
            "CIS 5230",
            "CIS 7980",

            "CSE 1050",
            "CSE 1060",
            "CSE 1070",
            "CSE 1250",
            "CSE 1600",
            "CSE 2610",
            "CSE 3333",
            "CSE 4230",
            "CSE 5230",
            "CSE 7980",

            "ENGR 5020",

            "ESE 2030",
            "ESE 3010",
            "ESE 4020",
            "ESE 5300",
            "ESE 5670",

            "IPD 5090",

            "MEAM 0110",
            "MEAM 0147",
            "MEAM 110",
            "MEAM 147",

            "MSE 2210"
    ));

    private static final Set<String> ENGINEERING_INCLUDED_EXCEPTIONS = new HashSet<>(Arrays.asList(
            "NSCI 3010"
    ));

    public static void setCourseCatalog(List<RawCourse> courses) {
        courseCatalog = courses;
    }

    public static List<MajorInfo> scrapeEngineeringMajorLinks() {
        List<MajorInfo> majors = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(
                    "https://catalog.upenn.edu/undergraduate/engineering-applied-science/majors/"
            ).get();

            Elements links = doc.select("a[href]");

            for (Element link : links) {
                String text = link.text().trim();
                String url = link.absUrl("href");

                if (!url.contains("/undergraduate/programs/")
                        && !url.contains("/undergraduate/engineering-applied-science/")) {
                    continue;
                }

                if (!(text.contains(", BSE") || text.contains(", BAS"))) {
                    continue;
                }

                boolean alreadyAdded = false;

                for (MajorInfo major : majors) {
                    if (major.url.equals(url)) {
                        alreadyAdded = true;
                        break;
                    }
                }

                if (!alreadyAdded) {
                    majors.add(new MajorInfo(text, url));
                }
            }

        } catch (Exception e) {
            System.out.println("Could not scrape Engineering majors page.");
            System.out.println(e.getMessage());
        }

        return majors;
    }

    public static List<MajorRequirement> scrapeEngineeringMajorRequirements() {
        List<MajorRequirement> allRequirements = new ArrayList<>();

        List<MajorInfo> majors = scrapeEngineeringMajorLinks();

        for (MajorInfo major : majors) {
            allRequirements.addAll(scrapeMajorRequirements(major));
        }

        return allRequirements;
    }

    public static List<MajorRequirement> scrapeMajorRequirements(MajorInfo major) {
        List<MajorRequirement> requirements = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(major.url).get();

            Elements tables = doc.select("table.sc_courselist");

            if (isBioengineeringMajor(major.name)) {
                if (!tables.isEmpty()) {
                    requirements.addAll(parseRequirementTable(major.name, tables.get(0)));
                }

                requirements = removeUnneededBioengineeringParsedRules(requirements);

                requirements.add(buildBioengineeringElectivesRule(major.name));
                requirements.add(buildBioengineeringEngineeringElectivesRule(major.name));
                requirements.add(buildBioengineeringSocialScienceRule(major.name));
                requirements.add(buildBioengineeringHumanitiesRule(major.name));
                requirements.add(buildBioengineeringSshTbsRule(major.name));
                requirements.add(buildBioengineeringFreeElectivesRule(major.name));

            } else {
                for (Element table : tables) {
                    requirements.addAll(parseRequirementTable(major.name, table));
                }

                if (isChemicalAndBiomolecularEngineeringMajor(major.name)) {
                    requirements = removeUnneededChemicalAndBiomolecularEngineeringParsedRules(requirements);

                    requirements.add(buildChemicalAndBiomolecularEngineeringLabElectiveRule(major.name));
                    requirements.add(buildChemicalAndBiomolecularEngineeringCbeElectiveRule(major.name));
                    requirements.add(buildChemicalAndBiomolecularEngineeringEngineeringElectiveRule(major.name));
                    requirements.add(buildChemicalAndBiomolecularEngineeringMathNaturalScienceEngineeringRule(major.name));
                }

                if (isComputerEngineeringMajor(major.name)) {
                    requirements = removeUnneededComputerEngineeringParsedRules(requirements);

                    requirements.add(buildComputerEngineeringCisEse2000PlusRule(major.name));
                    requirements.add(buildComputerEngineeringCisEse3000PlusRule(major.name));
                    requirements.add(buildComputerEngineeringMathOrNaturalScienceElectiveRule(major.name));
                    requirements.add(buildComputerEngineeringNaturalScienceLabRule(major.name));
                    requirements.add(buildComputerEngineeringMathNaturalScienceEngineeringRule(major.name));
                    requirements.add(buildComputerEngineeringProfessionalElectives3Rule(major.name));
                    requirements.add(buildComputerEngineeringSocialScienceHumanitiesRule(major.name));
                    requirements.add(buildComputerEngineeringSshTbsRule(major.name));
                }

                if (isComputerScienceBasMajor(major.name)) {
                    requirements = removeUnneededComputerScienceBasParsedRules(requirements);

                    requirements.add(buildComputerScienceBasCisNets1000PlusRule(major.name));
                    requirements.add(buildComputerScienceBasCisNets2000PlusRule(major.name));
                    requirements.add(buildComputerScienceBasCisProjectElectivesRule(major.name));
                    requirements.add(buildComputerScienceBasEngineeringElectiveRule(major.name));
                    requirements.add(buildComputerScienceBasNaturalSciencesRule(major.name));
                    requirements.add(buildComputerScienceBasMathNaturalScienceElectivesRule(major.name));
                    requirements.add(buildComputerScienceBasTechnicalElectivesRule(major.name));
                    requirements.add(buildComputerScienceBasSocialScienceHumanitiesRule(major.name));
                }

                if (isComputerScienceBseMajor(major.name)) {
                    requirements = removeUnneededComputerScienceBseParsedRules(requirements);

                    requirements.add(buildComputerScienceBseCisNets1000PlusRule(major.name));
                    requirements.add(buildComputerScienceBseCisNets2000PlusRule(major.name));
                    requirements.add(buildComputerScienceBseOperatingSystemsRule(major.name));
                    requirements.add(buildComputerScienceBseComputerOrganizationRule(major.name));
                    requirements.add(buildComputerScienceBseMathNaturalScienceElectiveRule(major.name));
                    requirements.add(buildComputerScienceBseTechnicalElectivesRule(major.name));
                    requirements.add(buildComputerScienceBseSocialScienceHumanitiesRule(major.name));
                    requirements.add(buildComputerScienceBseSshTbsRule(major.name));
                }

                if (isDigitalMediaDesignMajor(major.name)) {
                    requirements = removeUnneededDigitalMediaDesignParsedRules(requirements);

                    requirements.add(buildDigitalMediaDesignEngineeringTwoOfFollowingRule(major.name));
                    requirements.add(buildDigitalMediaDesignCisNets1000PlusRule(major.name));
                    requirements.add(buildDigitalMediaDesignCisNets2000PlusRule(major.name));
                    requirements.add(buildDigitalMediaDesignMathNaturalScienceFinalRule(major.name));
                    requirements.add(buildDigitalMediaDesignMathNaturalScienceElectiveRule(major.name));
                    requirements.add(buildDigitalMediaDesignElectivesRule(major.name));
                    requirements.add(buildDigitalMediaDesignSocialScienceHumanitiesRule(major.name));
                }

                if (isBiomedicalScienceMajor(major.name)) {
                    requirements = removeUnneededBiomedicalScienceParsedRules(requirements);

                    requirements.add(buildBiomedicalScienceBeElectivesRule(major.name));
                    requirements.add(buildBiomedicalScienceEngineeringElectivesRule(major.name));
                    requirements.add(buildBiomedicalScienceMathScienceEngineeringBusinessHealthRule(major.name));
                    requirements.add(buildBiomedicalScienceEthicsRule(major.name, doc));
                    requirements.add(buildBiomedicalScienceSocialScienceRule(major.name));
                    requirements.add(buildBiomedicalScienceHumanitiesRule(major.name));
                    requirements.add(buildBiomedicalScienceSshTbsRule(major.name));
                    requirements.add(buildBiomedicalScienceFreeElectivesRule(major.name));
                }
            }

            if (isArtificialIntelligenceMajor(major.name)) {
                requirements = removeRedundantArtificialIntelligenceNoteRules(requirements);
                requirements = removeGenericArtificialIntelligenceRules(requirements);
                requirements.addAll(buildArtificialIntelligenceRules(major.name));
            }

        } catch (Exception e) {
            System.out.println("Could not scrape major page: " + major.url);
            System.out.println(e.getMessage());
        }

        return requirements;
    }

    private static boolean isBioengineeringMajor(String majorName) {
        String lower = majorName.toLowerCase();

        return lower.contains("bioengineering")
                && lower.contains("bse");
    }

    private static boolean isBiomedicalScienceMajor(String majorName) {
        String lower = majorName.toLowerCase();

        return lower.contains("biomedical science")
                && lower.contains("bas");
    }

    private static boolean isChemicalAndBiomolecularEngineeringMajor(String majorName) {
        String lower = majorName.toLowerCase();

        return lower.contains("chemical and biomolecular engineering")
                && lower.contains("bse");
    }

    private static boolean isComputerEngineeringMajor(String majorName) {
        String lower = majorName.toLowerCase();

        return lower.contains("computer engineering")
                && lower.contains("bse");
    }

    private static boolean isComputerScienceBasMajor(String majorName) {
        String lower = majorName.toLowerCase();

        return lower.contains("computer science")
                && lower.contains("bas");
    }

    private static boolean isComputerScienceBseMajor(String majorName) {
        String lower = majorName.toLowerCase();

        return lower.contains("computer science")
                && lower.contains("bse")
                && !lower.contains("artificial intelligence");
    }

    private static boolean isDigitalMediaDesignMajor(String majorName) {
        String lower = majorName.toLowerCase();

        return lower.contains("digital media design")
                && lower.contains("bse");
    }

    private static boolean isAdvancedChemistryElectiveRow(String rowText, List<String> courseCodes) {
        String lower = rowText.toLowerCase();

        if (!lower.contains("advanced chemistry elective")) {
            return false;
        }

        Set<String> advancedChemistryOptions = new HashSet<>(Arrays.asList(
                "CHEM 2420",
                "CHEM 2421",
                "CHEM 2210",
                "MSE 2210"
        ));

        int matches = 0;

        for (String courseCode : courseCodes) {
            if (advancedChemistryOptions.contains(courseCode)) {
                matches++;
            }
        }

        return matches >= 2;
    }

    private static List<MajorRequirement> removeUnneededChemicalAndBiomolecularEngineeringParsedRules(
            List<MajorRequirement> requirements) {

        List<MajorRequirement> cleaned = new ArrayList<>();

        for (MajorRequirement req : requirements) {
            String note = "";
            String sectionName = "";

            if (req.note != null) {
                note = req.note.trim().toLowerCase();
            }

            if (req.sectionName != null) {
                sectionName = req.sectionName.trim().toLowerCase();
            }

            if (note.equals("code title course units")) {
                continue;
            }

            if (note.startsWith("select 4 courses from the following list")) {
                continue;
            }

            if (isUnneededCbeParsedConcentrationOrTrackRow(req)) {
                continue;
            }

            if (note.equals("writing requirement")
                    || (sectionName.equals("general electives") && note.equals("writing requirement"))) {
                continue;
            }

            if (note.contains("advanced chemistry elective")) {
                cleaned.add(req);
                continue;
            }

            if (note.contains("lab elective")
                    || note.contains("chem 2411")
                    || note.contains("chem 2421")
                    || note.contains("chem 2230")
                    || note.contains("cbe 4800")
                    || note.contains("cbe 3300")
                    || sectionName.contains("lab elective")) {
                continue;
            }

            if (note.contains("select 2 math, natural science or engineering courses")
                    || note.contains("select 2 math, natural science, or engineering courses")
                    || note.contains("math, natural science or engineering")
                    || note.contains("math, natural science, or engineering")
                    || (note.contains("math") && note.contains("natural science") && note.contains("engineering"))
                    || (sectionName.contains("math") && sectionName.contains("natural science") && sectionName.contains("engineering"))) {
                continue;
            }

            if (note.contains("cbe elective")
                    || note.contains("cbe electives")
                    || sectionName.contains("cbe elective")
                    || sectionName.contains("cbe electives")) {
                continue;
            }

            if (note.contains("engineering elective")
                    || note.contains("engineering electives")
                    || note.contains("seas engineering")
                    || note.contains("eung")
                    || sectionName.contains("engineering elective")
                    || sectionName.contains("engineering electives")) {
                continue;
            }

            cleaned.add(req);
        }

        return cleaned;
    }

    private static boolean isUnneededCbeParsedConcentrationOrTrackRow(MajorRequirement req) {
        if (req.sectionName == null || req.options == null) {
            return false;
        }

        String sectionName = req.sectionName.trim().toLowerCase();

        if (!sectionName.equals("general requirements")) {
            return false;
        }

        Set<String> unneededCourses = new HashSet<>(Arrays.asList(
                "CBE 0099",
                "CBE 3250",
                "CBE 3750",
                "CBE 5050",
                "CBE 5440",
                "CBE 5450",
                "CBE 5460",

                "EAS 3010",
                "EAS 4010",
                "EAS 4020",
                "EAS 4030",
                "ENGR 2500",
                "ESE 5210",
                "ENGR 5030",
                "ENVS 1000",
                "ESE 5670",
                "OIDD 2610",
                "MEAM 5020",
                "MEAM 2250",
                "MEAM 5030",
                "MSE 4550",
                "MSE 5550",
                "MSE 5450",
                "EESC 1030",

                "BE 3060",
                "BE 5530",
                "BIOL 2010",
                "BIOL 2210",
                "BIOL 2801",
                "BIOL 4210",
                "BIOL 4825",
                "CBE 1500",
                "CBE 4470",
                "CBE 4790",
                "CBE 4800",
                "CBE 5170",
                "CBE 5270",
                "CBE 5400",
                "CBE 5470",
                "CBE 5540",
                "CBE 5550",
                "CBE 5560",
                "CBE 5570",
                "CBE 5590",
                "CBE 5620",
                "CBE 5640",
                "CHEM 2510",
                "CHEM 5510",
                "ENGR 4500",
                "ENGR 5500",
                "PHYS 2280",

                "CBE 4300",
                "CBE 5110",
                "CBE 5220",
                "CBE 5250",
                "CBE 5350",
                "CBE 5700",
                "MSE 3300",
                "MSE 3600",
                "MSE 3930",
                "MSE 4300",
                "MEAM 5360"
        ));

        for (String course : req.options) {
            if (unneededCourses.contains(course)) {
                return true;
            }
        }

        return false;
    }

    private static MajorRequirement buildChemicalAndBiomolecularEngineeringLabElectiveRule(String majorName) {
        List<List<String>> labElectiveGroups = groups(
                group("CHEM 2411", "CHEM 2421"),
                group("CHEM 2230"),
                group("CBE 4800"),
                group("CBE 3300A", "CBE 3300B")
        );

        return new MajorRequirement(
                majorName,
                "Lab Elective",
                "CHOOSE_ONE_FROM_GROUPS",
                1,
                labElectiveGroups,
                "Choose one lab elective option. CHEM 2411 and CHEM 2421 together count as one course unit; CBE 3300A and CBE 3300B together count as one course unit.",
                true
        );
    }

    private static MajorRequirement buildChemicalAndBiomolecularEngineeringCbeElectiveRule(String majorName) {
        List<String> cbeElectives = getCbe3000PlusElectives();

        return new MajorRequirement(
                majorName,
                "CBE Elective (3000 level or above)",
                "CHOOSE_ONE",
                1,
                cbeElectives,
                "Choose 1 CBE elective at the 3000-level or higher."
        );
    }

    private static MajorRequirement buildChemicalAndBiomolecularEngineeringEngineeringElectiveRule(String majorName) {
        List<String> engineeringElectives = getEngineeringElectiveCourses();

        return new MajorRequirement(
                majorName,
                "Engineering Elective",
                "CHOOSE_ONE",
                1,
                engineeringElectives,
                "Choose 1 Engineering Elective. Uses the same approved Engineering Elective list as Bioengineering."
        );
    }

    private static MajorRequirement buildChemicalAndBiomolecularEngineeringMathNaturalScienceEngineeringRule(String majorName) {
        List<String> options = getMathNaturalScienceEngineeringCourses();

        return new MajorRequirement(
                majorName,
                "2 Math, Natural Science or Engineering courses",
                "CHOOSE_N",
                2,
                options,
                "Select 2 Math, Natural Science or Engineering courses. Combines approved Mathematics courses, Natural Science courses, and Engineering Elective courses."
        );
    }

    private static List<String> getCbe3000PlusElectives() {
        Set<String> electives = new LinkedHashSet<>();

        for (RawCourse course : courseCatalog) {
            String courseId = course.getCourseId();

            String department = getDepartmentCode(courseId);
            int number = getCourseNumber(courseId);

            if (!department.equals("CBE")) {
                continue;
            }

            if (number < 3000) {
                continue;
            }

            electives.add(courseId);
        }

        return new ArrayList<>(electives);
    }

    private static List<String> getMathNaturalScienceEngineeringCourses() {
        Set<String> courses = new LinkedHashSet<>();

        for (String course : scrapeMathCoursesFromCatalog()) {
            courses.add(course);
        }

        for (String course : getNaturalScienceCourses()) {
            courses.add(course);
        }

        for (String course : getEngineeringElectiveCourses()) {
            courses.add(course);
        }

        return new ArrayList<>(courses);
    }

    private static List<String> getMathOrNaturalScienceCourses() {
        Set<String> courses = new LinkedHashSet<>();

        for (String course : scrapeMathCoursesFromCatalog()) {
            courses.add(course);
        }

        for (String course : getNaturalScienceCourses()) {
            courses.add(course);
        }

        return new ArrayList<>(courses);
    }

    private static List<MajorRequirement> removeUnneededComputerEngineeringParsedRules(
            List<MajorRequirement> requirements) {

        List<MajorRequirement> cleaned = new ArrayList<>();

        for (MajorRequirement req : requirements) {
            String note = "";
            String sectionName = "";
            String ruleType = "";

            if (req.note != null) {
                note = req.note.trim().toLowerCase();
            }

            if (req.sectionName != null) {
                sectionName = req.sectionName.trim().toLowerCase();
            }

            if (req.ruleType != null) {
                ruleType = req.ruleType.trim().toLowerCase();
            }

            if (note.equals("code title course units")) {
                continue;
            }

            if (isComputerEngineeringConcentrationSection(sectionName)) {
                continue;
            }

            if (ruleType.equals("note_or_category_rule")
                    && sectionName.startsWith("professional electives")
                    && note.startsWith("select one of the following")) {
                continue;
            }

            if (note.contains("natural science lab")
                    || sectionName.contains("natural science lab")) {
                continue;
            }

            if (ruleType.equals("note_or_category_rule")
                    && sectionName.equals("engineering")
                    && note.contains("select 1 cu")
                    && note.contains("2000")
                    && note.contains("cis")
                    && note.contains("ese")) {
                continue;
            }

            if (ruleType.equals("note_or_category_rule")
                    && sectionName.equals("engineering")
                    && note.contains("select 2 cu")
                    && note.contains("3000")
                    && note.contains("cis")
                    && note.contains("ese")) {
                continue;
            }

            if (note.contains("math or natural science elective")
                    || note.contains("math/natural science elective")
                    || note.contains("mathematics or natural science elective")
                    || note.contains("math or science elective")
                    || note.contains("math and natural science elective")
                    || sectionName.contains("math or natural science elective")
                    || sectionName.contains("math/natural science elective")
                    || sectionName.contains("mathematics or natural science elective")) {
                continue;
            }

            if (note.contains("select 2 math, natural science or engineering courses")
                    || note.contains("select 2 math, natural science, or engineering courses")
                    || note.contains("math, natural science or engineering")
                    || note.contains("math, natural science, or engineering")
                    || (note.contains("math") && note.contains("natural science") && note.contains("engineering"))
                    || (sectionName.contains("math") && sectionName.contains("natural science") && sectionName.contains("engineering"))) {
                continue;
            }

            if (note.contains("2000+ level cis or ese")
                    || note.contains("2000 + level cis or ese")
                    || note.contains("2000-level cis or ese")
                    || note.contains("2000 level cis or ese")
                    || note.contains("2000+ cis or ese")
                    || note.contains("2000+ level cis")
                    || note.contains("2000+ level ese")
                    || note.contains("select 1 cu of 2000")
                    || note.contains("1 cu of 2000")
                    || sectionName.contains("2000+ level cis or ese")
                    || sectionName.contains("2000-level cis or ese")) {
                continue;
            }

            if (note.contains("3000+ level cis or ese")
                    || note.contains("3000 + level cis or ese")
                    || note.contains("3000-level cis or ese")
                    || note.contains("3000 level cis or ese")
                    || note.contains("3000+ cis or ese")
                    || note.contains("3000+ level cis")
                    || note.contains("3000+ level ese")
                    || note.contains("select 2 cus of 3000")
                    || note.contains("select 2 cu of 3000")
                    || note.contains("2 cus of 3000")
                    || note.contains("2 cu of 3000")
                    || sectionName.contains("3000+ level cis or ese")
                    || sectionName.contains("3000-level cis or ese")) {
                continue;
            }

            if (note.contains("select 3 social science or humanities courses")
                    || note.contains("select three social science or humanities courses")
                    || note.contains("social science or humanities courses")
                    || (sectionName.equals("general electives")
                    && note.contains("social science or humanities"))) {
                continue;
            }

            if (note.contains("select 2 social science, humanities, or technology in business")
                    || note.contains("select 2 social science, humanities or technology in business")
                    || note.contains("select 2 social science or humanities or technology in business")
                    || note.contains("social science, humanities, or technology in business")
                    || note.contains("social science, humanities or technology in business")
                    || note.contains("social science or humanities or technology in business")
                    || note.contains("technology in business & society")
                    || note.contains("technology in business and society")
                    || ((sectionName.equals("general electives")
                    || sectionName.contains("general elective"))
                    && note.contains("technology in business"))) {
                continue;
            }

            if (isComputerEngineeringProfessionalElectives3ParsedCourseRow(req)) {
                continue;
            }

            cleaned.add(req);
        }

        return cleaned;
    }

    private static boolean isComputerEngineeringConcentrationSection(String sectionName) {
        if (sectionName == null) {
            return false;
        }

        String normalized = sectionName.trim().toLowerCase();

        return normalized.equals("ai & robotics concentration")
                || normalized.equals("ai and robotics concentration")
                || normalized.equals("chips concentration")
                || normalized.equals("networking and distributed systems concentration")
                || normalized.equals("security and safety concentration");
    }

    private static boolean isComputerEngineeringProfessionalElectives3ParsedCourseRow(MajorRequirement req) {
        if (req.sectionName == null || req.options == null) {
            return false;
        }

        String sectionName = req.sectionName.trim().toLowerCase();

        if (!sectionName.equals("professional electives 3")) {
            return false;
        }

        Set<String> professionalElectives3Courses = new HashSet<>(Arrays.asList(
                "ESE 4000",
                "EAS 5450",
                "EAS 5950",
                "MGMT 2370",
                "OIDD 2360"
        ));

        for (String course : req.options) {
            if (professionalElectives3Courses.contains(course)) {
                return true;
            }
        }

        return false;
    }

    private static List<MajorRequirement> removeUnneededComputerScienceBasParsedRules(
            List<MajorRequirement> requirements) {

        List<MajorRequirement> cleaned = new ArrayList<>();

        for (MajorRequirement req : requirements) {
            String note = "";
            String sectionName = "";
            String ruleType = "";

            if (req.note != null) {
                note = req.note.trim().toLowerCase();
            }

            if (req.sectionName != null) {
                sectionName = req.sectionName.trim().toLowerCase();
            }

            if (req.ruleType != null) {
                ruleType = req.ruleType.trim().toLowerCase();
            }

            if (ruleType.equals("note_or_category_rule")
                    && note.contains("1000")
                    && note.contains("cis")
                    && note.contains("nets")) {
                continue;
            }

            if (ruleType.equals("note_or_category_rule")
                    && note.contains("2000")
                    && note.contains("cis")
                    && note.contains("nets")) {
                continue;
            }

            if (note.contains("1000+ level cis or nets")
                    || note.contains("1000 + level cis or nets")
                    || note.contains("1000-level cis or nets")
                    || note.contains("1000 level cis or nets")
                    || note.contains("select 1 cu of 1000")) {
                continue;
            }

            if (note.contains("2000+ level cis or nets")
                    || note.contains("2000 + level cis or nets")
                    || note.contains("2000-level cis or nets")
                    || note.contains("2000 level cis or nets")
                    || note.contains("select 1 cu of 2000")) {
                continue;
            }

            if (isComputerScienceBasCisProjectElectiveParsedCourseRow(req)) {
                continue;
            }

            if (note.contains("engineering elective")
                    || note.contains("engineering electives")
                    || note.contains("seas engineering")
                    || note.contains("eung")
                    || sectionName.contains("engineering elective")
                    || sectionName.contains("engineering electives")) {
                continue;
            }

            if (isComputerScienceBasNaturalScienceParsedRule(req)) {
                continue;
            }

            if (note.contains("math/natural science electives")
                    || note.contains("math / natural science electives")
                    || note.contains("math and natural science electives")
                    || note.contains("math or natural science electives")
                    || note.contains("mathematics/natural science electives")
                    || note.contains("mathematics and natural science electives")
                    || note.contains("mathematics or natural science electives")
                    || ((sectionName.equals("math and natural science")
                    || sectionName.contains("math and natural science"))
                    && note.contains("elective"))) {
                continue;
            }

            if (note.contains("technical elective")
                    || note.contains("technical electives")
                    || note.contains("select 8 technical")
                    || note.contains("select eight technical")
                    || sectionName.contains("technical elective")
                    || sectionName.contains("technical electives")) {
                continue;
            }

            if (note.contains("select 3 social science or humanities courses")
                    || note.contains("select three social science or humanities courses")
                    || note.contains("social science or humanities courses")
                    || (sectionName.contains("general electives")
                    && note.contains("social science or humanities"))) {
                continue;
            }

            cleaned.add(req);
        }

        return cleaned;
    }

    private static boolean isComputerScienceBasCisProjectElectiveParsedCourseRow(MajorRequirement req) {
        if (req.sectionName == null || req.options == null) {
            return false;
        }

        String sectionName = req.sectionName.trim().toLowerCase();

        if (!sectionName.equals("engineering")) {
            return false;
        }

        Set<String> cisProjectElectiveCourses = new HashSet<>(Arrays.asList(
                "CIS 3500",
                "CIS 4120",
                "CIS 5120",
                "CIS 4410",
                "CIS 5410",
                "CIS 4480",
                "CIS 5480",
                "CIS 4500",
                "CIS 5500",
                "CIS 4521",
                "CIS 5521",
                "CIS 4550",
                "CIS 5550",
                "CIS 4600",
                "CIS 5600",
                "CIS 4710",
                "CIS 5710",
                "CIS 5050",
                "CIS 5530",
                "ESE 3500",
                "NETS 2120"
        ));

        for (String course : req.options) {
            if (cisProjectElectiveCourses.contains(course)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isComputerScienceBasNaturalScienceParsedRule(MajorRequirement req) {
        if (req.sectionName == null) {
            return false;
        }

        String sectionName = req.sectionName.trim().toLowerCase();
        String note = "";

        if (req.note != null) {
            note = req.note.trim().toLowerCase();
        }

        if (!sectionName.equals("math and natural science")) {
            return false;
        }

        if (note.contains("select two of the following natural sciences")) {
            return true;
        }

        if (req.options == null || req.options.isEmpty()) {
            return false;
        }

        Set<String> naturalScienceCourses = new HashSet<>(Arrays.asList(
                "PHYS 0140",
                "PHYS 0141",
                "EAS 0091",
                "CHEM 1012",
                "CHEM 1151",
                "BIOL 1101",
                "BIOL 1121"
        ));

        for (String course : req.options) {
            if (naturalScienceCourses.contains(course)) {
                return true;
            }
        }

        return false;
    }

    private static List<MajorRequirement> removeUnneededComputerScienceBseParsedRules(
            List<MajorRequirement> requirements) {

        List<MajorRequirement> cleaned = new ArrayList<>();

        for (MajorRequirement req : requirements) {
            String note = "";
            String sectionName = "";
            String ruleType = "";

            if (req.note != null) {
                note = req.note.trim().toLowerCase();
            }

            if (req.sectionName != null) {
                sectionName = req.sectionName.trim().toLowerCase();
            }

            if (req.ruleType != null) {
                ruleType = req.ruleType.trim().toLowerCase();
            }

            if (ruleType.equals("note_or_category_rule")
                    && note.contains("1000")
                    && note.contains("cis")
                    && note.contains("nets")) {
                continue;
            }

            if (ruleType.equals("note_or_category_rule")
                    && note.contains("2000")
                    && note.contains("cis")
                    && note.contains("nets")) {
                continue;
            }

            if (note.contains("1000+ level cis or nets")
                    || note.contains("1000 + level cis or nets")
                    || note.contains("1000-level cis or nets")
                    || note.contains("1000 level cis or nets")
                    || note.contains("select 1 cu of 1000")) {
                continue;
            }

            if (note.contains("2000+ level cis or nets")
                    || note.contains("2000 + level cis or nets")
                    || note.contains("2000-level cis or nets")
                    || note.contains("2000 level cis or nets")
                    || note.contains("select 3 cu of 2000")
                    || note.contains("select 3 cus of 2000")
                    || note.contains("3 cu of 2000")
                    || note.contains("3 cus of 2000")) {
                continue;
            }

            if (isComputerScienceBseCrossListedRequiredCourseRow(req)) {
                continue;
            }

            if (note.contains("math/natural science elective")
                    || note.contains("math / natural science elective")
                    || note.contains("math and natural science elective")
                    || note.contains("math or natural science elective")
                    || note.contains("mathematics/natural science elective")
                    || note.contains("mathematics and natural science elective")
                    || note.contains("mathematics or natural science elective")
                    || ((sectionName.equals("math and natural science")
                    || sectionName.contains("math and natural science"))
                    && note.contains("elective"))) {
                continue;
            }

            if (note.contains("technical elective")
                    || note.contains("technical electives")
                    || note.contains("select 6 technical")
                    || note.contains("select six technical")
                    || sectionName.contains("technical elective")
                    || sectionName.contains("technical electives")) {
                continue;
            }

            if (note.contains("select 3 social science or humanities courses")
                    || note.contains("select three social science or humanities courses")
                    || note.contains("social science or humanities courses")
                    || (sectionName.contains("general electives")
                    && note.contains("social science or humanities"))) {
                continue;
            }

            if (note.contains("select 2 social science, humanities, or technology in business")
                    || note.contains("select 2 social science, humanities or technology in business")
                    || note.contains("select 2 social science or humanities or technology in business")
                    || note.contains("social science, humanities, or technology in business")
                    || note.contains("social science, humanities or technology in business")
                    || note.contains("social science or humanities or technology in business")
                    || note.contains("technology in business & society")
                    || note.contains("technology in business and society")
                    || ((sectionName.contains("general electives")
                    || sectionName.contains("general elective"))
                    && note.contains("technology in business"))) {
                continue;
            }

            cleaned.add(req);
        }

        return cleaned;
    }

    private static boolean isComputerScienceBseCrossListedRequiredCourseRow(MajorRequirement req) {
        if (req.sectionName == null || req.options == null) {
            return false;
        }

        String sectionName = req.sectionName.trim().toLowerCase();

        if (!sectionName.equals("engineering")) {
            return false;
        }

        Set<String> crossListedCourses = new HashSet<>(Arrays.asList(
                "CIS 4480",
                "CIS 5480",
                "CIS 4710",
                "CIS 5710"
        ));

        for (String course : req.options) {
            if (crossListedCourses.contains(course)) {
                return true;
            }
        }

        return false;
    }

    private static List<MajorRequirement> removeUnneededDigitalMediaDesignParsedRules(
            List<MajorRequirement> requirements) {

        List<MajorRequirement> cleaned = new ArrayList<>();

        for (MajorRequirement req : requirements) {
            String note = "";
            String sectionName = "";

            if (req.note != null) {
                note = req.note.trim().toLowerCase();
            }

            if (req.sectionName != null) {
                sectionName = req.sectionName.trim().toLowerCase();
            }

            if (sectionName.equals("engineering")
                    && note.contains("cis 4610")
                    && note.contains("cis 5610")
                    && note.contains("cis 4620")
                    && note.contains("cis 5620")
                    && note.contains("cis 4550")
                    && note.contains("cis 5550")) {
                continue;
            }

            if (note.contains("1000+ level cis or nets")
                    || note.contains("1000 + level cis or nets")
                    || note.contains("1000-level cis or nets")
                    || note.contains("1000 level cis or nets")
                    || note.contains("select 1 cu of 1000")) {
                continue;
            }

            if (note.contains("2000+ level cis or nets")
                    || note.contains("2000 + level cis or nets")
                    || note.contains("2000-level cis or nets")
                    || note.contains("2000 level cis or nets")
                    || note.contains("select 3 cu of 2000")
                    || note.contains("select 3 cus of 2000")
                    || note.contains("3 cu of 2000")
                    || note.contains("3 cus of 2000")) {
                continue;
            }

            if (isDigitalMediaDesignMathNaturalScienceFinalParsedRule(req)) {
                continue;
            }

            if (note.contains("math/natural science elective")
                    || note.contains("math / natural science elective")
                    || note.contains("math and natural science elective")
                    || note.contains("math or natural science elective")
                    || note.contains("mathematics/natural science elective")
                    || note.contains("mathematics and natural science elective")
                    || note.contains("mathematics or natural science elective")
                    || ((sectionName.equals("math & natural science")
                    || sectionName.equals("math and natural science")
                    || sectionName.contains("math"))
                    && note.contains("elective"))) {
                continue;
            }

            if (note.contains("dmd elective")
                    || note.contains("dmd electives")
                    || note.contains("select 3 dmd")
                    || note.contains("select three dmd")
                    || sectionName.contains("dmd elective")
                    || sectionName.contains("dmd electives")) {
                continue;
            }

            if (note.contains("select 4 social science or humanities courses")
                    || note.contains("select four social science or humanities courses")
                    || note.contains("social science or humanities courses")
                    || (sectionName.contains("general electives")
                    && note.contains("social science or humanities"))) {
                continue;
            }

            cleaned.add(req);
        }

        return cleaned;
    }

    private static boolean isDigitalMediaDesignMathNaturalScienceFinalParsedRule(MajorRequirement req) {
        if (req.sectionName == null) {
            return false;
        }

        String sectionName = req.sectionName.trim().toLowerCase();
        String note = "";

        if (req.note != null) {
            note = req.note.trim().toLowerCase();
        }

        if (!sectionName.equals("math & natural science")
                && !sectionName.equals("math and natural science")) {
            return false;
        }

        if (note.contains("select from the following list")) {
            return true;
        }

        if (req.options == null || req.options.isEmpty()) {
            return false;
        }

        Set<String> mathNaturalScienceFinalCourses = new HashSet<>(Arrays.asList(
                "BIOL 1101",
                "BIOL 1121",
                "BIOL 1124",
                "CHEM 1012",
                "CHEM 1101",
                "ESE 1120",
                "PHYS 0151",
                "PHYS 0171"
        ));

        for (String course : req.options) {
            if (mathNaturalScienceFinalCourses.contains(course)) {
                return true;
            }
        }

        return false;
    }

    private static MajorRequirement buildComputerEngineeringCisEse2000PlusRule(String majorName) {
        List<String> options = getCisEseCoursesAtOrAbove(2000);

        return new MajorRequirement(
                majorName,
                "1 CU of 2000+ level CIS or ESE engineering courses",
                "CHOOSE_ONE",
                1,
                options,
                "Select 1 CU of 2000+ level CIS or ESE engineering courses."
        );
    }

    private static MajorRequirement buildComputerEngineeringCisEse3000PlusRule(String majorName) {
        List<String> options = getCisEseCoursesAtOrAbove(3000);

        return new MajorRequirement(
                majorName,
                "2 CUs of 3000+ level CIS or ESE engineering courses",
                "CHOOSE_N",
                2,
                options,
                "Select 2 CUs of 3000+ level CIS or ESE engineering courses."
        );
    }

    private static MajorRequirement buildComputerEngineeringMathOrNaturalScienceElectiveRule(String majorName) {
        List<String> options = getMathOrNaturalScienceCourses();

        return new MajorRequirement(
                majorName,
                "Math or Natural Science Elective",
                "CHOOSE_ONE",
                1,
                options,
                "Select Math or Natural Science Elective. Combines approved Mathematics courses and Natural Science courses."
        );
    }

    private static MajorRequirement buildComputerEngineeringNaturalScienceLabRule(String majorName) {
        List<String> options = new ArrayList<>(Arrays.asList(
                "BIOL 1124",
                "CHEM 1101",
                "MEAM 1470",
                "PHYS 0050"
        ));

        return new MajorRequirement(
                majorName,
                "Natural Science Lab",
                "CHOOSE_ONE",
                1,
                options,
                "Choose one Natural Science Lab: BIOL 1124, CHEM 1101, MEAM 1470, or PHYS 0050."
        );
    }

    private static MajorRequirement buildComputerEngineeringMathNaturalScienceEngineeringRule(String majorName) {
        List<String> options = getMathNaturalScienceEngineeringCourses();

        return new MajorRequirement(
                majorName,
                "2 Math, Natural Science or Engineering courses",
                "CHOOSE_N",
                2,
                options,
                "Select 2 Math, Natural Science or Engineering courses. Combines approved Mathematics courses, Natural Science courses, and Engineering Elective courses."
        );
    }

    private static MajorRequirement buildComputerEngineeringProfessionalElectives3Rule(String majorName) {
        List<String> options = new ArrayList<>(Arrays.asList(
                "ESE 4000",
                "EAS 5450",
                "EAS 5950",
                "MGMT 2370",
                "OIDD 2360"
        ));

        return new MajorRequirement(
                majorName,
                "Professional Electives 3",
                "CHOOSE_ONE",
                1,
                options,
                "Choose one Professional Electives 3 course."
        );
    }

    private static MajorRequirement buildComputerEngineeringSocialScienceHumanitiesRule(String majorName) {
        List<String> sshCourses = scrapeSocialScienceHumanitiesCoursesFromCatalog();

        return new MajorRequirement(
                majorName,
                "General Electives",
                "CHOOSE_N",
                3,
                sshCourses,
                "Select 3 Social Science or Humanities courses."
        );
    }

    private static MajorRequirement buildComputerEngineeringSshTbsRule(String majorName) {
        List<String> sshTbsCourses = getSocialScienceHumanitiesAndTechnologyBusinessSocietyCourses();

        return new MajorRequirement(
                majorName,
                "General Electives",
                "CHOOSE_N",
                2,
                sshTbsCourses,
                "Select 2 Social Science, Humanities, or Technology in Business & Society courses."
        );
    }

    private static List<String> getCisEseCoursesAtOrAbove(int minimumNumber) {
        Set<String> courses = new LinkedHashSet<>();

        for (RawCourse course : courseCatalog) {
            String courseId = course.getCourseId();

            String department = getDepartmentCode(courseId);
            int number = getCourseNumber(courseId);

            if (!(department.equals("CIS") || department.equals("ESE"))) {
                continue;
            }

            if (number < minimumNumber) {
                continue;
            }

            courses.add(courseId);
        }

        return new ArrayList<>(courses);
    }

    private static MajorRequirement buildComputerScienceBasCisNets1000PlusRule(String majorName) {
        List<String> options = getCisNetsCoursesAtOrAbove(1000);

        return new MajorRequirement(
                majorName,
                "1 CU of 1000+ level CIS or NETS engineering courses",
                "CHOOSE_ONE",
                1,
                options,
                "Select 1 CU of 1000+ level CIS or NETS engineering courses."
        );
    }

    private static MajorRequirement buildComputerScienceBasCisNets2000PlusRule(String majorName) {
        List<String> options = getCisNetsCoursesAtOrAbove(2000);

        return new MajorRequirement(
                majorName,
                "1 CU of 2000+ level CIS or NETS engineering courses",
                "CHOOSE_ONE",
                1,
                options,
                "Select 1 CU of 2000+ level CIS or NETS engineering courses."
        );
    }

    private static MajorRequirement buildComputerScienceBasCisProjectElectivesRule(String majorName) {
        List<List<String>> projectElectiveGroups = groups(
                group("CIS 3500"),
                group("CIS 4120", "CIS 5120"),
                group("CIS 4410", "CIS 5410"),
                group("CIS 4480", "CIS 5480"),
                group("CIS 4500", "CIS 5500"),
                group("CIS 4521", "CIS 5521"),
                group("CIS 4550", "CIS 5550"),
                group("CIS 4600", "CIS 5600"),
                group("CIS 4710", "CIS 5710"),
                group("CIS 5050"),
                group("CIS 5530"),
                group("ESE 3500"),
                group("NETS 2120")
        );

        return new MajorRequirement(
                majorName,
                "CIS Project Electives",
                "CHOOSE_N_FROM_GROUPS",
                2,
                projectElectiveGroups,
                "Choose 2 CIS Project Electives. Courses in the same option group are alternatives and should not both be selected.",
                true
        );
    }

    private static MajorRequirement buildComputerScienceBasEngineeringElectiveRule(String majorName) {
        List<String> engineeringElectives = getEngineeringElectiveCourses();

        return new MajorRequirement(
                majorName,
                "Engineering Elective",
                "CHOOSE_N",
                2,
                engineeringElectives,
                "Choose 2 Engineering Electives. Uses the same approved Engineering Elective list as Bioengineering."
        );
    }

    private static MajorRequirement buildComputerScienceBasNaturalSciencesRule(String majorName) {
        List<List<String>> naturalScienceGroups = groups(
                group("PHYS 0140"),
                group("PHYS 0141"),
                group("EAS 0091", "CHEM 1012", "CHEM 1151"),
                group("BIOL 1101", "BIOL 1121")
        );

        return new MajorRequirement(
                majorName,
                "Natural Sciences",
                "CHOOSE_N_FROM_GROUPS",
                2,
                naturalScienceGroups,
                "Choose 2 Natural Sciences. Courses in the same option group are alternatives and should not both be selected.",
                true
        );
    }

    private static MajorRequirement buildComputerScienceBasMathNaturalScienceElectivesRule(String majorName) {
        List<String> options = getMathOrNaturalScienceCourses();

        return new MajorRequirement(
                majorName,
                "Math/Natural Science Electives",
                "CHOOSE_N",
                3,
                options,
                "Choose 3 Math/Natural Science Electives. Combines approved Mathematics courses and Natural Science courses."
        );
    }

    private static MajorRequirement buildComputerScienceBasTechnicalElectivesRule(String majorName) {
        List<String> options = getMathNaturalScienceEngineeringCourses();

        return new MajorRequirement(
                majorName,
                "Technical Electives",
                "CHOOSE_N",
                8,
                options,
                "Choose 8 Technical Electives. Combines approved Mathematics courses, Natural Science courses, and Engineering Elective courses."
        );
    }

    private static MajorRequirement buildComputerScienceBasSocialScienceHumanitiesRule(String majorName) {
        List<String> sshCourses = scrapeSocialScienceHumanitiesCoursesFromCatalog();

        return new MajorRequirement(
                majorName,
                "General Electives",
                "CHOOSE_N",
                3,
                sshCourses,
                "Select 3 Social Science or Humanities courses."
        );
    }

    private static MajorRequirement buildComputerScienceBseCisNets1000PlusRule(String majorName) {
        List<String> options = getCisNetsCoursesAtOrAbove(1000);

        return new MajorRequirement(
                majorName,
                "1 CU of 1000+ level CIS or NETS engineering courses",
                "CHOOSE_ONE",
                1,
                options,
                "Select 1 CU of 1000+ level CIS or NETS engineering courses."
        );
    }

    private static MajorRequirement buildComputerScienceBseCisNets2000PlusRule(String majorName) {
        List<String> options = getCisNetsCoursesAtOrAbove(2000);

        return new MajorRequirement(
                majorName,
                "3 CUs of 2000+ level CIS or NETS engineering courses",
                "CHOOSE_N",
                3,
                options,
                "Select 3 CUs of 2000+ level CIS or NETS engineering courses."
        );
    }

    private static MajorRequirement buildComputerScienceBseOperatingSystemsRule(String majorName) {
        List<String> options = new ArrayList<>(Arrays.asList(
                "CIS 4480",
                "CIS 5480"
        ));

        return new MajorRequirement(
                majorName,
                "Operating Systems Design and Implementation",
                "CHOOSE_ONE",
                1,
                options,
                "Choose one: CIS 4480 or CIS 5480."
        );
    }

    private static MajorRequirement buildComputerScienceBseComputerOrganizationRule(String majorName) {
        List<String> options = new ArrayList<>(Arrays.asList(
                "CIS 4710",
                "CIS 5710"
        ));

        return new MajorRequirement(
                majorName,
                "Computer Organization and Design",
                "CHOOSE_ONE",
                1,
                options,
                "Choose one: CIS 4710 or CIS 5710."
        );
    }

    private static MajorRequirement buildComputerScienceBseMathNaturalScienceElectiveRule(String majorName) {
        List<String> options = getMathOrNaturalScienceCourses();

        return new MajorRequirement(
                majorName,
                "Math/Natural Science Elective",
                "CHOOSE_ONE",
                1,
                options,
                "Choose 1 Math/Natural Science Elective. Combines approved Mathematics courses and Natural Science courses."
        );
    }

    private static MajorRequirement buildComputerScienceBseTechnicalElectivesRule(String majorName) {
        List<String> options = getMathNaturalScienceEngineeringCourses();

        return new MajorRequirement(
                majorName,
                "Technical Electives",
                "CHOOSE_N",
                6,
                options,
                "Choose 6 Technical Electives. Combines approved Mathematics courses, Natural Science courses, and Engineering Elective courses."
        );
    }

    private static MajorRequirement buildComputerScienceBseSocialScienceHumanitiesRule(String majorName) {
        List<String> sshCourses = scrapeSocialScienceHumanitiesCoursesFromCatalog();

        return new MajorRequirement(
                majorName,
                "General Electives",
                "CHOOSE_N",
                3,
                sshCourses,
                "Select 3 Social Science or Humanities courses."
        );
    }

    private static MajorRequirement buildComputerScienceBseSshTbsRule(String majorName) {
        List<String> sshTbsCourses = getSocialScienceHumanitiesAndTechnologyBusinessSocietyCourses();

        return new MajorRequirement(
                majorName,
                "General Electives",
                "CHOOSE_N",
                2,
                sshTbsCourses,
                "Select 2 Social Science, Humanities, or Technology in Business & Society courses."
        );
    }

    private static MajorRequirement buildDigitalMediaDesignEngineeringTwoOfFollowingRule(String majorName) {
        List<String> options = new ArrayList<>(Arrays.asList(
                "CIS 4610",
                "CIS 5610",
                "CIS 4620",
                "CIS 5620",
                "CIS 4550",
                "CIS 5550"
        ));

        return new MajorRequirement(
                majorName,
                "Engineering",
                "CHOOSE_N",
                2,
                options,
                "Select two of the following: CIS 4610, CIS 5610, CIS 4620, CIS 5620, CIS 4550, or CIS 5550."
        );
    }

    private static MajorRequirement buildDigitalMediaDesignCisNets1000PlusRule(String majorName) {
        List<String> options = getCisNetsCoursesAtOrAbove(1000);

        return new MajorRequirement(
                majorName,
                "1 CU of 1000+ level CIS or NETS engineering courses",
                "CHOOSE_ONE",
                1,
                options,
                "Select 1 CU of 1000+ level CIS or NETS engineering courses."
        );
    }

    private static MajorRequirement buildDigitalMediaDesignCisNets2000PlusRule(String majorName) {
        List<String> options = getCisNetsCoursesAtOrAbove(2000);

        return new MajorRequirement(
                majorName,
                "3 CUs of 2000+ level CIS or NETS engineering courses",
                "CHOOSE_N",
                3,
                options,
                "Select 3 CUs of 2000+ level CIS or NETS engineering courses."
        );
    }

    private static MajorRequirement buildDigitalMediaDesignMathNaturalScienceFinalRule(String majorName) {
        List<List<String>> mathNaturalScienceGroups = groups(
                group("BIOL 1101"),
                group("BIOL 1121", "BIOL 1124"),
                group("CHEM 1012", "CHEM 1101"),
                group("ESE 1120"),
                group("PHYS 0151"),
                group("PHYS 0171")
        );

        return new MajorRequirement(
                majorName,
                "Math & Natural Science",
                "CHOOSE_ONE_FROM_GROUPS",
                1,
                mathNaturalScienceGroups,
                "Choose one Math & Natural Science option. BIOL 1121 and BIOL 1124 must be taken together; CHEM 1012 and CHEM 1101 must be taken together.",
                true
        );
    }

    private static MajorRequirement buildDigitalMediaDesignMathNaturalScienceElectiveRule(String majorName) {
        List<String> options = getMathOrNaturalScienceCourses();

        return new MajorRequirement(
                majorName,
                "Math/Natural Science Elective",
                "CHOOSE_ONE",
                1,
                options,
                "Choose 1 Math/Natural Science Elective. Combines approved Mathematics courses and Natural Science courses."
        );
    }

    private static MajorRequirement buildDigitalMediaDesignElectivesRule(String majorName) {
        List<String> dmdElectives = getDigitalMediaDesignElectiveCourses();

        return new MajorRequirement(
                majorName,
                "DMD Electives",
                "CHOOSE_N",
                3,
                dmdElectives,
                "Select 3 DMD Electives from COMM, FNAR, CIMS, DSGN, THAR, MKTG, ARTH, IPD, MUSC, or EDUC courses."
        );
    }

    private static MajorRequirement buildDigitalMediaDesignSocialScienceHumanitiesRule(String majorName) {
        List<String> sshCourses = scrapeSocialScienceHumanitiesCoursesFromCatalog();

        return new MajorRequirement(
                majorName,
                "General Electives",
                "CHOOSE_N",
                4,
                sshCourses,
                "Select 4 Social Science or Humanities courses."
        );
    }

    private static List<String> getDigitalMediaDesignElectiveCourses() {
        Set<String> allowedDepartments = new HashSet<>(Arrays.asList(
                "COMM",
                "FNAR",
                "CIMS",
                "DSGN",
                "THAR",
                "MKTG",
                "ARTH",
                "IPD",
                "MUSC",
                "EDUC"
        ));

        Set<String> electives = new LinkedHashSet<>();

        for (RawCourse course : courseCatalog) {
            String courseId = course.getCourseId();
            String department = getDepartmentCode(courseId);

            if (allowedDepartments.contains(department)) {
                electives.add(courseId);
            }
        }

        return new ArrayList<>(electives);
    }

    private static List<String> getCisNetsCoursesAtOrAbove(int minimumNumber) {
        Set<String> courses = new LinkedHashSet<>();

        for (RawCourse course : courseCatalog) {
            String courseId = course.getCourseId();

            String department = getDepartmentCode(courseId);
            int number = getCourseNumber(courseId);

            if (!(department.equals("CIS") || department.equals("NETS"))) {
                continue;
            }

            if (number < minimumNumber) {
                continue;
            }

            courses.add(courseId);
        }

        return new ArrayList<>(courses);
    }

    private static List<MajorRequirement> removeUnneededBioengineeringParsedRules(
            List<MajorRequirement> requirements) {

        List<MajorRequirement> cleaned = new ArrayList<>();

        for (MajorRequirement req : requirements) {
            String note = "";

            if (req.note != null) {
                note = req.note.trim().toLowerCase();
            }

            if (note.equals("select 1 course unit of free elective")) {
                continue;
            }

            if (note.equals("select 2 social science or humanities courses")) {
                continue;
            }

            if (note.startsWith("select 2 social science courses")) {
                continue;
            }

            if (note.startsWith("select 2 humanities courses")) {
                continue;
            }

            if (note.startsWith("select 1 social science, humanities or technology in business")) {
                continue;
            }

            if (note.equals("code title course units")) {
                continue;
            }

            if (note.startsWith("select 2 courses in:")) {
                continue;
            }

            if (note.startsWith("select 2 additional courses in:")) {
                continue;
            }

            if (note.startsWith("select two additional courses in:")) {
                continue;
            }

            cleaned.add(req);
        }

        return cleaned;
    }

    private static List<MajorRequirement> removeUnneededBiomedicalScienceParsedRules(
            List<MajorRequirement> requirements) {

        List<MajorRequirement> cleaned = new ArrayList<>();

        for (MajorRequirement req : requirements) {
            String note = "";
            String sectionName = "";

            if (req.note != null) {
                note = req.note.trim().toLowerCase();
            }

            if (req.sectionName != null) {
                sectionName = req.sectionName.trim().toLowerCase();
            }

            if (note.equals("writing requirement")) {
                cleaned.add(req);
                continue;
            }

            if (isBiomedicalScienceEthicsParsedRule(req)) {
                continue;
            }

            if (sectionName.equals("professional electives")) {
                continue;
            }

            if (note.startsWith("select any 3 math, science, engineering, business")) {
                continue;
            }

            if (sectionName.equals("general electives")) {
                continue;
            }

            if (note.startsWith("select 2 social science courses")) {
                continue;
            }

            if (note.startsWith("select 2 humanities courses")) {
                continue;
            }

            if (note.startsWith("select 1 social science or humanities or technology in business")) {
                continue;
            }

            if (note.startsWith("select 1 social science, humanities or technology in business")) {
                continue;
            }

            if (note.equals("select 1 course unit of free elective")) {
                continue;
            }

            if (sectionName.equals("general requirements")
                    && isBiomedicalScienceProfessionalElectiveCourseRow(req)) {
                continue;
            }

            cleaned.add(req);
        }

        return cleaned;
    }

    private static boolean isBiomedicalScienceEthicsParsedRule(MajorRequirement req) {
        if (req.options == null || req.options.isEmpty()) {
            return false;
        }

        int ethicsCourseMatches = 0;

        for (String course : req.options) {
            if (course.equals("EAS 2030")
                    || course.equals("BIOE 4010")
                    || course.equals("HSOC 1330")
                    || course.equals("HSOC 2457")
                    || course.equals("LGST 1000")
                    || course.equals("LGST 2200")
                    || course.equals("NURS 3300")
                    || course.equals("NURS 5250")
                    || course.equals("PHIL 1342")
                    || course.equals("PHIL 4330")) {
                ethicsCourseMatches++;
            }
        }

        return ethicsCourseMatches >= 3;
    }

    private static boolean isBiomedicalScienceProfessionalElectiveCourseRow(MajorRequirement req) {
        if (req.options == null || req.options.isEmpty()) {
            return false;
        }

        Set<String> unwantedCourses = new HashSet<>(Arrays.asList(
                "BE 5270",
                "BE 5570",
                "ENGR 4500",
                "IMUN 5060",
                "IMUN 5070",
                "IMUN 6090",
                "CAMB 6090",
                "REG 6180"
        ));

        for (String course : req.options) {
            if (unwantedCourses.contains(course)) {
                return true;
            }
        }

        return false;
    }

    private static MajorRequirement buildBioengineeringElectivesRule(String majorName) {
        List<String> beElectives = getBioengineering4000PlusElectives();

        return new MajorRequirement(
                majorName,
                "2 BE Electives (400 or 500 level)",
                "CHOOSE_N",
                2,
                beElectives,
                "Choose 2 BE electives at the 4000-level or higher. BE 4950 and BE 4960 are excluded because they are required Senior Design courses."
        );
    }

    private static MajorRequirement buildBiomedicalScienceBeElectivesRule(String majorName) {
        List<String> beElectives = getBioengineering4000PlusElectives();

        return new MajorRequirement(
                majorName,
                "BE Electives (4000 or 5000 level)",
                "CHOOSE_N",
                2,
                beElectives,
                "Choose 2 BE electives at the 4000-level or higher. BE 4950 and BE 4960 are excluded because they are required Senior Design courses."
        );
    }

    private static List<String> getBioengineering4000PlusElectives() {
        Set<String> electives = new LinkedHashSet<>();

        for (RawCourse course : courseCatalog) {
            String courseId = course.getCourseId();

            String department = getDepartmentCode(courseId);
            int number = getCourseNumber(courseId);

            if (!department.equals("BE")) {
                continue;
            }

            if (number < 4000) {
                continue;
            }

            if (courseId.equals("BE 4950") || courseId.equals("BE 4960")) {
                continue;
            }

            electives.add(courseId);
        }

        return new ArrayList<>(electives);
    }

    private static MajorRequirement buildBioengineeringEngineeringElectivesRule(String majorName) {
        List<String> engineeringElectives = getEngineeringElectiveCourses();

        return new MajorRequirement(
                majorName,
                "2 Engineering Electives",
                "CHOOSE_N",
                2,
                engineeringElectives,
                "Choose 2 Engineering Electives. Includes courses from approved engineering subjects, excludes non-engineering subjects and explicitly excluded courses, and includes listed exceptions such as NSCI 3010."
        );
    }

    private static MajorRequirement buildBiomedicalScienceEngineeringElectivesRule(String majorName) {
        List<String> engineeringElectives = getEngineeringElectiveCourses();

        return new MajorRequirement(
                majorName,
                "Engineering Electives",
                "CHOOSE_N",
                2,
                engineeringElectives,
                "Choose 2 Engineering Electives. Includes courses from approved engineering subjects, excludes non-engineering subjects and explicitly excluded courses, and includes listed exceptions such as NSCI 3010."
        );
    }

    private static MajorRequirement buildBiomedicalScienceMathScienceEngineeringBusinessHealthRule(String majorName) {
        List<String> options = getMathScienceEngineeringBusinessHealthRelatedCourses();

        return new MajorRequirement(
                majorName,
                "Math, Science, Engineering, Business, or Health-Related Courses",
                "CHOOSE_N",
                3,
                options,
                "Select any 3 Math, Science, Engineering, Business, or health-related courses."
        );
    }

    private static MajorRequirement buildBiomedicalScienceEthicsRule(String majorName, Document doc) {
        List<String> ethicsCourses = scrapeBiomedicalScienceEthicsCourses(doc);

        return new MajorRequirement(
                majorName,
                "Ethics Requirement",
                "CHOOSE_ONE",
                1,
                ethicsCourses,
                "Choose one course that satisfies the Biomedical Science ethics requirement."
        );
    }

    private static List<String> scrapeBiomedicalScienceEthicsCourses(Document doc) {
        Set<String> ethicsCourses = new LinkedHashSet<>();

        String pageText = doc.text().replace('\u00A0', ' ');
        pageText = pageText.replaceAll("\\s+", " ").trim();

        String lower = pageText.toLowerCase();

        int start = lower.indexOf("ethics requirement options");

        if (start == -1) {
            start = lower.indexOf("ethics requirement");
        }

        if (start == -1) {
            return new ArrayList<>(ethicsCourses);
        }

        int end = pageText.length();

        String[] stopPhrases = {
                "footnotes",
                "notes",
                "professional electives",
                "general electives",
                "concentrations",
                "program requirements",
                "sample plan",
                "academic plan"
        };

        for (String stopPhrase : stopPhrases) {
            int stopIndex = lower.indexOf(stopPhrase, start + 1);

            if (stopIndex != -1 && stopIndex < end) {
                end = stopIndex;
            }
        }

        String ethicsText = pageText.substring(start, end);

        for (String course : extractCourseCodes(ethicsText)) {
            ethicsCourses.add(course);
        }

        return new ArrayList<>(ethicsCourses);
    }

    private static MajorRequirement buildBiomedicalScienceSocialScienceRule(String majorName) {
        List<String> socialScienceCourses = scrapeSocialScienceOnlyCoursesFromCatalog();

        return new MajorRequirement(
                majorName,
                "2 Social Science courses",
                "CHOOSE_N",
                2,
                socialScienceCourses,
                "Select 2 Social Science courses from the approved Social Sciences section of the SEAS Social Sciences and Humanities Breadth list."
        );
    }

    private static MajorRequirement buildBiomedicalScienceHumanitiesRule(String majorName) {
        List<String> humanitiesCourses = scrapeHumanitiesOnlyCoursesFromCatalog();

        return new MajorRequirement(
                majorName,
                "2 Humanities courses",
                "CHOOSE_N",
                2,
                humanitiesCourses,
                "Select 2 Humanities courses from the approved Humanities section of the SEAS Social Sciences and Humanities Breadth list."
        );
    }

    private static MajorRequirement buildBiomedicalScienceSshTbsRule(String majorName) {
        List<String> sshTbsCourses = getSocialScienceHumanitiesAndTechnologyBusinessSocietyCourses();

        return new MajorRequirement(
                majorName,
                "1 Social Science or Humanities or Technology in Business & Society course",
                "CHOOSE_ONE",
                1,
                sshTbsCourses,
                "Select 1 Social Science or Humanities or Technology in Business & Society course."
        );
    }

    private static MajorRequirement buildBiomedicalScienceFreeElectivesRule(String majorName) {
        List<String> freeElectiveCourses = getFreeElectiveCourses();

        return new MajorRequirement(
                majorName,
                "3 Free Elective courses",
                "CHOOSE_N",
                3,
                freeElectiveCourses,
                "Select 3 Free Elective courses."
        );
    }

    private static List<String> getMathScienceEngineeringBusinessHealthRelatedCourses() {
        Set<String> courses = new LinkedHashSet<>();

        for (String course : scrapeMathCoursesFromCatalog()) {
            courses.add(course);
        }

        for (String course : getNaturalScienceCourses()) {
            courses.add(course);
        }

        for (String course : getEngineeringElectiveCourses()) {
            courses.add(course);
        }

        for (String course : getBusinessCourses()) {
            courses.add(course);
        }

        return new ArrayList<>(courses);
    }

    private static List<String> scrapeMathCoursesFromCatalog() {
        String url = "https://academics.seas.upenn.edu/ugrad/student-handbook/courses-registrations/course-categories/mathematics-courses/";

        List<String> scrapedCourses = scrapeMathCoursesFromMathematicsPage(url);

        if (!scrapedCourses.isEmpty()) {
            return scrapedCourses;
        }

        return buildFallbackMathCoursesFromCatalog();
    }

    private static List<String> scrapeMathCoursesFromMathematicsPage(String url) {
        Set<String> mathCourses = new LinkedHashSet<>();

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .followRedirects(true)
                    .get();

            Elements elements = doc.select("li, p, td, th");

            for (Element element : elements) {
                String text = element.text().replace('\u00A0', ' ').trim();

                if (text.isEmpty()) {
                    continue;
                }

                String lower = text.toLowerCase();

                for (String course : extractSlashDepartmentCourses(text)) {
                    mathCourses.add(course);
                }

                for (String course : extractGroupedCourseCodes(text)) {
                    mathCourses.add(course);
                }

                if (lower.contains("all enm courses")
                        || lower.contains("all enm course")
                        || lower.contains("any enm courses")
                        || lower.contains("any enm course")) {
                    for (RawCourse course : courseCatalog) {
                        if (getDepartmentCode(course.getCourseId()).equals("ENM")) {
                            mathCourses.add(course.getCourseId());
                        }
                    }
                }

                if (lower.contains("math 1400")
                        || (lower.contains("math") && lower.contains("1400") && lower.contains("above"))) {
                    Set<Integer> excludedMathNumbers = extractExcludedMathNumbers(text);

                    if (excludedMathNumbers.isEmpty()) {
                        excludedMathNumbers.addAll(Arrays.asList(
                                1700,
                                1720,
                                1800,
                                2100,
                                2800
                        ));
                    }

                    for (RawCourse course : courseCatalog) {
                        String courseId = course.getCourseId();
                        String department = getDepartmentCode(courseId);
                        int number = getCourseNumber(courseId);

                        if (department.equals("MATH")
                                && number >= 1400
                                && !excludedMathNumbers.contains(number)) {
                            mathCourses.add(courseId);
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Could not scrape Mathematics Courses page.");
            System.out.println(e.getMessage());
        }

        return new ArrayList<>(mathCourses);
    }

    private static List<String> buildFallbackMathCoursesFromCatalog() {
        Set<String> mathCourses = new LinkedHashSet<>();

        Set<String> specificallyIncluded = new LinkedHashSet<>(Arrays.asList(
                "CIS 1600",
                "ENM 1600",
                "CIS 2610",
                "ENM 2610",
                "CIS 3333",
                "ESE 2030",
                "ESE 3010",
                "ENM 3010",
                "ESE 4020",
                "ESE 5300",
                "PHIL 1710",
                "PHIL 4723",
                "STAT 4300",
                "STAT 4310",
                "STAT 4320",
                "STAT 4330"
        ));

        Set<Integer> excludedMathNumbers = new HashSet<>(Arrays.asList(
                1700,
                1720,
                1800,
                2100,
                2800
        ));

        for (RawCourse course : courseCatalog) {
            String courseId = course.getCourseId();
            String department = getDepartmentCode(courseId);
            int number = getCourseNumber(courseId);

            if (specificallyIncluded.contains(courseId)) {
                mathCourses.add(courseId);
                continue;
            }

            if (department.equals("ENM")) {
                mathCourses.add(courseId);
                continue;
            }

            if (department.equals("MATH")
                    && number >= 1400
                    && !excludedMathNumbers.contains(number)) {
                mathCourses.add(courseId);
            }
        }

        return new ArrayList<>(mathCourses);
    }

    private static List<String> getNaturalScienceCourses() {
        String url = "https://academics.seas.upenn.edu/ugrad/student-handbook/courses-requirements/natural-science-courses/";

        List<String> courses = scrapeCourseCodesFromLinkedPage(url);

        if (!courses.isEmpty()) {
            return courses;
        }

        url = "https://academics.seas.upenn.edu/ugrad/student-handbook/courses-registrations/course-categories/natural-science-courses/";

        return scrapeCourseCodesFromLinkedPage(url);
    }

    private static List<String> getBusinessCourses() {
        Set<String> businessDepartments = new HashSet<>(Arrays.asList(
                "ACCT",
                "BEPP",
                "FNCE",
                "HCMG",
                "LGST",
                "MGMT",
                "MKTG",
                "OIDD",
                "REAL",
                "WH"
        ));

        Set<String> businessCourses = new LinkedHashSet<>();

        for (RawCourse course : courseCatalog) {
            String courseId = course.getCourseId();
            String department = getDepartmentCode(courseId);

            if (businessDepartments.contains(department)) {
                businessCourses.add(courseId);
            }
        }

        return new ArrayList<>(businessCourses);
    }

    private static List<String> getEngineeringElectiveCourses() {
        Set<String> electives = new LinkedHashSet<>();

        for (RawCourse course : courseCatalog) {
            String courseId = course.getCourseId();

            if (isEngineeringElectiveCourse(courseId)) {
                electives.add(courseId);
            }
        }

        return new ArrayList<>(electives);
    }

    private static boolean isEngineeringElectiveCourse(String courseId) {
        String department = getDepartmentCode(courseId);
        int number = getCourseNumber(courseId);

        if (ENGINEERING_INCLUDED_EXCEPTIONS.contains(courseId)) {
            return true;
        }

        if (NON_ENGINEERING_SUBJECTS.contains(department)) {
            return false;
        }

        if (!ENGINEERING_SUBJECTS.contains(department)) {
            return false;
        }

        if (ENGINEERING_EXCLUDED_COURSES.contains(courseId)) {
            return false;
        }

        if (number == 2960 || number == 2970) {
            return false;
        }

        return true;
    }

    private static MajorRequirement buildBioengineeringSocialScienceRule(String majorName) {
        List<String> socialScienceCourses = scrapeSocialScienceOnlyCoursesFromCatalog();

        return new MajorRequirement(
                majorName,
                "2 Social Science courses",
                "CHOOSE_N",
                2,
                socialScienceCourses,
                "Select 2 Social Science courses from the approved Social Sciences section of the SEAS Social Sciences and Humanities Breadth list."
        );
    }

    private static MajorRequirement buildBioengineeringHumanitiesRule(String majorName) {
        List<String> humanitiesCourses = scrapeHumanitiesOnlyCoursesFromCatalog();

        return new MajorRequirement(
                majorName,
                "2 Humanities courses",
                "CHOOSE_N",
                2,
                humanitiesCourses,
                "Select 2 Humanities courses from the approved Humanities section of the SEAS Social Sciences and Humanities Breadth list."
        );
    }

    private static MajorRequirement buildBioengineeringSshTbsRule(String majorName) {
        List<String> sshTbsCourses = getSocialScienceHumanitiesAndTechnologyBusinessSocietyCourses();

        return new MajorRequirement(
                majorName,
                "1 Social Science, Humanities or Technology in Business & Society course",
                "CHOOSE_ONE",
                1,
                sshTbsCourses,
                "Select 1 Social Science, Humanities or Technology in Business & Society course."
        );
    }

    private static MajorRequirement buildBioengineeringFreeElectivesRule(String majorName) {
        List<String> freeElectiveCourses = getFreeElectiveCourses();

        return new MajorRequirement(
                majorName,
                "3 Free Elective courses",
                "CHOOSE_N",
                3,
                freeElectiveCourses,
                "Select 3 Free Elective courses."
        );
    }

    private static boolean isArtificialIntelligenceMajor(String majorName) {
        return majorName.toLowerCase().contains("artificial intelligence")
                && majorName.toLowerCase().contains("bse");
    }

    private static List<MajorRequirement> removeRedundantArtificialIntelligenceNoteRules(
            List<MajorRequirement> requirements) {

        List<MajorRequirement> cleaned = new ArrayList<>();

        for (MajorRequirement req : requirements) {
            String note = "";

            if (req.note != null) {
                note = req.note.trim().toLowerCase();
            }

            if (req.majorName.toLowerCase().contains("artificial intelligence")
                    && req.ruleType.equals("NOTE_OR_CATEGORY_RULE")
                    && req.options.isEmpty()
                    && note.startsWith("select three total course units from the following")) {
                continue;
            }

            cleaned.add(req);
        }

        return cleaned;
    }

    private static List<MajorRequirement> removeGenericArtificialIntelligenceRules(
            List<MajorRequirement> requirements) {

        List<MajorRequirement> cleaned = new ArrayList<>();

        Set<String> aiSections = new HashSet<>(Arrays.asList(
                "Artificial Intelligence",
                "Introduction to AI",
                "Machine Learning",
                "Signals & Systems",
                "Optimization & Control",
                "Vision & Language",
                "AI Project",
                "AI Electives",
                "Machine Learning Electives",
                "Optimization, Systems, and Control Electives",
                "Other AI Electives"
        ));

        for (MajorRequirement req : requirements) {
            if (!aiSections.contains(req.sectionName)) {
                cleaned.add(req);
            }
        }

        return cleaned;
    }

    private static List<MajorRequirement> buildArtificialIntelligenceRules(String majorName) {
        List<MajorRequirement> rules = new ArrayList<>();

        rules.add(new MajorRequirement(
                majorName,
                "Artificial Intelligence",
                "AI_TOTAL_REQUIREMENT",
                12,
                new ArrayList<>(),
                "12 course units total. Must include one course from each of the six AI categories, then 6 additional AI elective course units. One course cannot satisfy multiple categories."
        ));

        List<List<String>> introToAI = groups(
                group("CIS 4210", "CIS 5210"),
                group("ESE 2000")
        );

        List<List<String>> machineLearning = groups(
                group("CIS 4190", "CIS 5190"),
                group("CIS 5200")
        );

        List<List<String>> signalsSystems = groups(
                group("ESE 2100"),
                group("ESE 2240")
        );

        List<List<String>> optimizationControl = groups(
                group("ESE 3040"),
                group("ESE 4210")
        );

        List<List<String>> visionLanguage = groups(
                group("CIS 4300", "CIS 5300"),
                group("CIS 4810", "CIS 5810")
        );

        List<List<String>> aiProject = groups(
                group("CIS 3500"),
                group("CIS 4300", "CIS 5300"),
                group("CIS 4810", "CIS 5810"),
                group("ESE 3060"),
                group("ESE 3600"),
                group("ESE 4210"),
                group("NETS 2120"),
                group("NETS 2130")
        );

        rules.add(new MajorRequirement(
                majorName,
                "Introduction to AI",
                "CHOOSE_ONE_FROM_GROUPS",
                1,
                introToAI,
                "Choose one course from Introduction to AI. Courses in the same option group are alternatives and should not both be selected.",
                true
        ));

        rules.add(new MajorRequirement(
                majorName,
                "Machine Learning",
                "CHOOSE_ONE_FROM_GROUPS",
                1,
                machineLearning,
                "Choose one course from Machine Learning. Courses in the same option group are alternatives and should not both be selected.",
                true
        ));

        rules.add(new MajorRequirement(
                majorName,
                "Signals & Systems",
                "CHOOSE_ONE_FROM_GROUPS",
                1,
                signalsSystems,
                "Choose one course from Signals & Systems.",
                true
        ));

        rules.add(new MajorRequirement(
                majorName,
                "Optimization & Control",
                "CHOOSE_ONE_FROM_GROUPS",
                1,
                optimizationControl,
                "Choose one course from Optimization & Control.",
                true
        ));

        rules.add(new MajorRequirement(
                majorName,
                "Vision & Language",
                "CHOOSE_ONE_FROM_GROUPS",
                1,
                visionLanguage,
                "Choose one course from Vision & Language. Courses in the same option group are alternatives and should not both be selected.",
                true
        ));

        rules.add(new MajorRequirement(
                majorName,
                "AI Project",
                "CHOOSE_ONE_FROM_GROUPS",
                1,
                aiProject,
                "Choose one course from AI Project. Courses in the same option group are alternatives and should not both be selected.",
                true
        ));

        List<List<String>> allAIOptions = new ArrayList<>();

        allAIOptions.addAll(introToAI);
        allAIOptions.addAll(machineLearning);
        allAIOptions.addAll(signalsSystems);
        allAIOptions.addAll(optimizationControl);
        allAIOptions.addAll(visionLanguage);
        allAIOptions.addAll(aiProject);

        allAIOptions.addAll(groups(
                group("CIS 3333"),
                group("CIS 6200"),
                group("CIS 6250"),
                group("ESE 4380", "ESE 5380"),
                group("ESE 5140"),
                group("ESE 5460"),
                group("ESE 6450"),
                group("ESE 6740"),
                group("CIS 4270", "CIS 5270"),

                group("ESE 3030"),
                group("ESE 5000"),
                group("ESE 5050"),
                group("ESE 5060"),
                group("ESE 6050"),
                group("ESE 6060"),
                group("ESE 6180"),
                group("ESE 6190"),

                group("BE 5210"),
                group("CIS 4120", "CIS 5120"),
                group("CIS 4500", "CIS 5500"),
                group("CIS 5360"),
                group("CIS 5800"),
                group("CIS 6500"),
                group("MEAM 5200"),
                group("MEAM 6200"),
                group("ESE 4040"),
                group("ESE 6150"),
                group("ESE 6500"),
                group("ESE 6510"),
                group("NETS 3120"),
                group("NETS 4120")
        ));

        rules.add(new MajorRequirement(
                majorName,
                "AI Electives",
                "CHOOSE_N_FROM_GROUPS",
                6,
                allAIOptions,
                "After choosing one course from each of the six AI categories, choose 6 additional AI course units from any course in those categories or from the AI elective lists. Do not double count the same course. Courses in the same option group are alternatives and should not both be selected.",
                true
        ));

        return rules;
    }

    private static List<String> group(String... courses) {
        return new ArrayList<>(Arrays.asList(courses));
    }

    @SafeVarargs
    private static List<List<String>> groups(List<String>... groups) {
        return new ArrayList<>(Arrays.asList(groups));
    }

    private static List<MajorRequirement> parseRequirementTable(String majorName, Element table) {
        List<MajorRequirement> requirements = new ArrayList<>();

        String currentSection = "General Requirements";

        Set<String> cognitiveScienceCoursesToSkip = new HashSet<>();
        boolean skippingCognitiveScienceRows = false;

        Elements rows = table.select("tr");

        for (Element row : rows) {
            String rowText = row.text().trim();

            if (rowText.isEmpty()) {
                continue;
            }

            if (isSectionHeader(row)) {
                currentSection = cleanSectionName(rowText);
                skippingCognitiveScienceRows = false;
                continue;
            }

            if (skippingCognitiveScienceRows) {
                List<String> rowCourses = extractCourseCodes(rowText);

                if (!rowCourses.isEmpty()
                        && cognitiveScienceCoursesToSkip.containsAll(rowCourses)) {
                    continue;
                } else {
                    skippingCognitiveScienceRows = false;
                }
            }

            if (isNaturalScienceElectiveRow(rowText)) {
                Element link = row.selectFirst("a[href]");

                if (link != null) {
                    String electiveListUrl = link.absUrl("href");
                    List<String> naturalScienceCourses = scrapeCourseCodesFromLinkedPage(electiveListUrl);

                    if (!naturalScienceCourses.isEmpty()) {
                        requirements.add(new MajorRequirement(
                                majorName,
                                currentSection,
                                "CHOOSE_ONE",
                                1,
                                naturalScienceCourses,
                                "Natural Science Electives List"
                        ));
                    }
                }

                continue;
            }

            if (isSeasEngineeringElectiveRow(rowText)) {
                Element link = row.selectFirst("a[href]");

                if (link != null) {
                    String electiveListUrl = link.absUrl("href");
                    List<String> seasEngineeringCourses = scrapeCourseCodesFromLinkedPage(electiveListUrl);

                    if (!seasEngineeringCourses.isEmpty()) {
                        requirements.add(new MajorRequirement(
                                majorName,
                                currentSection,
                                "CHOOSE_N",
                                3,
                                seasEngineeringCourses,
                                "SEAS Engineering (EUNG) Technical Electives List"
                        ));
                    }
                }

                continue;
            }

            if (isCognitiveScienceElectiveRow(rowText)) {
                List<String> cognitiveScienceCourses =
                        scrapeCognitiveScienceElectiveCourses(row, table);

                if (!cognitiveScienceCourses.isEmpty()) {
                    requirements.add(new MajorRequirement(
                            majorName,
                            currentSection,
                            "CHOOSE_ONE",
                            1,
                            cognitiveScienceCourses,
                            "Cognitive Science Elective List"
                    ));

                    cognitiveScienceCoursesToSkip.clear();
                    cognitiveScienceCoursesToSkip.addAll(cognitiveScienceCourses);
                    skippingCognitiveScienceRows = true;
                }

                continue;
            }

            if (isSocialScienceHumanitiesOrTechnologyBusinessSocietyRow(rowText)) {
                List<String> options = getSocialScienceHumanitiesAndTechnologyBusinessSocietyCourses();

                if (!options.isEmpty()) {
                    int coursesNeeded = extractFirstNumber(rowText);

                    if (coursesNeeded <= 0) {
                        coursesNeeded = 2;
                    }

                    requirements.add(new MajorRequirement(
                            majorName,
                            currentSection,
                            "CHOOSE_N",
                            coursesNeeded,
                            options,
                            "Select " + coursesNeeded + " Social Science or Humanities or Technology in Business & Society courses"
                    ));
                }

                continue;
            }

            if (isSocialScienceHumanitiesRow(rowText)) {
                List<String> writingCourses = scrapeWritingCourses();

                if (!writingCourses.isEmpty()) {
                    requirements.add(new MajorRequirement(
                            majorName,
                            currentSection,
                            "CHOOSE_ONE",
                            1,
                            writingCourses,
                            "Writing Requirement"
                    ));
                }

                List<String> sshCourses = scrapeSocialScienceHumanitiesCoursesFromCatalog();

                if (!sshCourses.isEmpty()) {
                    int sshCoursesNeeded = extractFirstNumber(rowText);

                    if (sshCoursesNeeded <= 0) {
                        sshCoursesNeeded = 2;
                    }

                    requirements.add(new MajorRequirement(
                            majorName,
                            currentSection,
                            "CHOOSE_N",
                            sshCoursesNeeded,
                            sshCourses,
                            "Select " + sshCoursesNeeded + " Social Science or Humanities courses"
                    ));
                }

                continue;
            }

            if (isFreeElectiveRow(rowText)) {
                List<String> freeElectiveCourses = getFreeElectiveCourses();

                if (!freeElectiveCourses.isEmpty()) {
                    requirements.add(new MajorRequirement(
                            majorName,
                            currentSection,
                            "CHOOSE_ONE",
                            1,
                            freeElectiveCourses,
                            "Select 1 course unit of Free Elective"
                    ));
                }

                continue;
            }

            if (!containsCourseCode(rowText)) {
                if (isImportantRuleText(rowText)) {
                    requirements.add(new MajorRequirement(
                            majorName,
                            currentSection,
                            "NOTE_OR_CATEGORY_RULE",
                            extractFirstNumber(rowText),
                            new ArrayList<>(),
                            rowText
                    ));
                }

                continue;
            }

            List<String> courseCodes = extractCourseCodes(rowText);

            if (courseCodes.isEmpty()) {
                continue;
            }

            String ruleType = "REQUIRED_ALL";
            int coursesNeeded = courseCodes.size();

            if (isOrRow(rowText, row)) {
                ruleType = "CHOOSE_ONE";
                coursesNeeded = 1;
            }

            if (isChemicalAndBiomolecularEngineeringMajor(majorName)
                    && isAdvancedChemistryElectiveRow(rowText, courseCodes)) {
                ruleType = "CHOOSE_ONE";
                coursesNeeded = 1;
            }

            requirements.add(new MajorRequirement(
                    majorName,
                    currentSection,
                    ruleType,
                    coursesNeeded,
                    courseCodes,
                    rowText
            ));
        }

        return mergeOrRows(requirements);
    }

    private static boolean isNaturalScienceElectiveRow(String rowText) {
        String lower = rowText.toLowerCase();

        return lower.contains("natural science elective")
                || lower.contains("natural science electives");
    }

    private static boolean isSeasEngineeringElectiveRow(String rowText) {
        String lower = rowText.toLowerCase();

        return lower.contains("seas engineering")
                || lower.contains("eung");
    }

    private static boolean isCognitiveScienceElectiveRow(String rowText) {
        String lower = rowText.toLowerCase();

        return lower.contains("cognitive science elective")
                || lower.contains("cognitive science electives");
    }

    private static List<String> scrapeCognitiveScienceElectiveCourses(Element cognitiveRow, Element table) {
        Set<String> courseCodes = new LinkedHashSet<>();

        Element link = cognitiveRow.selectFirst("a[href]");

        if (link != null) {
            String url = link.absUrl("href");

            if (!url.isEmpty()) {
                courseCodes.addAll(scrapeCourseCodesFromLinkedPage(url));
            }
        }

        if (courseCodes.isEmpty()) {
            Elements rows = table.select("tr");

            boolean foundCognitiveRow = false;

            for (Element row : rows) {
                String rowText = row.text().trim();

                if (rowText.isEmpty()) {
                    continue;
                }

                if (!foundCognitiveRow) {
                    if (row == cognitiveRow || rowText.equals(cognitiveRow.text().trim())) {
                        foundCognitiveRow = true;
                    }

                    continue;
                }

                if (isSectionHeader(row)) {
                    break;
                }

                if (isImportantRuleText(rowText) && !containsCourseCode(rowText)) {
                    break;
                }

                List<String> rowCourses = extractCourseCodes(rowText);

                for (String course : rowCourses) {
                    courseCodes.add(course);
                }
            }
        }

        return new ArrayList<>(courseCodes);
    }

    private static boolean isSocialScienceHumanitiesOrTechnologyBusinessSocietyRow(String rowText) {
        String lower = rowText.toLowerCase();

        boolean mentionsSSH =
                lower.contains("social science or humanities")
                        || lower.contains("social sciences or humanities")
                        || lower.contains("social science and humanities")
                        || lower.contains("social sciences and humanities");

        boolean mentionsTBS =
                lower.contains("technology in business & society")
                        || lower.contains("technology in business and society")
                        || lower.contains("technology in business");

        return mentionsSSH && mentionsTBS;
    }

    private static boolean isSocialScienceHumanitiesRow(String rowText) {
        String lower = rowText.toLowerCase();

        boolean mentionsSSH =
                lower.contains("social science or humanities")
                        || lower.contains("social sciences or humanities")
                        || lower.contains("social science and humanities")
                        || lower.contains("social sciences and humanities");

        boolean mentionsTBS =
                lower.contains("technology in business & society")
                        || lower.contains("technology in business and society")
                        || lower.contains("technology in business");

        return mentionsSSH && !mentionsTBS;
    }

    private static boolean isFreeElectiveRow(String rowText) {
        String lower = rowText.toLowerCase();

        return lower.contains("free elective")
                || lower.contains("free electives");
    }

    private static List<String> getFreeElectiveCourses() {
        List<String> result = new ArrayList<>();

        for (RawCourse course : courseCatalog) {
            String courseId = course.getCourseId();

            if (!isExcludedFreeElectiveCourse(courseId)) {
                result.add(courseId);
            }
        }

        return result;
    }

    private static boolean isExcludedFreeElectiveCourse(String courseId) {
        String subject = getDepartmentCode(courseId);

        if (subject.equals("MED")) {
            return true;
        }

        if (subject.equals("TGP")) {
            return true;
        }

        if (subject.equals("CIT")) {
            return true;
        }

        if (subject.equals("DYNM")) {
            return true;
        }

        if (subject.equals("MSCI")) {
            return true;
        }

        Set<String> excludedCourses = buildFreeElectiveExclusionSet();

        return excludedCourses.contains(courseId);
    }

    private static Set<String> buildFreeElectiveExclusionSet() {
        Set<String> excluded = new HashSet<>();

        excluded.add("ASTR 0001");
        excluded.add("CHEM 1011");

        excluded.add("CIS 0100");
        excluded.add("CIS 1000");
        excluded.add("CIS 0101");
        excluded.add("CIS 1010");
        excluded.add("CSE 1000");
        excluded.add("CSE 0100");
        excluded.add("CSE 1010");
        excluded.add("CSE 0101");

        excluded.add("CIS 1050");
        excluded.add("CIS 1100");

        excluded.add("EAS 5030");
        excluded.add("EAS 4030");
        excluded.add("EAS 5050");
        excluded.add("EAS 3010");

        excluded.add("MATH 1510");
        excluded.add("MATH 1700");

        excluded.add("MEAM 0091");
        excluded.add("MEAM 0092");
        excluded.add("MEAM 0093");
        excluded.add("MEAM 0094");
        excluded.add("MEAM 0095");

        excluded.add("PHYS 0008");
        excluded.add("PHYS 0009");
        excluded.add("PHYS 0016");
        excluded.add("PHYS 0080");
        excluded.add("PHYS 0090");
        excluded.add("PHYS 0101");
        excluded.add("PHYS 0102");
        excluded.add("PHYS 0137");
        excluded.add("PHYS 1100");

        excluded.add("STAT 0001");
        excluded.add("STAT 0002");
        excluded.add("STAT 1010");
        excluded.add("STAT 1018");
        excluded.add("STAT 1020");
        excluded.add("STAT 1028");
        excluded.add("STAT 1110");
        excluded.add("STAT 1120");
        excluded.add("STAT 3990");
        excluded.add("STAT 4010");
        excluded.add("STAT 4020");
        excluded.add("STAT 4050");
        excluded.add("STAT 4100");
        excluded.add("STAT 4220");

        excluded.add("NSCI 1000");
        excluded.add("NSCI 1010");
        excluded.add("NSCI 3020");
        excluded.add("NSCI 3100");
        excluded.add("NSCI 4100");

        excluded.add("WH 0010");
        excluded.add("WH 1010");
        excluded.add("WH 1500");
        excluded.add("WH 1508");
        excluded.add("WH 2010");
        excluded.add("WH 2011");
        excluded.add("WH 2050");
        excluded.add("WH 2130");
        excluded.add("WH 2160");
        excluded.add("WH 2970");
        excluded.add("WH 2980");
        excluded.add("WH 2981");
        excluded.add("WH 2990");
        excluded.add("WH 2991");
        excluded.add("WH 3990");
        excluded.add("WH 3991");

        return excluded;
    }

    private static List<String> scrapeCourseCodesFromLinkedPage(String url) {
        Set<String> courseCodes = new LinkedHashSet<>();

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .followRedirects(true)
                    .get();

            Elements tables = doc.select("table.sc_courselist, table");

            if (!tables.isEmpty()) {
                for (Element table : tables) {
                    courseCodes.addAll(extractCourseCodes(table.text()));
                }
            } else {
                courseCodes.addAll(extractCourseCodes(doc.text()));
            }

        } catch (Exception e) {
            System.out.println("Could not scrape linked elective list: " + url);
            System.out.println(e.getMessage());
        }

        return new ArrayList<>(courseCodes);
    }

    private static List<String> scrapeWritingCourses() {
        String url = "https://academics.seas.upenn.edu/ugrad/student-handbook/courses-requirements/writing-courses/";

        Set<String> courseCodes = new LinkedHashSet<>();

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .followRedirects(true)
                    .get();

            Elements tables = doc.select("table");

            for (Element table : tables) {
                courseCodes.addAll(extractCourseCodes(table.text()));
            }

            if (courseCodes.isEmpty()) {
                courseCodes.addAll(extractCourseCodes(doc.text()));
            }

        } catch (Exception e) {
            System.out.println("Could not scrape Writing Requirement page.");
            System.out.println(e.getMessage());
        }

        return new ArrayList<>(courseCodes);
    }

    private static List<String> getSocialScienceHumanitiesAndTechnologyBusinessSocietyCourses() {
        Set<String> courses = new LinkedHashSet<>();

        List<String> sshCourses = scrapeSocialScienceHumanitiesCoursesFromCatalog();

        for (String course : sshCourses) {
            courses.add(course);
        }

        List<String> tbsCourses = scrapeTechnologyBusinessSocietyCourses();

        for (String course : tbsCourses) {
            courses.add(course);
        }

        return new ArrayList<>(courses);
    }

    private static List<String> scrapeTechnologyBusinessSocietyCourses() {
        List<String> urls = Arrays.asList(
                "https://ugrad.seas.upenn.edu/student-handbook/courses-requirements/technology-in-business-and-society-courses/",
                "https://academics.seas.upenn.edu/ugrad/student-handbook/courses-requirements/technology-in-business-and-society-courses/"
        );

        Set<String> allCourseCodes = new LinkedHashSet<>();

        for (String url : urls) {
            Set<String> coursesFromThisUrl = scrapeTechnologyBusinessSocietyCoursesFromUrl(url);

            if (!coursesFromThisUrl.isEmpty()) {
                allCourseCodes.addAll(coursesFromThisUrl);
                break;
            }
        }

        Set<String> filteredCourses = keepOnlyCoursesFoundInCatalog(allCourseCodes);

        if (!filteredCourses.isEmpty()) {
            return new ArrayList<>(filteredCourses);
        }

        return new ArrayList<>(allCourseCodes);
    }

    private static Set<String> scrapeTechnologyBusinessSocietyCoursesFromUrl(String url) {
        Set<String> courseCodes = new LinkedHashSet<>();

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .followRedirects(true)
                    .get();

            Elements courseListElements = doc.select(
                    "table, div.wp-block-table, li, p, td, th"
            );

            for (Element element : courseListElements) {
                String text = element.text().trim();

                if (text.isEmpty()) {
                    continue;
                }

                courseCodes.addAll(extractGroupedCourseCodes(text));
            }

            if (courseCodes.isEmpty()) {
                courseCodes.addAll(extractGroupedCourseCodes(doc.text()));
            }

        } catch (Exception e) {
            System.out.println("Could not scrape Technology in Business & Society page: " + url);
            System.out.println(e.getMessage());
        }

        return courseCodes;
    }

    private static Set<String> keepOnlyCoursesFoundInCatalog(Set<String> courseIds) {
        Set<String> catalogIds = new HashSet<>();

        for (RawCourse course : courseCatalog) {
            catalogIds.add(course.getCourseId());
        }

        Set<String> filtered = new LinkedHashSet<>();

        for (String courseId : courseIds) {
            if (catalogIds.contains(courseId)) {
                filtered.add(courseId);
            }
        }

        return filtered;
    }

    private static List<String> extractGroupedCourseCodes(String text) {
        List<String> result = new ArrayList<>();

        text = text.replace('\u00A0', ' ');
        text = text.replaceAll("\\(([A-Z]{2,5})\\)", "");
        text = text.replaceAll("\\s+", " ").trim();

        for (String course : extractCourseCodes(text)) {
            if (!result.contains(course)) {
                result.add(course);
            }
        }

        Pattern groupedPattern = Pattern.compile(
                "\\b([A-Z]{2,5})\\s*:?\\s*((?:\\d{4}(?:\\s*\\([^)]*\\))?\\*?\\s*(?:,|;|/|\\bor\\b|\\band\\b)?\\s*)+)"
        );

        Matcher groupedMatcher = groupedPattern.matcher(text);

        while (groupedMatcher.find()) {
            String department = groupedMatcher.group(1);
            String numbersText = groupedMatcher.group(2);

            if (!isValidDepartmentCode(department)) {
                continue;
            }

            Pattern numberPattern = Pattern.compile("\\b(\\d{4})\\b");
            Matcher numberMatcher = numberPattern.matcher(numbersText);

            while (numberMatcher.find()) {
                String courseId = department + " " + numberMatcher.group(1);

                if (!result.contains(courseId)) {
                    result.add(courseId);
                }
            }
        }

        return result;
    }

    private static List<String> extractSlashDepartmentCourses(String text) {
        List<String> courses = new ArrayList<>();

        text = text.replace('\u00A0', ' ');
        text = text.replaceAll("\\s+", " ").trim();

        Pattern pattern = Pattern.compile("\\b([A-Z]{2,5}(?:/[A-Z]{2,5})+)\\s+(\\d{4})\\b");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String departmentsText = matcher.group(1);
            String number = matcher.group(2);

            String[] departments = departmentsText.split("/");

            for (String department : departments) {
                department = department.trim();

                if (isValidDepartmentCode(department)) {
                    String courseId = department + " " + number;

                    if (!courses.contains(courseId)) {
                        courses.add(courseId);
                    }
                }
            }
        }

        return courses;
    }

    private static Set<Integer> extractExcludedMathNumbers(String text) {
        Set<Integer> excludedNumbers = new LinkedHashSet<>();

        String lower = text.toLowerCase();
        int exceptIndex = lower.indexOf("except");

        if (exceptIndex == -1) {
            return excludedNumbers;
        }

        String excludedPart = text.substring(exceptIndex);

        Pattern numberPattern = Pattern.compile("\\b(\\d{4})\\b");
        Matcher numberMatcher = numberPattern.matcher(excludedPart);

        while (numberMatcher.find()) {
            excludedNumbers.add(Integer.parseInt(numberMatcher.group(1)));
        }

        return excludedNumbers;
    }

    private static List<String> scrapeSocialScienceHumanitiesCoursesFromCatalog() {
        String url = "https://academics.seas.upenn.edu/ugrad/student-handbook/courses-requirements/social-sciences-and-humanities-breadth/";

        Set<String> departmentCodes = new LinkedHashSet<>();
        Set<String> specificCourses = new LinkedHashSet<>();
        Set<String> excludedCourses = new LinkedHashSet<>();

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .followRedirects(true)
                    .get();

            Elements listItems = doc.select("li");

            for (Element item : listItems) {
                String text = item.text().trim();

                if (text.isEmpty()) {
                    continue;
                }

                collectDepartmentCodes(text, departmentCodes);
                collectSpecificCourses(text, specificCourses);
                collectExcludedCourses(text, excludedCourses);
            }

        } catch (Exception e) {
            System.out.println("Could not scrape Social Science/Humanities page.");
            System.out.println(e.getMessage());
        }

        return buildCourseListFromDepartmentsAndSpecificCourses(
                departmentCodes,
                specificCourses,
                excludedCourses
        );
    }

    private static List<String> scrapeSocialScienceOnlyCoursesFromCatalog() {
        String url = "https://academics.seas.upenn.edu/ugrad/student-handbook/courses-registrations/course-categories/social-sciences-and-humanities-breadth/";

        Set<String> departmentCodes = new LinkedHashSet<>();
        Set<String> specificCourses = new LinkedHashSet<>();
        Set<String> excludedCourses = new LinkedHashSet<>();

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .followRedirects(true)
                    .get();

            Elements elements = doc.select("h2, h3, h4, li, p");

            boolean inSocialScienceSection = false;

            for (Element element : elements) {
                String text = element.text().trim();

                if (text.isEmpty()) {
                    continue;
                }

                String lower = text.toLowerCase();

                if (lower.equals("social sciences")) {
                    inSocialScienceSection = true;
                    continue;
                }

                if (inSocialScienceSection && lower.equals("humanities")) {
                    break;
                }

                if (!inSocialScienceSection) {
                    continue;
                }

                collectDepartmentCodes(text, departmentCodes);
                collectSpecificCourses(text, specificCourses);
                collectExcludedCourses(text, excludedCourses);
            }

        } catch (Exception e) {
            System.out.println("Could not scrape Social Science-only page.");
            System.out.println(e.getMessage());
        }

        return buildCourseListFromDepartmentsAndSpecificCourses(
                departmentCodes,
                specificCourses,
                excludedCourses
        );
    }

    private static List<String> scrapeHumanitiesOnlyCoursesFromCatalog() {
        String url = "https://academics.seas.upenn.edu/ugrad/student-handbook/courses-registrations/course-categories/social-sciences-and-humanities-breadth/";

        Set<String> departmentCodes = new LinkedHashSet<>();
        Set<String> specificCourses = new LinkedHashSet<>();
        Set<String> excludedCourses = new LinkedHashSet<>();

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .followRedirects(true)
                    .get();

            Elements elements = doc.select("h2, h3, h4, li, p");

            boolean inHumanitiesSection = false;

            for (Element element : elements) {
                String text = element.text().trim();

                if (text.isEmpty()) {
                    continue;
                }

                String lower = text.toLowerCase();

                if (lower.equals("humanities")) {
                    inHumanitiesSection = true;
                    continue;
                }

                if (inHumanitiesSection
                        && (lower.equals("notes")
                        || lower.equals("important notes")
                        || lower.equals("writing courses")
                        || lower.equals("technology in business & society")
                        || lower.equals("technology in business and society"))) {
                    break;
                }

                if (!inHumanitiesSection) {
                    continue;
                }

                collectDepartmentCodes(text, departmentCodes);
                collectSpecificCourses(text, specificCourses);
                collectExcludedCourses(text, excludedCourses);
            }

        } catch (Exception e) {
            System.out.println("Could not scrape Humanities-only page.");
            System.out.println(e.getMessage());
        }

        return buildCourseListFromDepartmentsAndSpecificCourses(
                departmentCodes,
                specificCourses,
                excludedCourses
        );
    }

    private static List<String> buildCourseListFromDepartmentsAndSpecificCourses(
            Set<String> departmentCodes,
            Set<String> specificCourses,
            Set<String> excludedCourses) {

        Set<String> result = new LinkedHashSet<>();

        for (RawCourse course : courseCatalog) {
            String courseId = course.getCourseId();

            String department = getDepartmentCode(courseId);
            int number = getCourseNumber(courseId);

            if (departmentCodes.contains(department)
                    && number > 0
                    && number < 6000
                    && !excludedCourses.contains(courseId)) {
                result.add(courseId);
            }
        }

        for (String courseId : specificCourses) {
            if (!excludedCourses.contains(courseId)) {
                result.add(courseId);
            }
        }

        return new ArrayList<>(result);
    }

    private static void collectDepartmentCodes(String text, Set<String> departmentCodes) {
        boolean hasCourseNumber = text.matches(".*\\b\\d{4}\\b.*");
        boolean hasExcept = text.toLowerCase().contains("except");

        if (hasCourseNumber && !hasExcept) {
            return;
        }

        Pattern dashPattern = Pattern.compile("\\b([A-Z]{2,5})\\b\\s*[–-]");
        Matcher dashMatcher = dashPattern.matcher(text);

        while (dashMatcher.find()) {
            String code = dashMatcher.group(1);

            if (isValidDepartmentCode(code)) {
                departmentCodes.add(code);
            }
        }

        Pattern parenthesesPattern = Pattern.compile("\\(([A-Z]{2,5}(?:,\\s*[A-Z]{2,5})*)\\)");
        Matcher parenthesesMatcher = parenthesesPattern.matcher(text);

        while (parenthesesMatcher.find()) {
            String inside = parenthesesMatcher.group(1);
            String[] codes = inside.split(",");

            for (String code : codes) {
                code = code.trim();

                if (isValidDepartmentCode(code)) {
                    departmentCodes.add(code);
                }
            }
        }
    }

    private static void collectSpecificCourses(String text, Set<String> specificCourses) {
        boolean hasCourseNumber = text.matches(".*\\b\\d{4}\\b.*");

        if (!hasCourseNumber) {
            return;
        }

        if (text.toLowerCase().contains("except")) {
            return;
        }

        String department = findFirstDepartmentCode(text);

        if (department == null) {
            return;
        }

        Pattern numberPattern = Pattern.compile("\\b(\\d{4})\\b");
        Matcher numberMatcher = numberPattern.matcher(text);

        while (numberMatcher.find()) {
            String number = numberMatcher.group(1);
            specificCourses.add(department + " " + number);
        }
    }

    private static void collectExcludedCourses(String text, Set<String> excludedCourses) {
        String lower = text.toLowerCase();

        if (!lower.contains("except")) {
            return;
        }

        String department = findFirstDepartmentCode(text);

        if (department == null) {
            return;
        }

        Pattern numberPattern = Pattern.compile("\\b(\\d{4})\\b");
        Matcher numberMatcher = numberPattern.matcher(text);

        while (numberMatcher.find()) {
            String number = numberMatcher.group(1);
            excludedCourses.add(department + " " + number);
        }
    }

    private static String findFirstDepartmentCode(String text) {
        Pattern dashPattern = Pattern.compile("\\b([A-Z]{2,5})\\b\\s*[–-]");
        Matcher dashMatcher = dashPattern.matcher(text);

        if (dashMatcher.find()) {
            String code = dashMatcher.group(1);

            if (isValidDepartmentCode(code)) {
                return code;
            }
        }

        Pattern parenthesesPattern = Pattern.compile("\\(([A-Z]{2,5})\\)");
        Matcher parenthesesMatcher = parenthesesPattern.matcher(text);

        if (parenthesesMatcher.find()) {
            String code = parenthesesMatcher.group(1);

            if (isValidDepartmentCode(code)) {
                return code;
            }
        }

        Pattern codePattern = Pattern.compile("\\b([A-Z]{2,5})\\b");
        Matcher codeMatcher = codePattern.matcher(text);

        if (codeMatcher.find()) {
            String code = codeMatcher.group(1);

            if (isValidDepartmentCode(code)) {
                return code;
            }
        }

        return null;
    }

    private static boolean isValidDepartmentCode(String code) {
        Set<String> invalidCodes = new HashSet<>(Arrays.asList(
                "SS", "H", "CU", "CUS", "NOTE", "AND", "THE", "FOR", "ALL"
        ));

        return code.length() >= 2
                && code.length() <= 5
                && !invalidCodes.contains(code);
    }

    private static String getDepartmentCode(String courseId) {
        Pattern pattern = Pattern.compile("^([A-Z]{2,5})\\s+\\d{4}$");
        Matcher matcher = pattern.matcher(courseId);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    private static int getCourseNumber(String courseId) {
        Pattern pattern = Pattern.compile("^[A-Z]{2,5}\\s+(\\d{4})$");
        Matcher matcher = pattern.matcher(courseId);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return -1;
    }

    private static boolean isSectionHeader(Element row) {
        Elements codeCells = row.select("td.codecol");

        if (!codeCells.isEmpty()) {
            return false;
        }

        String text = row.text().trim();

        if (containsCourseCode(text)) {
            return false;
        }

        if (text.length() > 150) {
            return false;
        }

        return row.select("td, th").size() == 1
                || row.hasClass("areaheader")
                || row.hasClass("listsum")
                || row.select("td.areaheader, th.areaheader").size() > 0;
    }

    private static String cleanSectionName(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    private static boolean containsCourseCode(String text) {
        Pattern pattern = Pattern.compile("([A-Z]{2,5})\\s*(\\d{4})");
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }

    private static List<String> extractCourseCodes(String text) {
        List<String> codes = new ArrayList<>();

        Pattern pattern = Pattern.compile("([A-Z]{2,5})\\s*(\\d{4})");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String code = matcher.group(1) + " " + matcher.group(2);

            if (!codes.contains(code)) {
                codes.add(code);
            }
        }

        return codes;
    }

    private static boolean isOrRow(String rowText, Element row) {
        String lower = rowText.toLowerCase();
        String className = row.className().toLowerCase();

        return lower.startsWith("or ")
                || lower.contains(" or ")
                || row.select("td.codecol.orclass").size() > 0
                || row.select("td.orclass").size() > 0
                || className.contains("orclass");
    }

    private static boolean isImportantRuleText(String rowText) {
        String lower = rowText.toLowerCase();

        return lower.contains("select")
                || lower.contains("choose")
                || lower.contains("at least")
                || lower.contains("categories")
                || lower.contains("cannot satisfy multiple")
                || lower.contains("course units")
                || lower.contains("one course unit from each");
    }

    private static int extractFirstNumber(String text) {
        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return 0;
    }

    private static List<MajorRequirement> mergeOrRows(List<MajorRequirement> input) {
        List<MajorRequirement> merged = new ArrayList<>();

        for (MajorRequirement req : input) {
            boolean startsWithOr = req.note != null
                    && req.note.trim().toLowerCase().startsWith("or ");

            if (!merged.isEmpty() && startsWithOr) {
                MajorRequirement previous = merged.get(merged.size() - 1);

                boolean sameMajor = previous.majorName.equals(req.majorName);
                boolean sameSection = previous.sectionName.equals(req.sectionName);

                boolean previousCanMerge =
                        previous.ruleType.equals("REQUIRED_ALL")
                                || previous.ruleType.equals("CHOOSE_ONE");

                boolean currentCanMerge =
                        req.ruleType.equals("REQUIRED_ALL")
                                || req.ruleType.equals("CHOOSE_ONE");

                if (sameMajor && sameSection && previousCanMerge && currentCanMerge) {
                    merged.remove(merged.size() - 1);

                    List<String> combinedOptions = new ArrayList<>();

                    for (String course : previous.options) {
                        if (!combinedOptions.contains(course)) {
                            combinedOptions.add(course);
                        }
                    }

                    for (String course : req.options) {
                        if (!combinedOptions.contains(course)) {
                            combinedOptions.add(course);
                        }
                    }

                    merged.add(new MajorRequirement(
                            previous.majorName,
                            previous.sectionName,
                            "CHOOSE_ONE",
                            1,
                            combinedOptions,
                            previous.note + " OR " + req.note
                    ));

                    continue;
                }
            }

            merged.add(req);
        }

        return merged;
    }
}