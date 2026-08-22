package com.qshop.sellbox.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qshop.sellbox.SellBoxConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

/** Client-only layout editor. Values in the JSON are offsets from the normal GUI coordinates. */
public final class SellBoxLayoutDebug {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "qshop_sellbox_layout.json";
    private static final int MAX_OFFSET = 512;
    private static final EnumMap<Widget, Position> DEFAULT_POSITIONS = defaultPositions();
    private static final EnumMap<Widget, Position> POSITIONS = new EnumMap<>(Widget.class);
    private static Widget selected = Widget.ITEM_TITLE;
    private static boolean enabled;
    private static boolean loaded;

    public enum Widget {
        ITEM_TITLE("Items title", 0),
        ITEM_INVENTORY("Inventory label", 0),
        TAB_ITEMS("Items tab", -1),
        TAB_OWNER("Owner tab", -1),
        OWNER_TITLE("Owner title", 1),
        OWNER_AVATAR("Owner avatar", 1),
        OWNER_INFO("Owner name/status", 1),
        OWNER_BUTTON("Choose player button", 1),
        MODE_LABEL("Sell mode label", 1),
        MODE_INTERVAL("Interval mode button", 1),
        MODE_CLOSED_GUI("GUI close mode button", 1),
        INTERVAL_LABEL("Sell interval label", 1),
        INTERVAL_INPUT("Interval input", 1),
        INTERVAL_UNIT("Interval unit button", 1),
        ACTION_BAR_NOTIFICATION("Action Bar checkbox", 1),
        CHAT_NOTIFICATION("Chat checkbox", 1);

        private final String label;
        private final int tab;

        Widget(String label, int tab) {
            this.label = label;
            this.tab = tab;
        }

        public String label() {
            return label;
        }

        private boolean visibleOnTab(int currentTab) {
            return tab < 0 || tab == currentTab;
        }
    }

    private SellBoxLayoutDebug() {}

    public static void beginScreen() {
        loaded = false;
        load();
    }

    public static boolean isEnabled() {
        return enabled && SellBoxConfig.layoutDebugEnabled();
    }

    public static boolean isConfiguredEnabled() {
        return SellBoxConfig.layoutDebugEnabled();
    }

    public static boolean toggle() {
        if (!SellBoxConfig.layoutDebugEnabled()) return false;
        load();
        enabled = !enabled;
        return enabled;
    }

    public static Widget selected() {
        return selected;
    }

    public static void ensureSelected(int tab) {
        if (selected.visibleOnTab(tab)) return;
        for (Widget widget : Widget.values()) {
            if (widget.visibleOnTab(tab)) {
                selected = widget;
                return;
            }
        }
    }

    public static void selectNext(int tab, boolean reverse) {
        List<Widget> visible = new ArrayList<>();
        for (Widget widget : Widget.values()) {
            if (widget.visibleOnTab(tab)) visible.add(widget);
        }
        if (visible.isEmpty()) return;
        int index = visible.indexOf(selected);
        if (index < 0) {
            selected = visible.get(0);
            return;
        }
        int next = Math.floorMod(index + (reverse ? -1 : 1), visible.size());
        selected = visible.get(next);
    }

    public static int x(Widget widget, int normalX) {
        return normalX + position(widget).x();
    }

    public static int y(Widget widget, int normalY) {
        return normalY + position(widget).y();
    }

    public static void moveSelected(int tab, int dx, int dy) {
        if (!selected.visibleOnTab(tab)) selectNext(tab, false);
        Position current = position(selected);
        POSITIONS.put(selected, new Position(
                clamp(current.x() + dx), clamp(current.y() + dy)));
        save();
    }

