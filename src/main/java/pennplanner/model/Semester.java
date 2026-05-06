package pennplanner.model;

import java.util.ArrayList;
import java.util.List;

public class Semester {
    private final int index;
    private final String season;
    private final int year;
    private final List<Course> courses = new ArrayList<>();

    public Semester(int index, String season, int year) {
        this.index = index;
        this.season = season;
        this.year = year;
    }

    public void addCourse(Course c) { courses.add(c); }

    public int getIndex() { return index; }
    public String getSeason() { return season; }
    public int getYear() { return year; }
    public List<Course> getCourses() { return courses; }

    public double getTotalCUs() {
        return courses.stream().mapToDouble(Course::getCreditUnits).sum();
    }

    public String getLabel() { return season + " " + year; }
}
