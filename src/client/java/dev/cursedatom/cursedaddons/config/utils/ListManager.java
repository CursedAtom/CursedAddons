package dev.cursedatom.cursedaddons.config.utils;

import dev.cursedatom.cursedaddons.utils.ConfigProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import com.google.common.collect.Lists;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ListManager<T extends AbstractConfigUnit> {
    private final String configKey;
    private final Class<?> unitClass;
    private final Function<T, String> displayFormatter;
    private final Function<T, String> tooltipFormatter;
    private final Consumer<T> onEdit;
    private final BiConsumer<T, Integer> onSave;

    private List<T> items;
    private int selectedIndex = -1;

    public ListManager(String configKey, Class<?> unitClass,
                      Function<T, String> displayFormatter, Function<T, String> tooltipFormatter,
                      Consumer<T> onEdit, BiConsumer<T, Integer> onSave) {
        this.configKey = configKey;
        this.unitClass = unitClass;
        this.displayFormatter = displayFormatter;
        this.tooltipFormatter = tooltipFormatter;
        this.onEdit = onEdit;
        this.onSave = onSave;
        loadItems();
    }

    @SuppressWarnings("unchecked")
    private void loadItems() {
        items = new ArrayList<>();
        Object listObj = ConfigProvider.get(configKey);
        if (listObj instanceof List) {
            try {
                items = (List<T>) AbstractConfigUnit.fromList((List<Object>) listObj, (Class<T>) unitClass);
            } catch (Exception e) {
                items = new ArrayList<>();
            }
        }
    }

    private void saveItems() {
        List<Object> mapList = new ArrayList<>();
        for (T item : items) {
            if (item instanceof AbstractConfigUnit) {
                mapList.add(((AbstractConfigUnit) item).toMap());
            }
        }
        ConfigProvider.set(configKey, mapList);
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public List<T> getItems() {
        return items;
    }

    public void addItem(T item) {
        items.add(item);
        saveItems();
    }

    public void updateItem(int index, T item) {
        if (index >= 0 && index < items.size()) {
            items.set(index, item);
            saveItems();
        }
    }

    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
            if (selectedIndex == index) {
                selectedIndex = -1;
            } else if (selectedIndex > index) {
                selectedIndex--;
            }
            saveItems();
        }
    }

    public List<AbstractWidget> getLegacyListWidgets(Minecraft minecraft, int startY, int centerX, int buttonWidth, int buttonHeight, Runnable refreshScreen) {
        List<AbstractWidget> widgets = new ArrayList<>();
        int actionButtonWidth = 60;
        int spacing = 10;
        int screenCenterX = minecraft.getWindow().getGuiScaledWidth() / 2;

        Button editButton = Button.builder(Component.literal("Edit"), button -> {
            if (selectedIndex >= 0 && selectedIndex < items.size()) {
                onEdit.accept(items.get(selectedIndex));
            }
        }).bounds(screenCenterX - actionButtonWidth/2, startY, actionButtonWidth, buttonHeight).build();
        widgets.add(editButton);

        Button deleteButton = Button.builder(Component.literal("Delete"), button -> {
            if (selectedIndex >= 0 && selectedIndex < items.size()) {
                removeItem(selectedIndex);
                selectedIndex = -1;
                refreshScreen.run();
            }
        }).bounds(screenCenterX - actionButtonWidth/2 - actionButtonWidth - spacing, startY, actionButtonWidth, buttonHeight).build();
        widgets.add(deleteButton);

        Button addButton = Button.builder(Component.literal("Add"), button -> {
            onEdit.accept(null);
        }).bounds(screenCenterX - actionButtonWidth/2 + actionButtonWidth + spacing, startY, actionButtonWidth, buttonHeight).build();
        widgets.add(addButton);

        int y = startY + 40;
        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            T item = items.get(i);
            String displayText = displayFormatter.apply(item);
            String tooltipText = tooltipFormatter.apply(item);

            Button.Builder buttonBuilder = Button.builder(Component.literal(displayText), b -> {
                selectedIndex = index;
                refreshScreen.run();
            }).bounds(centerX - 25, y, buttonWidth + 50, buttonHeight);

            if (!tooltipText.isEmpty()) {
                buttonBuilder.tooltip(Tooltip.create(Component.literal(tooltipText)));
            }

            Button button = buttonBuilder.build();

            if (i == selectedIndex) {
                button.setMessage(Component.literal("§6§l>§r " + displayText + " §6§l<§r"));
            }
            widgets.add(button);

            Button toggleButton = createToggleButton(item, index, centerX + 235, y, buttonHeight, refreshScreen);
            if (toggleButton != null) {
                widgets.add(toggleButton);
            }

            y += 25;
        }

        return widgets;
    }

    public List<AbstractWidget> getScrollableListWidgets(Minecraft minecraft, int startY, int centerX, int buttonWidth, int buttonHeight, Runnable refreshScreen) {
        List<AbstractWidget> widgets = new ArrayList<>();
        int actionButtonWidth = 60;
        int spacing = 10;
        int screenCenterX = minecraft.getWindow().getGuiScaledWidth() / 2;

        Button editButton = Button.builder(Component.literal("Edit"), button -> {
            if (selectedIndex >= 0 && selectedIndex < items.size()) {
                onEdit.accept(items.get(selectedIndex));
            }
        }).bounds(screenCenterX - actionButtonWidth/2, startY, actionButtonWidth, buttonHeight).build();
        widgets.add(editButton);

        Button deleteButton = Button.builder(Component.literal("Delete"), button -> {
            if (selectedIndex >= 0 && selectedIndex < items.size()) {
                removeItem(selectedIndex);
                selectedIndex = -1;
                refreshScreen.run();
            }
        }).bounds(screenCenterX - actionButtonWidth/2 - actionButtonWidth - spacing, startY, actionButtonWidth, buttonHeight).build();
        widgets.add(deleteButton);

        Button addButton = Button.builder(Component.literal("Add"), button -> {
            onEdit.accept(null);
        }).bounds(screenCenterX - actionButtonWidth/2 + actionButtonWidth + spacing, startY, actionButtonWidth, buttonHeight).build();
        widgets.add(addButton);

        // Create scrollable list
        int listWidth = 480;
        int listX = minecraft.getWindow().getGuiScaledWidth() / 2 - listWidth / 2;
        int listTop = startY + 30;
        int listBottom = minecraft.getWindow().getGuiScaledHeight() - 120;
        int listHeight = Math.max(100, listBottom - listTop);

        ConfigList list = new ConfigList(minecraft, listWidth, listHeight, listTop);
        list.setPosition(listX, listTop);

        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            T item = items.get(i);
            String displayText = displayFormatter.apply(item);
            String tooltipText = tooltipFormatter.apply(item);

            ConfigEntry entry = new ConfigEntry(() -> {
                selectedIndex = index;
                refreshScreen.run();
            });

            Button displayButton = Button.builder(Component.literal(displayText), b -> {
                selectedIndex = index;
                refreshScreen.run();
            }).bounds(0, 0, buttonWidth + 50, buttonHeight).build();

            if (!tooltipText.isEmpty()) {
                displayButton.setTooltip(Tooltip.create(Component.literal(tooltipText)));
            }

            if (index == selectedIndex) {
                displayButton.setMessage(Component.literal("§6§l>§r " + displayText + " §6§l<§r"));
            }

            entry.addButton(displayButton);

            Button toggleButton = createToggleButton(item, index, buttonWidth + 60, 0, buttonHeight, refreshScreen);
            if (toggleButton != null) {
                entry.addButton(toggleButton);
            }

            list.addConfigEntry(entry);
        }

        widgets.add(list);
        return widgets;
    }

    private Button createToggleButton(T item, int index, int x, int y, int height, Runnable refreshScreen) {
        try {
            Field enabledField = item.getClass().getDeclaredField("enabled");
            enabledField.setAccessible(true);
            boolean enabled = (boolean) enabledField.get(item);
            return Button.builder(Component.literal(enabled ? "§aON" : "§cOFF"), b -> {
                try {
                    enabledField.set(item, !enabled);
                    saveItems();
                    refreshScreen.run();
                } catch (IllegalAccessException e) {
                    // Ignore
                }
            }).bounds(x, y, 40, height).build();
        } catch (Exception e) {
            return null;
        }
    }

    public void onItemSaved(T item, int index) {
        if (index >= 0 && index < items.size()) {
            items.set(index, item);
        } else {
            items.add(item);
        }
        saveItems();
        onSave.accept(item, index);
    }

    public void saveItem(T item, int index) {
        if (index >= 0 && index < items.size()) {
            items.set(index, item);
        } else {
            items.add(item);
        }
        saveItems();
    }

    // Scrollable list classes
    public static class ConfigList extends ContainerObjectSelectionList<ListManager.ConfigEntry> {
        public ConfigList(Minecraft mc, int width, int height, int y) {
            super(mc, width, height, y, 25);
            this.centerListVertically = false;
        }

        @Override
        public int getRowWidth() {
            return 320;
        }

        @Override
        protected void renderListBackground(final GuiGraphics graphics) {
            // Don't render the default list background to avoid interfering with buttons
        }

        @Override
        protected void renderSelection(final GuiGraphics graphics, final ConfigEntry entry, final int outlineColor) {
            // Don't render selection background to avoid interfering with buttons
        }

        public void addConfigEntry(ConfigEntry entry) {
            this.addEntry(entry);
        }
    }

    public static class ConfigEntry extends ContainerObjectSelectionList.Entry<ListManager.ConfigEntry> {
        private final List<AbstractWidget> children = Lists.newArrayList();
        private final Runnable onSelect;

        public ConfigEntry(Runnable onSelect) {
            this.onSelect = onSelect;
        }

        public void addButton(Button button) {
            this.children.add(button);
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            // Position buttons relative to this entry
            int buttonY = this.getContentY();
            int displayButtonX = this.getContentX();
            int toggleButtonX = this.getContentX() + 260;

            if (!this.children.isEmpty()) {
                Button displayButton = (Button) this.children.get(0);
                displayButton.setPosition(displayButtonX, buttonY);
                displayButton.render(graphics, mouseX, mouseY, delta);

                if (this.children.size() > 1) {
                    Button toggleButton = (Button) this.children.get(1);
                    toggleButton.setPosition(toggleButtonX, buttonY);
                    toggleButton.render(graphics, mouseX, mouseY, delta);
                }
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.children;
        }

        public void select() {
            if (onSelect != null) {
                onSelect.run();
            }
        }
    }
}
