package com.qingtu.agent.tool;

import java.util.List;
import java.util.Map;

/**
 * 工具描述（用于 LLM 了解工具能力）
 */
public class ToolDefinition {
    private String name;
    private String description;
    private Map<String, ToolParameter> parameters;
    private String category;  // doc/rag/web/weather/school

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, ToolParameter> getParameters() { return parameters; }
    public void setParameters(Map<String, ToolParameter> parameters) { this.parameters = parameters; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public static class ToolParameter {
        private String type;        // string/number/boolean/object/array
        private String description;
        private boolean required;
        private Object defaultValue;
        private List<String> enumValues;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        public Object getDefaultValue() { return defaultValue; }
        public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
        public List<String> getEnumValues() { return enumValues; }
        public void setEnumValues(List<String> enumValues) { this.enumValues = enumValues; }
    }

    /**
     * 工具执行结果（使用 record）
     */
    public record ExecuteResult(
            boolean success,
            Object data,
            String errorMessage
    ) {
        public static ExecuteResult success(Object data) {
            return new ExecuteResult(true, data, null);
        }

        public static ExecuteResult error(String errorMessage) {
            return new ExecuteResult(false, null, errorMessage);
        }
    }
}
