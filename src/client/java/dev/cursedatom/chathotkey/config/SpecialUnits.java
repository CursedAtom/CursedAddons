package dev.cursedatom.chathotkey.config;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SpecialUnits {

    public enum KeyModifiers {
        SHIFT, ALT, CTRL, NONE
    }

    public static class MacroUnit {
        public String key;
        public KeyModifiers modifier;
        public String command;
        public boolean enabled;

        public MacroUnit() {
            this.key = InputConstants.UNKNOWN.getName();
            this.modifier = KeyModifiers.NONE;
            this.command = "";
            this.enabled = true;
        }

        public MacroUnit(String key, KeyModifiers modifier, String command, boolean enabled) {
            this.key = key;
            this.modifier = modifier;
            this.command = command;
            this.enabled = enabled;
        }

        @SuppressWarnings("unchecked")
        public static MacroUnit of(Object ele) {
            if (ele instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) ele;
                String key = (String) map.getOrDefault("key", InputConstants.UNKNOWN.getName());
                KeyModifiers modifier = KeyModifiers.valueOf((String) map.getOrDefault("modifier", "NONE"));
                String command = (String) map.getOrDefault("command", "");
                boolean enabled = (boolean) map.getOrDefault("enabled", true);
                return new MacroUnit(key, modifier, command, enabled);
            } else if (ele instanceof MacroUnit) {
                return (MacroUnit) ele;
            } else {
                throw new IllegalArgumentException("Unexpected element type of Object: " + ele);
            }
        }

        public static List<MacroUnit> fromList(List<Object> list) {
            List<MacroUnit> arr = new ArrayList<>();
            for (Object ele : list) {
                arr.add(MacroUnit.of(ele));
            }
            return arr;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MacroUnit macroUnit = (MacroUnit) o;
            return key.equals(macroUnit.key) && modifier == macroUnit.modifier && command.equals(macroUnit.command) && enabled == macroUnit.enabled;
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, modifier, command, enabled);
        }

        @Override
        public String toString() {
            return "MacroUnit{key='" + key + "', modifier=" + modifier + ", command='" + command + "', enabled=" + enabled + "}";
        }
    }
}
