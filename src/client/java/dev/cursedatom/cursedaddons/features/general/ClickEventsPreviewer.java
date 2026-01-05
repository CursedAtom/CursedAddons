package dev.cursedatom.cursedaddons.features.general;

import dev.cursedatom.cursedaddons.utils.LoggerUtils;
import dev.cursedatom.cursedaddons.utils.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

public class ClickEventsPreviewer {
    public static Style work(Style style) {
        if (style == null) {
            return null;
        }

        HoverEvent hoverEvent = style.getHoverEvent();
        
        // Debug logging
        // if (style.getClickEvent() != null) {
        //    LoggerUtils.info("ClickEventsPreviewer: Processing style with ClickEvent: " + style.getClickEvent());
        // }

        // Skip if it's ShowItem or ShowEntity to preserve special rendering

        if (hoverEvent instanceof HoverEvent.ShowItem || hoverEvent instanceof HoverEvent.ShowEntity) {
            return style;
        }

        Component textToAppend = getComponentToAppend(style);
        if (textToAppend == null) {
            return style;
        }

        Component oldHoverComponent = null;
        if (hoverEvent instanceof HoverEvent.ShowText showText) {
             oldHoverComponent = showText.value();
        }
        
        // Idempotency check: Don't append if already modified
        if (oldHoverComponent != null && oldHoverComponent.getString().contains(TextUtils.trans("texts.PreviewClickEvents.overall").getString())) {
            return style;
        }

        Component newHoverComponent;
        if (oldHoverComponent != null && !oldHoverComponent.getString().isBlank()) {
            // Append with separator
            newHoverComponent = oldHoverComponent.copy().append(TextUtils.literal("\n\n")).append(textToAppend);
        } else {
            // No previous text, just the preview
            newHoverComponent = textToAppend;
        }

        return style.withHoverEvent(new HoverEvent.ShowText(newHoverComponent));
    }

    private static Component getComponentToAppend(Style style) {
        boolean hasInsertion = style.getInsertion() != null && !style.getInsertion().isBlank();
        ClickEvent clickEvent = style.getClickEvent();
        boolean hasClickEvent = clickEvent != null;

        if (!hasInsertion && !hasClickEvent) {
            return null;
        }

        List<Component> texts = new ArrayList<>();
        texts.add(TextUtils.trans("texts.PreviewClickEvents.overall"));

        if (hasInsertion) {
            texts.add(TextUtils.trans("texts.PreviewClickEvents.insertion", style.getInsertion()));
        }

        if (hasClickEvent) {
            texts.add(TextUtils.trans("texts.PreviewClickEvents.clickEvent"));

            String value = "";
            String actionName = "";

            if (clickEvent instanceof ClickEvent.OpenUrl openUrl) {
                value = openUrl.uri().toString();
                actionName = "open_url";
            } else if (clickEvent instanceof ClickEvent.OpenFile openFile) {
                value = openFile.file().getAbsolutePath();
                actionName = "open_file";
            } else if (clickEvent instanceof ClickEvent.RunCommand runCommand) {
                value = runCommand.command();
                actionName = "run_command";
            } else if (clickEvent instanceof ClickEvent.SuggestCommand suggestCommand) {
                value = suggestCommand.command();
                actionName = "suggest_command";
            } else if (clickEvent instanceof ClickEvent.ChangePage changePage) {
                value = String.valueOf(changePage.page());
                actionName = "change_page";
            } else if (clickEvent instanceof ClickEvent.CopyToClipboard copyToClipboard) {
                value = copyToClipboard.value();
                actionName = "copy_to_clipboard";
            } else {
                 value = "[Unknown]";
                 actionName = "unknown";
            }

            Component valueComponent = TextUtils.of(value).copy().withStyle(ChatFormatting.GREEN);

            switch (actionName) {
                case "open_url":
                    texts.add(TextUtils.trans("texts.PreviewClickEvents.clickEvent.openUrl", valueComponent));
                    break;
                case "open_file":
                    texts.add(TextUtils.trans("texts.PreviewClickEvents.clickEvent.openFile", valueComponent));
                    break;
                case "run_command":
                    texts.add(TextUtils.trans("texts.PreviewClickEvents.clickEvent.runCommand", valueComponent));
                    break;
                case "suggest_command":
                    texts.add(TextUtils.trans("texts.PreviewClickEvents.clickEvent.suggestCommand", valueComponent));
                    break;
                case "change_page":
                    texts.add(TextUtils.trans("texts.PreviewClickEvents.clickEvent.changePage", valueComponent));
                    break;
                case "copy_to_clipboard":
                    texts.add(TextUtils.trans("texts.PreviewClickEvents.clickEvent.copyToClipboard", valueComponent));
                    break;
                default:
                    LoggerUtils.warn("[CursedAddons] Unknown clickEvent type: " + clickEvent.getClass().getSimpleName());
            }
        }

        return TextUtils.textArray2text(texts);
    }
}