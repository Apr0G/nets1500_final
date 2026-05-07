package pennplanner.model;

import java.util.ArrayList;
import java.util.List;

public class Schedule {
    private final List<Semester> semesters = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void addSemester(Semester s) { semesters.add(s); }
    public void addWarning(String w) { warnings.add(w); }

    public List<Semester> getSemesters() { return semesters; }
    public List<String> getWarnings() { return warnings; }

    public double getTotalCUs() {
        return semesters.stream().mapToDouble(Semester::getTotalCUs).sum();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔════════════════════════════════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                              PENN UNDERGRADUATE COURSE PLAN                                       ║\n");
        sb.append("╠════════════════════════════════════════════════════════════════════════════════════════════════════╣\n");

        for (Semester sem : semesters) {
            sb.append(String.format("║  %-98s║\n", sem.getLabel() + "  [" + String.format("%.1f", sem.getTotalCUs()) + " CU]"));
            sb.append("╠════════════════════════════════════════════════════════════════════════════════════════════════════╣\n");
            for (Course c : sem.getCourses()) {
                String line = "  • " + c.getId() + " – " + c.getName() + "  (" + c.getCreditUnits() + " CU)";
                sb.append(String.format("║  %-98s║\n", line.length() > 98 ? line.substring(0, 97) + "…" : line));
            }
            if (sem.getCourses().isEmpty()) {
                sb.append(String.format("║  %-98s║\n", "  (no courses)"));
            }
            sb.append("╠════════════════════════════════════════════════════════════════════════════════════════════════════╣\n");
        }

        sb.append(String.format("║  %-98s║\n", "TOTAL:  " + String.format("%.1f", getTotalCUs()) + " CU across " + semesters.size() + " semester(s)"));
        sb.append("╚════════════════════════════════════════════════════════════════════════════════════════════════════╝\n");

        if (!warnings.isEmpty()) {
            sb.append("\nWARNINGS:\n");
            for (String w : warnings) {
                sb.append("  ⚠  ").append(w).append("\n");
            }
        }

        return sb.toString();
    }
}
