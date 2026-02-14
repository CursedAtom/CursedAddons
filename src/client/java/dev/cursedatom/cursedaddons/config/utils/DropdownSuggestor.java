package dev.cursedatom.cursedaddons.config.utils;

import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders a dropdown suggestion list below an {@link net.minecraft.client.gui.components.EditBox}
 * and handles keyboard and mouse navigation for picking a suggestion.
 */
public class DropdownSuggestor {
    private static final int MAX_VISIBLE = 10;
    private static final int ROW_HEIGHT = 12;
    private static final int BG_COLOR = 0xE0000000;
    private static final int BORDER_COLOR = 0xFF333333;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SELECTED_COLOR = 0xFF3366AA;

    private final EditBox editBox;
    private final Font font;
    private final List<String> allItems;

    private final List<String> filtered = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private boolean visible = false;

    public DropdownSuggestor(EditBox editBox, Font font, List<String> items) {
        this.editBox = editBox;
        this.font = font;
        this.allItems = items;
    }

    public void update(String text) {
        filtered.clear();
        selectedIndex = -1;
        scrollOffset = 0;

        if (text == null || text.isEmpty()) {
            visible = false;
            return;
        }

        String lower = text.toLowerCase(Locale.ROOT);
        for (String item : allItems) {
            if (item.toLowerCase(Locale.ROOT).contains(lower)) {
                filtered.add(item);
            }
        }

        visible = !filtered.isEmpty();
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!visible || filtered.isEmpty()) return;

        int x = editBox.getX();
        int y = editBox.getY() + editBox.getHeight() + 1;
        int w = editBox.getWidth();
        int rows = Math.min(filtered.size(), MAX_VISIBLE);
        int h = rows * ROW_HEIGHT + 2;

        // Background
        graphics.fill(x, y, x + w, y + h, BG_COLOR);
        // Border
        graphics.fill(x, y, x + w, y + 1, BORDER_COLOR);
        graphics.fill(x, y + h - 1, x + w, y + h, BORDER_COLOR);
        graphics.fill(x, y, x + 1, y + h, BORDER_COLOR);
        graphics.fill(x + w - 1, y, x + w, y + h, BORDER_COLOR);

        int end = Math.min(scrollOffset + MAX_VISIBLE, filtered.size());
        for (int i = scrollOffset; i < end; i++) {
            int rowY = y + 1 + (i - scrollOffset) * ROW_HEIGHT;

            if (i == selectedIndex) {
                graphics.fill(x + 1, rowY, x + w - 1, rowY + ROW_HEIGHT, SELECTED_COLOR);
            }

            String text = filtered.get(i);
            // Trim text to fit width
            String display = font.plainSubstrByWidth(text, w - 6);
            graphics.drawString(font, display, x + 3, rowY + 2, TEXT_COLOR);
        }

        // Scrollbar hint if there are more items
        if (filtered.size() > MAX_VISIBLE) {
            int barHeight = Math.max(4, h * MAX_VISIBLE / filtered.size());
            int barY = y + 1 + (h - 2 - barHeight) * scrollOffset / Math.max(1, filtered.size() - MAX_VISIBLE);
            graphics.fill(x + w - 3, barY, x + w - 1, barY + barHeight, 0xFF888888);
        }
    }

    public boolean keyPressed(int keyCode) {
        if (!visible || filtered.isEmpty()) return false;

        // Tab or Enter: accept
        if (keyCode == GLFW.GLFW_KEY_TAB || keyCode == GLFW.GLFW_KEY_ENTER) {
            if (selectedIndex >= 0 && selectedIndex < filtered.size()) {
                editBox.setValue(filtered.get(selectedIndex));
                visible = false;
                return true;
            }
            return false;
        }

        // Escape: close
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            visible = false;
            return true;
        }

        // Down
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            if (selectedIndex < filtered.size() - 1) {
                selectedIndex++;
                ensureVisible();
            }
            return true;
        }

        // Up
        if (keyCode == GLFW.GLFW_KEY_UP) {
            if (selectedIndex > 0) {
                selectedIndex--;
                ensureVisible();
            } else if (selectedIndex == -1) {
                selectedIndex = 0;
                ensureVisible();
            }
            return true;
        }

        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!visible || filtered.isEmpty()) return false;

        int x = editBox.getX();
        int y = editBox.getY() + editBox.getHeight() + 1;
        int w = editBox.getWidth();
        int rows = Math.min(filtered.size(), MAX_VISIBLE);
        int h = rows * ROW_HEIGHT + 2;

        if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
            int clickedRow = (int) ((mouseY - y - 1) / ROW_HEIGHT) + scrollOffset;
            if (clickedRow >= 0 && clickedRow < filtered.size()) {
                editBox.setValue(filtered.get(clickedRow));
                visible = false;
            }
            return true;
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!visible || filtered.size() <= MAX_VISIBLE) return false;

        int x = editBox.getX();
        int y = editBox.getY() + editBox.getHeight() + 1;
        int w = editBox.getWidth();
        int rows = Math.min(filtered.size(), MAX_VISIBLE);
        int h = rows * ROW_HEIGHT + 2;

        if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
            scrollOffset -= (int) amount;
            scrollOffset = Math.max(0, Math.min(scrollOffset, filtered.size() - MAX_VISIBLE));
            return true;
        }

        return false;
    }

    public boolean isVisible() {
        return visible;
    }

    private void ensureVisible() {
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + MAX_VISIBLE) {
            scrollOffset = selectedIndex - MAX_VISIBLE + 1;
        }
    }
}
