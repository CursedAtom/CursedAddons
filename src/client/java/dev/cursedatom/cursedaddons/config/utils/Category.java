package dev.cursedatom.cursedaddons.config.utils;

import java.util.List;

/**
 * POJO representing a top-level tab category in the config GUI, deserialized from {@code config_gui.json}.
 */
public class Category {
    private String nameKey;
    private List<ConfigItem> content;

    public Category() {}

    public String getNameKey() {
        return nameKey;
    }

    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }

    public List<ConfigItem> getContent() {
        return content;
    }

    public void setContent(List<ConfigItem> content) {
        this.content = content;
    }
}
