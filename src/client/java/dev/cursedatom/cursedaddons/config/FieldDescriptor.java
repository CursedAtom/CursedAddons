package dev.cursedatom.cursedaddons.config;

import java.util.List;

public class FieldDescriptor {
    public enum FieldType {
        TEXT, TOGGLE, KEYBIND, CYCLE
    }

    public final String fieldName;
    public final FieldType type;
    public final String hint;
    public final int maxLength;
    public final List<String> cycleOptions; // For CYCLE type

    public FieldDescriptor(String fieldName, FieldType type, String hint, int maxLength) {
        this.fieldName = fieldName;
        this.type = type;
        this.hint = hint;
        this.maxLength = maxLength;
        this.cycleOptions = null;
    }

    public FieldDescriptor(String fieldName, FieldType type, List<String> cycleOptions) {
        this.fieldName = fieldName;
        this.type = type;
        this.hint = null;
        this.maxLength = 0;
        this.cycleOptions = cycleOptions;
    }

    public FieldDescriptor(String fieldName, FieldType type) {
        this.fieldName = fieldName;
        this.type = type;
        this.hint = null;
        this.maxLength = 0;
        this.cycleOptions = null;
    }
}
