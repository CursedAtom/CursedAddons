package dev.cursedatom.cursedaddons.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.List;

public class TextUtils {
    public static final String PREFIX = "key.cursedaddons.";
    public static final Component SPACER = literal("").copy().setStyle(Style.EMPTY);

    public static Component literal(String str) {
        return Component.literal(str);
    }

    public static Component transWithPrefix(String str, String prefix) {
        return Component.translatable(prefix + str);
    }

    public static Component transWithPrefix(String str, String prefix, Object... args) {
        return Component.translatable(prefix + str, args);
    }

    public static Component trans(String str, Object... args) {
        return transWithPrefix(str, PREFIX, args);
    }

    public static Component trans(String str) {
        return transWithPrefix(str, PREFIX);
    }

    public static Component of(String str) {
        return Component.nullToEmpty(str);
    }

    public static Component empty() {
        return Component.empty();
    }
    
    public static Component textArray2text(List<Component> texts) {
        MutableComponent result = (MutableComponent) literal("");
        for (int i = 0; i < texts.size(); i++) {
            result.append(texts.get(i));
            if (i != texts.size() - 1) {
                result.append(literal("\n"));
            }
        }
        return result;
    }
}
