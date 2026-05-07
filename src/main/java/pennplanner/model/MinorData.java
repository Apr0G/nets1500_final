package pennplanner.model;

import java.util.List;

public class MinorData {
    private String id;
    private List<RuleEntry> rules;

    public String getId() { return id; }
    public List<RuleEntry> getRules() { return rules == null ? List.of() : rules; }
}
