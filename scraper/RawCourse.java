import java.util.*;

public class RawCourse {
    private String courseId;
    private String courseName;
    private double creditUnits;
    private List<String> prerequisites;
    private List<String> corequisites;
    private List<String> mutuallyExclusive;
    private List<String> alsoOfferedAs;
    private boolean offeredInFall;
    private boolean offeredInSpring;
    private boolean notOfferedEveryYear;
    private String description;
    private List<String> required;

    public RawCourse(String courseId, String courseName, double creditUnits,
                     List<String> prerequisites, List<String> corequisites,
                     List<String> mutuallyExclusive, List<String> alsoOfferedAs,
                     boolean offeredInFall, boolean offeredInSpring,
                     boolean notOfferedEveryYear,
                     String description, List<String> required) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.creditUnits = creditUnits;
        this.prerequisites = prerequisites;
        this.corequisites = corequisites;
        this.mutuallyExclusive = mutuallyExclusive;
        this.alsoOfferedAs = alsoOfferedAs;
        this.offeredInFall = offeredInFall;
        this.offeredInSpring = offeredInSpring;
        this.notOfferedEveryYear = notOfferedEveryYear;
        this.description = description;
        this.required = required;
    }

    public String getCourseId() {
        return courseId;
    }

    public List<String> getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(List<String> prerequisites) {
        this.prerequisites = prerequisites;
    }

    public void addRequiredProgram(String programName) {
        if (!required.contains(programName)) {
            required.add(programName);
        }
    }

    public String toString() {
        return courseId + " - " + courseName + " (" + creditUnits + " CU)";
    }
}