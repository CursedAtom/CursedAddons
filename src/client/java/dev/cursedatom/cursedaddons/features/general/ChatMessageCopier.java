package dev.cursedatom.cursedaddons.features.general;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.cursedatom.cursedaddons.config.ConfigKeys;
import dev.cursedatom.cursedaddons.mixin.client.ChatComponentAccessor;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;
import dev.cursedatom.cursedaddons.utils.TextUtils;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ChatMessageCopier {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private ChatMessageCopier() {}

    public static boolean isEnabled() {
        return ConfigProvider.getBoolean(ConfigKeys.COPY_CHAT_MESSAGE_ENABLED, false);
    }

    public static boolean tryCopyMessageAt(double screenX, double screenY) {
        Minecraft mc = Minecraft.getInstance();
        ChatComponent chat = mc.gui.getChat();
        ChatComponentAccessor accessor = (ChatComponentAccessor) chat;

        List<GuiMessage.Line> trimmedMessages = accessor.getTrimmedMessages();
        List<GuiMessage> allMessages = accessor.getAllMessages();

        if (trimmedMessages.isEmpty() || allMessages.isEmpty()) {
            return false;
        }

        int trimmedIndex = getVisibleLineIndexAt(accessor, chat, mc, screenX, screenY);
        if (trimmedIndex < 0) {
            return false;
        }

        // Map trimmed line index to message index by counting endOfEntry markers
        int messageIndex = 0;
        for (int i = 0; i < trimmedIndex; i++) {
            if (trimmedMessages.get(i).endOfEntry()) {
                messageIndex++;
            }
        }

        if (messageIndex >= allMessages.size()) {
            return false;
        }

        Component content = allMessages.get(messageIndex).content();

        String text;
        String feedbackKey;

        boolean ctrlDown = isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean shiftDown = isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);

        if (ctrlDown && shiftDown) {
            // Ctrl+Shift+Right-click: JSON
            JsonElement json = ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, content).getOrThrow();
            text = GSON.toJson(json);
            feedbackKey = "texts.CopyChatMessage.copiedJson";
        } else if (ctrlDown) {
            // Ctrl+Right-click: legacy formatted
            text = TextUtils.toLegacyString(content);
            feedbackKey = "texts.CopyChatMessage.copiedFormatted";
        } else {
            // Right-click: plain text
            text = content.getString();
            feedbackKey = "texts.CopyChatMessage.copiedPlain";
        }

        mc.keyboardHandler.setClipboard(text);
        mc.gui.setOverlayMessage(TextUtils.trans(feedbackKey), false);

        return true;
    }

    /**
     * Maps screen coordinates to a trimmedMessages index by replicating the
     * coordinate math from ChatComponent's private render method.
     * Returns -1 if the click is not on a visible chat line.
     */
    private static int getVisibleLineIndexAt(ChatComponentAccessor accessor, ChatComponent chat, Minecraft mc, double screenX, double screenY) {
        List<GuiMessage.Line> trimmedMessages = accessor.getTrimmedMessages();
        int totalLines = trimmedMessages.size();
        if (totalLines == 0) {
            return -1;
        }

        double scale = accessor.invokeGetScale();
        int scrollPos = accessor.getChatScrollbarPos();
        int linesPerPage = chat.getLinesPerPage();
        int lineHeight = accessor.invokeGetLineHeight();

        // The chat renders at the bottom of the screen, above the input bar.
        // bottomY is the Y position (in scaled/chat coords) of the bottom of the lowest chat line.
        // In the render method: bottomY = (guiHeight - 40) / scale
        int guiHeight = mc.getWindow().getGuiScaledHeight();
        double bottomY = (guiHeight - 40) / scale;

        // Convert screen Y to chat-space Y
        double chatY = screenY / scale;

        // Check X bounds: chat starts at x=4 (after the scale+translate in render)
        // and extends to 4 + chatWidth. The render lambda uses updatePose with scale + translate(4, 0).
        double chatX = screenX / scale - 4.0;
        int chatWidth = ChatComponent.getWidth(mc.options.chatWidth().get());
        if (chatX < 0 || chatX > chatWidth) {
            return -1;
        }

        // Visible lines iterate from index (min(totalLines - scrollPos, linesPerPage) - 1) down to 0.
        // Each visible line i (0 = bottom-most) is rendered at Y = bottomY - i * lineHeight.
        // The line occupies Y range [topOfLine, topOfLine + lineHeight).
        // So the top of line i (from bottom) = bottomY - (i + 1) * lineHeight
        // and the bottom of line i = bottomY - i * lineHeight
        int visibleCount = Math.min(totalLines - scrollPos, linesPerPage);

        for (int i = 0; i < visibleCount; i++) {
            double lineBottom = bottomY - i * lineHeight;
            double lineTop = lineBottom - lineHeight;

            if (chatY > lineTop && chatY <= lineBottom) {
                // This is visible line i (counting from bottom), which corresponds to
                // trimmedMessages index: i + scrollPos
                return i + scrollPos;
            }
        }

        return -1;
    }

    private static boolean isKeyDown(int keyCode) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), keyCode);
    }
}
