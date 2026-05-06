package pennplanner.model;

import java.util.List;

public class Course {
    private final String id;
    private final String name;
    private final String description;
    private final double creditUnits;
    private final List<String> prerequisites;
    private final boolean offeredFall;
    private final boolean offeredSpring;
    private final boolean notOfferedEveryYear;
    private final List<String> requiredFor;
    private final List<String> equivalentCourses;

    public Course(RawCourse raw) {
        this.id = raw.getId();
        this.name = raw.getName();
        this.description = raw.getDescription();
        this.creditUnits = raw.getCreditUnits();
        this.prerequisites = raw.getPrerequisites();
        this.offeredFall = raw.isOfferedFall();
        this.offeredSpring = raw.isOfferedSpring();
        this.notOfferedEveryYear = raw.isNotOfferedEveryYear();
        this.requiredFor = raw.getRequiredFor();

        java.util.Set<String> equiv = new java.util.LinkedHashSet<>();
        equiv.addAll(raw.getMutuallyExclusive());
        equiv.addAll(raw.getAlsoOfferedAs());
        this.equivalentCourses = List.copyOf(equiv);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getCreditUnits() { return creditUnits; }
    public List<String> getPrerequisites() { return prerequisites; }
    public boolean isOfferedFall() { return offeredFall; }
    public boolean isOfferedSpring() { return offeredSpring; }
    public boolean isNotOfferedEveryYear() { return notOfferedEveryYear; }
    public List<String> getRequiredFor() { return requiredFor; }
    public List<String> getEquivalentCourses() { return equivalentCourses; }
}
