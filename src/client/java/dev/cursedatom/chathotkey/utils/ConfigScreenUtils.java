package dev.cursedatom.chathotkey.utils;

import dev.cursedatom.chathotkey.config.SpecialUnits;
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

import static dev.cursedatom.chathotkey.utils.TextUtils.trans;

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
                        if (unit.enabled) {
                            displayText = displayBase.withStyle(ChatFormatting.GREEN);
                        } else {
                            displayText = displayBase.withStyle(ChatFormatting.RED);
                        }
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

                    return new MultiElementListEntry<>(displayText, unit, entries, true);
                });
            default:
                return null;
        }
    }
}
