package pennplanner.model;

import java.util.List;

public class RuleEntry {
    private String majorName;
    private String minorName;
    private String sectionName;
    private String ruleType;
    private int coursesNeeded;
    private List<String> options;

    public String getName() {
        return majorName != null ? majorName : minorName;
    }

    public String getSectionName() { return sectionName; }
    public String getRuleType() { return ruleType == null ? "" : ruleType; }
    public int getCoursesNeeded() { return coursesNeeded; }
    public List<String> getOptions() { return options == null ? List.of() : options; }

    public boolean isRequiredAll() { return "REQUIRED_ALL".equalsIgnoreCase(ruleType); }
    public boolean isChooseOne()   { return "CHOOSE_ONE".equalsIgnoreCase(ruleType); }
    public boolean isChooseN()     { return "CHOOSE_N".equalsIgnoreCase(ruleType); }
    public boolean isActionable()  { return isRequiredAll() || isChooseOne() || isChooseN(); }
}
