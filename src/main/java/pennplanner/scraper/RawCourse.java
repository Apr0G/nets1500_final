package pennplanner.scraper;

import java.util.*;

public class RawCourse {
    private String id;
    private String name;
    private double creditUnits;
    private List<String> prerequisites;
    private List<String> corequisites;
    private List<String> mutuallyExclusive;
    private List<String> alsoOfferedAs;
    private boolean offeredFall;
    private boolean offeredSpring;
    private boolean notOfferedEveryYear;
    private String description;

    public RawCourse(String id, String name, double creditUnits,
                     List<String> prerequisites, List<String> corequisites,
                     List<String> mutuallyExclusive, List<String> alsoOfferedAs,
                     boolean offeredFall, boolean offeredSpring,
                     boolean notOfferedEveryYear,
                     String description) {
        this.id = id;
        this.name = name;
        this.creditUnits = creditUnits;
        this.prerequisites = prerequisites;
        this.corequisites = corequisites;
        this.mutuallyExclusive = mutuallyExclusive;
        this.alsoOfferedAs = alsoOfferedAs;
        this.offeredFall = offeredFall;
        this.offeredSpring = offeredSpring;
        this.notOfferedEveryYear = notOfferedEveryYear;
        this.description = description;
    }

    public String getCourseId() { return id; }

    public List<String> getPrerequisites() { return prerequisites; }

    public void setPrerequisites(List<String> prerequisites) { this.prerequisites = prerequisites; }

    public String toString() {
        return id + " - " + name + " (" + creditUnits + " CU)";
    }
}
