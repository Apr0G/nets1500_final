package pennplanner.model;

import java.util.List;

public class MajorData {
    private String id;
    private String name;
    private String school;
    private List<String> requiredCourses;
    private List<ChoiceGroup> choiceGroups;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSchool() { return school; }
    public List<String> getRequiredCourses() { return requiredCourses == null ? List.of() : requiredCourses; }
    public List<ChoiceGroup> getChoiceGroups() { return choiceGroups == null ? List.of() : choiceGroups; }
}
