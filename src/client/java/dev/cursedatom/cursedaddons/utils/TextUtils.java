package dev.cursedatom.cursedaddons.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

import java.util.List;

/**
 * Utility class for creating and converting Minecraft {@link Component} text objects.
 * Provides helpers for translation keys with the mod prefix, legacy-format string conversion,
 * and joining component lists.
 */
public class TextUtils {
    public static final String PREFIX = "cursedaddons.";
    public static final Component SPACER = literal("");

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
    
    public static Component joinComponents(List<Component> texts) {
        MutableComponent result = (MutableComponent) literal("");
        for (int i = 0; i < texts.size(); i++) {
            result.append(texts.get(i));
            if (i != texts.size() - 1) {
                result.append(literal("\n"));
            }
        }
        return result;
    }

    /**
     * Converts a {@link Component} to a legacy {@code §}-formatted string, preserving color and style codes.
     * <p>
     * When a formatting attribute was active on the previous character but not the current one (i.e., formatting
     * was removed), a {@code §r} reset code is inserted first so that subsequent styles start from a clean state.
     */
    public static String toLegacyString(Component component) {
        FormattedCharSequence sequence = component.getVisualOrderText();
        StringBuilder sb = new StringBuilder();
        sequence.accept(new FormattedCharSink() {
            private Style lastStyle = Style.EMPTY;

            @Override
            public boolean accept(int index, Style style, int codePoint) {
                if (!style.equals(lastStyle)) {
                    boolean formattingRemoved =
                        (lastStyle.isBold() && !style.isBold()) ||
                        (lastStyle.isItalic() && !style.isItalic()) ||
                        (lastStyle.isUnderlined() && !style.isUnderlined()) ||
                        (lastStyle.isStrikethrough() && !style.isStrikethrough()) ||
                        (lastStyle.isObfuscated() && !style.isObfuscated()) ||
                        (lastStyle.getColor() != null && !lastStyle.getColor().equals(style.getColor()));

                    if (formattingRemoved) {
                        sb.append('§').append(ChatFormatting.RESET.getChar());
                    }

                    if (style.getColor() != null && (formattingRemoved || lastStyle.getColor() == null || !style.getColor().equals(lastStyle.getColor()))) {
                        String colorCode = style.getColor().serialize();
                        ChatFormatting formatting = ChatFormatting.getByName(colorCode);
                        if (formatting != null) {
                            sb.append('§').append(formatting.getChar());
                        }
                    }
                    if (style.isBold() && (formattingRemoved || !lastStyle.isBold())) {
                        sb.append('§').append(ChatFormatting.BOLD.getChar());
                    }
                    if (style.isItalic() && (formattingRemoved || !lastStyle.isItalic())) {
                        sb.append('§').append(ChatFormatting.ITALIC.getChar());
                    }
                    if (style.isUnderlined() && (formattingRemoved || !lastStyle.isUnderlined())) {
                        sb.append('§').append(ChatFormatting.UNDERLINE.getChar());
                    }
                    if (style.isStrikethrough() && (formattingRemoved || !lastStyle.isStrikethrough())) {
                        sb.append('§').append(ChatFormatting.STRIKETHROUGH.getChar());
                    }
                    if (style.isObfuscated() && (formattingRemoved || !lastStyle.isObfuscated())) {
                        sb.append('§').append(ChatFormatting.OBFUSCATED.getChar());
                    }

                    lastStyle = style;
                }
                sb.append((char) codePoint);
                return true;
            }
        });
        return sb.toString();
    }
}
