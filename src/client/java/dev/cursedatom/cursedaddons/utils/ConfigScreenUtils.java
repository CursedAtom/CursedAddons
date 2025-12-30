package dev.cursedatom.cursedaddons.utils;

import dev.cursedatom.cursedaddons.config.SpecialUnits;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;

import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dev.cursedatom.cursedaddons.utils.TextUtils.trans;

public class ConfigScreenUtils {

    public static Component getTooltip(String key, String variableType) {
        return getTooltip(key, variableType, ConfigUtils.get(key)); // Use current value if default not avail or logic simplification
    }

    public static Component getTooltip(String key, String variableType, Object defaultVal) {
        Component keyName = TextUtils.of(key).copy().withStyle(ChatFormatting.GOLD);
        Component main = trans(key + ".@Tooltip").copy().withStyle(ChatFormatting.WHITE);
        Component type = TextUtils.trans("texts.variableType", variableType).copy().withStyle(ChatFormatting.GRAY);
        MutableComponent tooltip = TextUtils.empty().copy();
        tooltip.append(keyName).append("§r\n").append(main).append("§r\n").append(type);
        return tooltip;
    }

    public static AbstractConfigListEntry<?> getEntryBuilder(ConfigEntryBuilder eb, String type, String key, String errorSupplier, int... args) {
        Component tooltip = getTooltip(key, type);
        Object defVal = ConfigUtils.DEFAULT_CONFIG != null ? ConfigUtils.DEFAULT_CONFIG.get(key) : ConfigUtils.get(key);
        
        switch (type) {
            case "boolean":
                return eb.startBooleanToggle(trans(key), (boolean) ConfigUtils.get(key))
                        .setDefaultValue((boolean) defVal).setTooltip(tooltip)
                        .setSaveConsumer(v -> ConfigUtils.set(key, v)).build();
            case "MacroList":
                @SuppressWarnings("unchecked")
                List<Object> currentList = (List<Object>) ConfigUtils.get(key);
                @SuppressWarnings("unchecked")
                List<Object> defaultList = (List<Object>) defVal;
                return new NestedListListEntry<SpecialUnits.MacroUnit, MultiElementListEntry<SpecialUnits.MacroUnit>>(
                        trans(key), SpecialUnits.MacroUnit.fromList(currentList), true,
                        () -> Optional.of(new Component[]{tooltip}), v -> ConfigUtils.set(key, v),
                        () -> SpecialUnits.MacroUnit.fromList(defaultList),
                        eb.getResetButtonKey(), true, true, (passedUnit, ignored) -> {
                    SpecialUnits.MacroUnit unit = (passedUnit == null) ? new SpecialUnits.MacroUnit() : passedUnit;

                    Component displayText;
                    if (passedUnit == null || unit.key.equals(InputConstants.UNKNOWN.getName())) {
                        displayText = trans(key + ".@New");
                    } else {
                        String keyDisplay = InputConstants.getKey(unit.key).getDisplayName().getString();
                        MutableComponent displayBase;
                        if (unit.modifier == SpecialUnits.KeyModifiers.NONE) {
                            displayBase = trans(key + ".@Display", "§6" + keyDisplay, unit.command).copy();
                        } else {
                            displayBase = trans(key + ".@Display", "§6" + unit.modifier + " + " + keyDisplay, unit.command).copy();
                        }
                        String status = unit.enabled ? " [Enabled]" : " [Disabled]";
                        displayBase.append(Component.literal(status).withStyle(unit.enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
                        displayText = displayBase;
                    }

                    List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
                    SpecialUnits.MacroUnit defaultObj = new SpecialUnits.MacroUnit();

                    entries.add(eb.startKeyCodeField(trans(key + ".Key"), InputConstants.getKey(unit.key))
                            .setTooltip(getTooltip(key + ".Key", "keycode", InputConstants.getKey(unit.key)))
                            .setDefaultValue(InputConstants.getKey(defaultObj.key))
                            .setKeySaveConsumer(k -> unit.key = k.getName()).build());

                    entries.add(eb.startEnumSelector(trans(key + ".Modifier"), SpecialUnits.KeyModifiers.class,
                                    unit.modifier).setTooltip(getTooltip(key + ".Modifier", "EnumKeyModifiers", unit.modifier))
                            .setDefaultValue(defaultObj.modifier).setSaveConsumer(v -> unit.modifier = v).build());

                    entries.add(eb.startStrField(trans(key + ".Command"), unit.command)
                            .setTooltip(getTooltip(key + ".Command", "String", unit.command))
                            .setDefaultValue(defaultObj.command).setSaveConsumer(v -> unit.command = v).build());

                    // Add "Enabled" toggle
                    entries.add(eb.startBooleanToggle(trans(key + ".Enabled"), unit.enabled)
                            .setTooltip(getTooltip(key + ".Enabled", "boolean", defaultObj.enabled))
                            .setDefaultValue(defaultObj.enabled).setSaveConsumer(v -> unit.enabled = v).build());

                    return new MultiElementListEntry<>(displayText, unit, entries, false);
                });
            case "AliasList":
                @SuppressWarnings("unchecked")
                List<Object> currentAliasList = (List<Object>) ConfigUtils.get(key);
                @SuppressWarnings("unchecked")
                List<Object> defaultAliasList = (List<Object>) defVal;
                return new NestedListListEntry<SpecialUnits.AliasUnit, MultiElementListEntry<SpecialUnits.AliasUnit>>(
                        trans(key), SpecialUnits.AliasUnit.fromList(currentAliasList), true,
                        () -> Optional.of(new Component[]{tooltip}), v -> ConfigUtils.set(key, v),
                        () -> SpecialUnits.AliasUnit.fromList(defaultAliasList),
                        eb.getResetButtonKey(), true, true, (passedUnit, ignored) -> {
                    SpecialUnits.AliasUnit unit = (passedUnit == null) ? new SpecialUnits.AliasUnit() : passedUnit;

                    Component displayText;
                    if (passedUnit == null || unit.alias.isEmpty()) {
                        displayText = trans(key + ".@New");
                    } else {
                        MutableComponent displayBase = trans(key + ".@Display", "§6" + unit.alias, unit.replacement).copy();
                        String status = unit.enabled ? " [Enabled]" : " [Disabled]";
                        displayBase.append(Component.literal(status).withStyle(unit.enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
                        displayText = displayBase;
                    }

                    List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
                    SpecialUnits.AliasUnit defaultObj = new SpecialUnits.AliasUnit();

                    entries.add(eb.startStrField(trans(key + ".Alias"), unit.alias)
                            .setTooltip(getTooltip(key + ".Alias", "String", unit.alias))
                            .setDefaultValue(defaultObj.alias).setSaveConsumer(v -> unit.alias = v).build());

                    entries.add(eb.startStrField(trans(key + ".ReplaceWith"), unit.replacement)
                            .setTooltip(getTooltip(key + ".ReplaceWith", "String", unit.replacement))
                            .setDefaultValue(defaultObj.replacement).setSaveConsumer(v -> unit.replacement = v).build());

                    // Add "Enabled" toggle
                    entries.add(eb.startBooleanToggle(trans(key + ".Enabled"), unit.enabled)
                            .setTooltip(getTooltip(key + ".Enabled", "boolean", defaultObj.enabled))
                            .setDefaultValue(defaultObj.enabled).setSaveConsumer(v -> unit.enabled = v).build());

                    return new MultiElementListEntry<>(displayText, unit, entries, false);
                });
            case "NotificationList":
                @SuppressWarnings("unchecked")
                List<Object> currentNotificationList = (List<Object>) ConfigUtils.get(key);
                @SuppressWarnings("unchecked")
                List<Object> defaultNotificationList = (List<Object>) defVal;
                return new NestedListListEntry<SpecialUnits.NotificationUnit, MultiElementListEntry<SpecialUnits.NotificationUnit>>(
                        trans(key), SpecialUnits.NotificationUnit.fromList(currentNotificationList), true,
                        () -> Optional.of(new Component[]{tooltip}), v -> ConfigUtils.set(key, v),
                        () -> SpecialUnits.NotificationUnit.fromList(defaultNotificationList),
                        eb.getResetButtonKey(), true, true, (passedUnit, ignored) -> {
                    SpecialUnits.NotificationUnit unit = (passedUnit == null) ? new SpecialUnits.NotificationUnit() : passedUnit;

                    Component displayText;
                    if (passedUnit == null || unit.pattern.isEmpty()) {
                        displayText = trans(key + ".@New");
                    } else {
                        MutableComponent displayBase = trans(key + ".@Display", "§6" + unit.pattern, unit.regex ? "regex" : "text").copy();
                        String status = unit.enabled ? " [Enabled]" : " [Disabled]";
                        displayBase.append(Component.literal(status).withStyle(unit.enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
                        displayText = displayBase;
                    }

                    List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
                    SpecialUnits.NotificationUnit defaultObj = new SpecialUnits.NotificationUnit();

                    entries.add(eb.startStrField(trans(key + ".Pattern"), unit.pattern)
                            .setTooltip(getTooltip(key + ".Pattern", "String", unit.pattern))
                            .setDefaultValue(defaultObj.pattern).setSaveConsumer(v -> unit.pattern = v).build());

                    entries.add(eb.startBooleanToggle(trans(key + ".Regex"), unit.regex)
                            .setTooltip(getTooltip(key + ".Regex", "boolean", unit.regex))
                            .setDefaultValue(defaultObj.regex).setSaveConsumer(v -> unit.regex = v).build());

                    entries.add(eb.startBooleanToggle(trans(key + ".SoundEnabled"), unit.soundEnabled)
                            .setTooltip(getTooltip(key + ".SoundEnabled", "boolean", unit.soundEnabled))
                            .setDefaultValue(defaultObj.soundEnabled).setSaveConsumer(v -> unit.soundEnabled = v).build());

                    entries.add(eb.startTextDescription(Component.literal("Full sound list: https://minecraft.wiki/w/Sounds.json (expand \'Sound events\')")).build());

                    entries.add(eb.startStrField(trans(key + ".Sound"), unit.sound)
                            .setDefaultValue(defaultObj.sound)
                            .setSaveConsumer(v -> unit.sound = v)
                            .setTooltip(getTooltip(key + ".Sound", "String", unit.sound))
                            .build());

                    entries.add(eb.startBooleanToggle(trans(key + ".TitleEnabled"), unit.titleEnabled)
                            .setTooltip(getTooltip(key + ".TitleEnabled", "boolean", unit.titleEnabled))
                            .setDefaultValue(defaultObj.titleEnabled).setSaveConsumer(v -> unit.titleEnabled = v).build());

                    entries.add(eb.startStrField(trans(key + ".Title"), unit.title)
                            .setTooltip(getTooltip(key + ".Title", "String", unit.title))
                            .setDefaultValue(defaultObj.title).setSaveConsumer(v -> unit.title = v).build());

                    entries.add(eb.startBooleanToggle(trans(key + ".CommandEnabled"), unit.commandEnabled)
                            .setTooltip(getTooltip(key + ".CommandEnabled", "boolean", unit.commandEnabled))
                            .setDefaultValue(defaultObj.commandEnabled).setSaveConsumer(v -> unit.commandEnabled = v).build());

                    entries.add(eb.startStrField(trans(key + ".Command"), unit.command)
                            .setTooltip(getTooltip(key + ".Command", "String", unit.command))
                            .setDefaultValue(defaultObj.command).setSaveConsumer(v -> unit.command = v).build());

                    // Add "Enabled" toggle
                    entries.add(eb.startBooleanToggle(trans(key + ".Enabled"), unit.enabled)
                            .setTooltip(getTooltip(key + ".Enabled", "boolean", defaultObj.enabled))
                            .setDefaultValue(defaultObj.enabled).setSaveConsumer(v -> unit.enabled = v).build());

                    return new MultiElementListEntry<>(displayText, unit, entries, false);
                });
            default:
                return null;
        }
    }
}
