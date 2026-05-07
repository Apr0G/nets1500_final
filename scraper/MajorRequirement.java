import java.util.*;

public class MajorRequirement {
    public String majorName;
    public String sectionName;
    public String ruleType;
    public int coursesNeeded;
    public List<String> options;
    public List<List<String>> optionGroups;
    public String note;

    public MajorRequirement(String majorName, String sectionName, String ruleType,
                            int coursesNeeded, List<String> options, String note) {
        this.majorName = majorName;
        this.sectionName = sectionName;
        this.ruleType = ruleType;
        this.coursesNeeded = coursesNeeded;
        this.options = options;
        this.optionGroups = new ArrayList<>();
        this.note = note;
    }

    public MajorRequirement(String majorName, String sectionName, String ruleType,
                            int coursesNeeded, List<List<String>> optionGroups,
                            String note, boolean usesOptionGroups) {
        this.majorName = majorName;
        this.sectionName = sectionName;
        this.ruleType = ruleType;
        this.coursesNeeded = coursesNeeded;
        this.optionGroups = optionGroups;
        this.options = flattenOptionGroups(optionGroups);
        this.note = note;
    }

    private List<String> flattenOptionGroups(List<List<String>> optionGroups) {
        List<String> flattened = new ArrayList<>();

        for (List<String> group : optionGroups) {
            for (String course : group) {
                if (!flattened.contains(course)) {
                    flattened.add(course);
                }
            }
        }

        return flattened;
    }
}