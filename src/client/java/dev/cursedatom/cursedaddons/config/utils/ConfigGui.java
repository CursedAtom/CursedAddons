package dev.cursedatom.cursedaddons.config.utils;

import java.util.List;

public class ConfigGui {
    private int version;
    private List<Category> categories;

    public ConfigGui() {}

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }
}