    public static void renderOverlay(GuiGraphics graphics, Font font,
                                     int x, int y, int width, int height) {
        int right = x + Math.max(1, width);
        int bottom = y + Math.max(1, height);
        graphics.fill(x, y, right, y + 1, 0xFFFFD54F);
        graphics.fill(x, bottom - 1, right, bottom, 0xFFFFD54F);
        graphics.fill(x, y, x + 1, bottom, 0xFFFFD54F);
        graphics.fill(right - 1, y, right, bottom, 0xFFFFD54F);

        String label = "F8 Debug | Tab: " + selected.label()
                + " | arrows: 5px | Alt: 1px";
        String position = "offset " + position(selected).x() + ", " + position(selected).y();
        int textWidth = Math.max(font.width(label), font.width(position));
        int panelX = 4;
        int panelY = 4;
        graphics.fill(panelX - 2, panelY - 2, panelX + textWidth + 4,
                panelY + font.lineHeight * 2 + 3, 0xCC111111);
        graphics.drawString(font, Component.literal(label), panelX, panelY, 0xFFFFD54F, false);
        graphics.drawString(font, Component.literal(position), panelX,
                panelY + font.lineHeight, 0xFFFFFFFF, false);
    }

    private static Position position(Widget widget) {
        return POSITIONS.getOrDefault(widget, DEFAULT_POSITIONS.getOrDefault(widget, new Position(0, 0)));
    }

    private static EnumMap<Widget, Position> defaultPositions() {
        EnumMap<Widget, Position> positions = new EnumMap<>(Widget.class);
        positions.put(Widget.ITEM_TITLE, new Position(0, 0));
        positions.put(Widget.ITEM_INVENTORY, new Position(0, 0));
        positions.put(Widget.TAB_ITEMS, new Position(0, 0));
        positions.put(Widget.TAB_OWNER, new Position(0, 0));
        positions.put(Widget.OWNER_TITLE, new Position(0, 0));
        positions.put(Widget.OWNER_AVATAR, new Position(0, 0));
        positions.put(Widget.OWNER_INFO, new Position(-1, -4));
        positions.put(Widget.OWNER_BUTTON, new Position(-1, -3));
        positions.put(Widget.MODE_LABEL, new Position(0, -9));
        positions.put(Widget.MODE_INTERVAL, new Position(-1, -7));
        positions.put(Widget.MODE_CLOSED_GUI, new Position(0, -7));
        positions.put(Widget.INTERVAL_LABEL, new Position(0, -12));
        positions.put(Widget.INTERVAL_INPUT, new Position(0, -17));
        positions.put(Widget.INTERVAL_UNIT, new Position(5, -17));
        positions.put(Widget.ACTION_BAR_NOTIFICATION, new Position(0, -19));
        positions.put(Widget.CHAT_NOTIFICATION, new Position(0, -16));
        return positions;
    }

    private static int clamp(int value) {
        return Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, value));
    }

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve(FILE_NAME);
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        POSITIONS.clear();
        Path file = file();
        if (!Files.isRegularFile(file)) return;
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) return;
            JsonObject widgets = root.getAsJsonObject().getAsJsonObject("widgets");
            if (widgets == null) return;
            for (Widget widget : Widget.values()) {
                JsonElement raw = widgets.get(widget.name().toLowerCase(Locale.ROOT));
                if (raw == null || !raw.isJsonObject()) continue;
                JsonObject value = raw.getAsJsonObject();
                POSITIONS.put(widget, new Position(
                        clamp(readInt(value, "x")), clamp(readInt(value, "y"))));
            }
        } catch (Exception exception) {
            System.err.println("[qshop_sellbox] Could not read layout debug JSON: " + exception.getMessage());
        }
    }

    private static int readInt(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
                ? value.getAsInt() : 0;
    }

    private static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("description", "Component offsets relative to the normal GUI layout.");
        JsonObject widgets = new JsonObject();
        for (Widget widget : Widget.values()) {
            Position position = position(widget);
            JsonObject value = new JsonObject();
            value.addProperty("x", position.x());
            value.addProperty("y", position.y());
            widgets.add(widget.name().toLowerCase(Locale.ROOT), value);
        }
        root.add("widgets", widgets);

        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            System.err.println("[qshop_sellbox] Could not save layout debug JSON: " + exception.getMessage());
        }
    }

    private record Position(int x, int y) {}
}
