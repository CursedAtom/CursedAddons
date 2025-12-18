package dev.cursedatom.chathotkey.features.general;

import dev.cursedatom.chathotkey.utils.LoggerUtils;
import dev.cursedatom.chathotkey.utils.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ClickEventsPreviewer {
    public static Style work(Style style) {
        if (style == null) {
            return null;
        }
        HoverEvent hoverEvent = style.getHoverEvent();
        Component textToAppend = getComponentToAppend(style);

        if (isModified(style) || textToAppend == null) {
            return style;
        }
        Component textToAppendWithTwoEmptyLinesInFront = TextUtils.literal("\n\n").copy().append(textToAppend);
        if (hoverEvent == null) {
            style = style.withHoverEvent(new HoverEvent.ShowText(textToAppend));
        } else {
            Component oldHoverComponent = null;
            if (hoverEvent instanceof HoverEvent.ShowText) {
                 oldHoverComponent = ((HoverEvent.ShowText) hoverEvent).value();
            }
            
            if (oldHoverComponent != null && !oldHoverComponent.getString().isBlank()) {
                Component newHoverComponent = (TextUtils.SPACER.copy().append(oldHoverComponent)).append(textToAppendWithTwoEmptyLinesInFront);
                style = style.withHoverEvent(new HoverEvent.ShowText(newHoverComponent));
            } else {
                 HoverEvent.EntityTooltipInfo entityContent = null;
                 ItemStack itemStack = null;

                 if (hoverEvent instanceof HoverEvent.ShowEntity) {
                     entityContent = ((HoverEvent.ShowEntity) hoverEvent).entity();
                 }
                 if (hoverEvent instanceof HoverEvent.ShowItem) {
                     itemStack = ((HoverEvent.ShowItem) hoverEvent).item();
                 }

                if (entityContent != null) {
                    oldHoverComponent = TextUtils.textArray2text(entityContent.getTooltipLines());
                } else if (itemStack != null) {
                     oldHoverComponent = TextUtils.textArray2text(
                            Screen.getTooltipFromItem(Minecraft.getInstance(), itemStack)
                    );
                }
                if (oldHoverComponent != null) {
                    Component newHoverComponent = (TextUtils.SPACER.copy().append(oldHoverComponent)).append(textToAppendWithTwoEmptyLinesInFront);
                    style = style.withHoverEvent(new HoverEvent.ShowText(newHoverComponent));
                } else {
                    style = style.withHoverEvent(new HoverEvent.ShowText(textToAppendWithTwoEmptyLinesInFront));
                }
            }
        }
        return style;
    }

    private static Component getComponentToAppend(Style style) {
        boolean hasInsertion = style.getInsertion() != null && !style.getInsertion().isBlank();
        boolean hasClickEvent = style.getClickEvent() != null;
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
            ClickEvent clickEvent = style.getClickEvent();
            
            String value = "";
            switch (clickEvent.action()) {
                case OPEN_URL:
                    value = ((ClickEvent.OpenUrl) clickEvent).uri().toString();
                    break;
                case OPEN_FILE:
                    value = ((ClickEvent.OpenFile) clickEvent).file().getAbsolutePath();
                    break;
                case RUN_COMMAND:
                    value = ((ClickEvent.RunCommand) clickEvent).command();
                    break;
                case SUGGEST_COMMAND:
                    value = ((ClickEvent.SuggestCommand) clickEvent).command();
                    break;
                case CHANGE_PAGE:
                    value = String.valueOf(((ClickEvent.ChangePage) clickEvent).page());
                    break;
                case COPY_TO_CLIPBOARD:
                    value = ((ClickEvent.CopyToClipboard) clickEvent).value();
                    break;
                default:
                    value = "[ERROR]";
            }
            Component valueComponent = TextUtils.of(value).copy().withStyle(ChatFormatting.GREEN);
            String action = clickEvent.action().getSerializedName();
            
            switch (action) {
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
                    LoggerUtils.warn("[ChatHotkey] Unknown clickEvent action type: " + action);
            }
        }
        return TextUtils.textArray2text(texts);
    }

    private static Boolean isModified(Style style) {
        HoverEvent hoverEvent = style.getHoverEvent();
        if (hoverEvent == null) {
            return false;
        }
        
        Component tooltip = null;
        if (hoverEvent instanceof HoverEvent.ShowText) {
             tooltip = ((HoverEvent.ShowText) hoverEvent).value();
        }

        if (tooltip == null || tooltip.getString().isBlank()) {
            return false;
        }
        return tooltip.getString().contains(TextUtils.trans("texts.PreviewClickEvents.overall").getString());
    }
}