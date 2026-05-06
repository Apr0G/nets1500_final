package pennplanner.model;

import java.util.List;

public class ChoiceGroup {
    private String name;
    private List<String> courses;
    private int count;

    public String getName() { return name; }
    public List<String> getCourses() { return courses == null ? List.of() : courses; }
    public int getCount() { return count; }
}
