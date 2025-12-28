package dev.cursedatom.cursedaddons.config;

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

    public static class AliasUnit {
        public String alias;
        public String replacement;
        public boolean enabled;

        public AliasUnit() {
            this.alias = "";
            this.replacement = "";
            this.enabled = true;
        }

        public AliasUnit(String alias, String replacement, boolean enabled) {
            this.alias = alias;
            this.replacement = replacement;
            this.enabled = enabled;
        }

        @SuppressWarnings("unchecked")
        public static AliasUnit of(Object ele) {
            if (ele instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) ele;
                String alias = (String) map.getOrDefault("alias", "");
                String replacement = (String) map.getOrDefault("replacement", "");
                boolean enabled = (boolean) map.getOrDefault("enabled", true);
                return new AliasUnit(alias, replacement, enabled);
            } else if (ele instanceof AliasUnit) {
                return (AliasUnit) ele;
            } else {
                throw new IllegalArgumentException("Unexpected element type of Object: " + ele);
            }
        }

        public static List<AliasUnit> fromList(List<Object> list) {
            List<AliasUnit> arr = new ArrayList<>();
            for (Object ele : list) {
                arr.add(AliasUnit.of(ele));
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
            AliasUnit aliasUnit = (AliasUnit) o;
            return alias.equals(aliasUnit.alias) && replacement.equals(aliasUnit.replacement) && enabled == aliasUnit.enabled;
        }

        @Override
        public int hashCode() {
            return Objects.hash(alias, replacement, enabled);
        }

        @Override
        public String toString() {
            return "AliasUnit{alias='" + alias + "', replacement='" + replacement + "', enabled=" + enabled + "}";
        }
    }

    public static class NotificationUnit {
        public String pattern;
        public boolean regex;
        public String sound;
        public boolean soundEnabled;
        public String title;
        public boolean titleEnabled;
        public String command;
        public boolean commandEnabled;
        public boolean enabled;

        public NotificationUnit() {
            this.pattern = "";
            this.regex = false;
            this.sound = "";
            this.soundEnabled = false;
            this.title = "";
            this.titleEnabled = false;
            this.command = "";
            this.commandEnabled = false;
            this.enabled = true;
        }

        public NotificationUnit(String pattern, boolean regex, String sound, boolean soundEnabled, String title, boolean titleEnabled, String command, boolean commandEnabled, boolean enabled) {
            this.pattern = pattern;
            this.regex = regex;
            this.sound = sound;
            this.soundEnabled = soundEnabled;
            this.title = title;
            this.titleEnabled = titleEnabled;
            this.command = command;
            this.commandEnabled = commandEnabled;
            this.enabled = enabled;
        }

        @SuppressWarnings("unchecked")
        public static NotificationUnit of(Object ele) {
            if (ele instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) ele;
                String pattern = (String) map.getOrDefault("pattern", "");
                boolean regex = (boolean) map.getOrDefault("regex", false);
                String sound = (String) map.getOrDefault("sound", "");
                boolean soundEnabled = (boolean) map.getOrDefault("soundEnabled", false);
                String title = (String) map.getOrDefault("title", "");
                boolean titleEnabled = (boolean) map.getOrDefault("titleEnabled", false);
                String command = (String) map.getOrDefault("command", "");
                boolean commandEnabled = (boolean) map.getOrDefault("commandEnabled", false);
                boolean enabled = (boolean) map.getOrDefault("enabled", true);
                return new NotificationUnit(pattern, regex, sound, soundEnabled, title, titleEnabled, command, commandEnabled, enabled);
            } else if (ele instanceof NotificationUnit) {
                return (NotificationUnit) ele;
            } else {
                throw new IllegalArgumentException("Unexpected element type of Object: " + ele);
            }
        }

        public static List<NotificationUnit> fromList(List<Object> list) {
            List<NotificationUnit> arr = new ArrayList<>();
            for (Object ele : list) {
                arr.add(NotificationUnit.of(ele));
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
            NotificationUnit that = (NotificationUnit) o;
            return regex == that.regex && soundEnabled == that.soundEnabled && titleEnabled == that.titleEnabled && commandEnabled == that.commandEnabled && enabled == that.enabled && pattern.equals(that.pattern) && sound.equals(that.sound) && title.equals(that.title) && command.equals(that.command);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pattern, regex, sound, soundEnabled, title, titleEnabled, command, commandEnabled, enabled);
        }

        @Override
        public String toString() {
            return "NotificationUnit{pattern='" + pattern + "', regex=" + regex + ", sound='" + sound + "', soundEnabled=" + soundEnabled + ", title='" + title + "', titleEnabled=" + titleEnabled + ", command='" + command + "', commandEnabled=" + commandEnabled + ", enabled=" + enabled + "}";
        }
    }
}
