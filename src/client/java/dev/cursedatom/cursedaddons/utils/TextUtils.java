package dev.cursedatom.cursedaddons.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

import java.util.List;

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

    public static String toLegacyString(Component component) {
        FormattedCharSequence sequence = component.getVisualOrderText();
        StringBuilder sb = new StringBuilder();
        sequence.accept(new FormattedCharSink() {
            private Style lastStyle = Style.EMPTY;

            @Override
            public boolean accept(int index, Style style, int codePoint) {
                if (!style.equals(lastStyle)) {
                    if (style.getColor() != null && (lastStyle.getColor() == null || !style.getColor().equals(lastStyle.getColor()))) {
                        String colorCode = style.getColor().serialize();
                        ChatFormatting formatting = ChatFormatting.getByName(colorCode);
                        if (formatting != null) {
                            sb.append('§').append(formatting.getChar());
                        }
                    }
                    if (style.isBold() && !lastStyle.isBold()) {
                        sb.append('§').append(ChatFormatting.BOLD.getChar());
                    }
                    if (style.isItalic() && !lastStyle.isItalic()) {
                        sb.append('§').append(ChatFormatting.ITALIC.getChar());
                    }
                    if (style.isUnderlined() && !lastStyle.isUnderlined()) {
                        sb.append('§').append(ChatFormatting.UNDERLINE.getChar());
                    }
                    if (style.isStrikethrough() && !lastStyle.isStrikethrough()) {
                        sb.append('§').append(ChatFormatting.STRIKETHROUGH.getChar());
                    }
                    if (style.isObfuscated() && !lastStyle.isObfuscated()) {
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
