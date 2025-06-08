import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.locks.Condition;

public class AutomationRuleConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String ruleName;
    private String ruleType;
    private boolean isActive;
    private List<String> targetDevices;
    private List<Condition> conditions;
    private List<String> actions;

    public AutomationRuleConfig(String ruleName, String ruleType) {
        this.ruleName = ruleName;
        this.ruleType = ruleType;
        this.isActive = true;
    }

    /**
     *
     * Represnts a condition for automation rule.
     */

    public static class Condition implements Serializable {
        private String type; // "TIME", "DEVICE_STATE", "TEMPERATURE"
        private String operator; // "EQUAL", "GREATER_THAN", "LESS_THAN"
        private String value;

        public Condition(String type, String operator, String value) {
            this.type = type;
            this.operator = operator;
            this.value = value;
        }
        // Getters and setters
        public String getType() { return type; }
        public String getOperator() { return operator; }
        public String getValue() { return value; }
    }

    public static class Action implements Serializable {
        private String deviceName;
        private String command; // "TURN_ON", "TURN_OFF", "SET_TEMPERATURE"
        private String parameter;

        private Action(String deviceName, String command, String parameter) {
            this.deviceName = deviceName;
            this.command = command;
            this.parameter = parameter;
        }

        // Getters and setters
        public String getDeviceName() { return deviceName; }
        public String getCommand() { return command; }
        public String getParameter() { return parameter; }
    }
        // Getters and setters
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean isActive) { this.isActive = isActive; }

    public List<String> getTargetDevices() { return targetDevices; }
    public void setTargetDevices(List<String> targetDevices) { this.targetDevices = targetDevices; }

    public List<Condition> getConditions() { return conditions; }
    public void setConditions(List<Condition> conditions) { this.conditions = conditions; }

    public List<String> getActions() { return actions; }
    public void setActions(List<String> actions) { this.actions = actions; }
}
