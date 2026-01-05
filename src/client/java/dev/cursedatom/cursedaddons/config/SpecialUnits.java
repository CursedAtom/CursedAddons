package dev.cursedatom.cursedaddons.config;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;

public class SpecialUnits {

    public enum KeyModifiers {
        SHIFT, ALT, CTRL, NONE
    }

    public static class MacroUnit extends AbstractConfigUnit {
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

        public static MacroUnit of(Object ele) {
            return AbstractConfigUnit.of(ele, MacroUnit.class);
        }

        public static List<MacroUnit> fromList(List<Object> list) {
            return AbstractConfigUnit.fromList(list, MacroUnit.class);
        }
    }

    public static class AliasUnit extends AbstractConfigUnit {
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

        public static AliasUnit of(Object ele) {
            return AbstractConfigUnit.of(ele, AliasUnit.class);
        }

        public static List<AliasUnit> fromList(List<Object> list) {
            return AbstractConfigUnit.fromList(list, AliasUnit.class);
        }
    }

    public static class NotificationUnit extends AbstractConfigUnit {
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

        public static NotificationUnit of(Object ele) {
            return AbstractConfigUnit.of(ele, NotificationUnit.class);
        }

        public static List<NotificationUnit> fromList(List<Object> list) {
            return AbstractConfigUnit.fromList(list, NotificationUnit.class);
        }
    }
}
