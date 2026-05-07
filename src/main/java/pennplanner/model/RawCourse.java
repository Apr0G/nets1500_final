package pennplanner.model;

import java.util.List;

public class RawCourse {
    private String id;
    private String name;
    private String description;
    private double creditUnits;
    private List<String> prerequisites;
    private boolean offeredFall;
    private boolean offeredSpring;
    private boolean notOfferedEveryYear;
    private List<String> mutuallyExclusive;
    private List<String> alsoOfferedAs;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description == null ? "" : description; }
    public double getCreditUnits() { return creditUnits <= 0 ? 1.0 : creditUnits; }
    public List<String> getPrerequisites() { return prerequisites == null ? List.of() : prerequisites; }
    public boolean isOfferedFall() { return offeredFall; }
    public boolean isOfferedSpring() { return offeredSpring; }
    public boolean isNotOfferedEveryYear() { return notOfferedEveryYear; }
    public List<String> getMutuallyExclusive() { return mutuallyExclusive == null ? List.of() : mutuallyExclusive; }
    public List<String> getAlsoOfferedAs() { return alsoOfferedAs == null ? List.of() : alsoOfferedAs; }
}
