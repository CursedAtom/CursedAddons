package dev.cursedatom.cursedaddons.config.utils;

import java.util.List;

public class FieldDefinition {
    private String name;
    private String type;
    private String labelKey;
    private String hintKey;
    private List<String> options;
    private List<String> optionKeys;
    private Object defaultValue;
    private Integer maxLength;
    private Boolean required;

    public FieldDefinition() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }

    public String getHintKey() {
        return hintKey;
    }

    public void setHintKey(String hintKey) {
        this.hintKey = hintKey;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public List<String> getOptionKeys() {
        return optionKeys;
    }

    public void setOptionKeys(List<String> optionKeys) {
        this.optionKeys = optionKeys;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }
}
