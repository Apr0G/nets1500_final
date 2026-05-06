package pennplanner.model;

import java.util.List;
import java.util.Set;

public class StudentProfile {
    private final String majorId;
    private final List<String> minorIds;
    private final List<String> desiredElectiveIds;
    private final Set<String> excludedCourseIds;
    private final List<String> interests;
    private final double maxCUsPerSemester;
    private final int totalSemesters;
    private final boolean startInFall;
    private final int startYear;
    private final String school;

    public StudentProfile(
            String majorId,
            List<String> minorIds,
            List<String> desiredElectiveIds,
            Set<String> excludedCourseIds,
            List<String> interests,
            double maxCUsPerSemester,
            int totalSemesters,
            boolean startInFall,
            int startYear,
            String school) {
        this.majorId = majorId;
        this.minorIds = minorIds;
        this.desiredElectiveIds = desiredElectiveIds;
        this.excludedCourseIds = excludedCourseIds;
        this.interests = interests;
        this.maxCUsPerSemester = maxCUsPerSemester;
        this.totalSemesters = totalSemesters;
        this.startInFall = startInFall;
        this.startYear = startYear;
        this.school = school;
    }

    public String getMajorId() { return majorId; }
    public List<String> getMinorIds() { return minorIds; }
    public List<String> getDesiredElectiveIds() { return desiredElectiveIds; }
    public Set<String> getExcludedCourseIds() { return excludedCourseIds; }
    public List<String> getInterests() { return interests; }
    public double getMaxCUsPerSemester() { return maxCUsPerSemester; }
    public int getTotalSemesters() { return totalSemesters; }
    public boolean isStartInFall() { return startInFall; }
    public int getStartYear() { return startYear; }
    public String getSchool() { return school; }

    public double getTargetCUs() {
        return school != null && school.equalsIgnoreCase("CAS") ? 32.0 : 38.0;
    }
}
