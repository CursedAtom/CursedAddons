package dev.cursedatom.cursedaddons.config.utils;

import java.util.List;

/**
 * POJO representing a single config item within a {@link Category}, deserialized from {@code config_gui.json}.
 * An item may be a boolean toggle, a list with editable entries, or hidden.
 */
public class ConfigItem {
    private String type;
    private String key;
    private String labelKey;
    private String descriptionKey;
    private List<FieldDefinition> fields;
    private boolean hidden;

    public ConfigItem() {}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public void setDescriptionKey(String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }

    public List<FieldDefinition> getFields() {
        return fields;
    }

    public void setFields(List<FieldDefinition> fields) {
        this.fields = fields;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
