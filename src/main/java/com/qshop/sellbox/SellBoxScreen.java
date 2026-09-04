package com.qshop.sellbox;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qshop.sellbox.client.SellBoxEmiCompat;
import com.qshop.sellbox.client.SellBoxLayoutDebug;
import com.qshop.sellbox.client.SellBoxTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

public final class SellBoxScreen extends AbstractContainerScreen<SellBoxMenu> {
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DARK_TEXT_COLOR = 0xFF555555;
    private static final int ONLINE_COLOR = 0xFF2E8B57;
    private static final int OFFLINE_COLOR = 0xFFC0392B;
    private static final int BUTTON_MIN_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 16;

    private enum IntervalUnit {
        SECONDS("qshop_sellbox.unit.seconds", 20L),
        MINUTES("qshop_sellbox.unit.minutes", 20L * 60L),
        HOURS("qshop_sellbox.unit.hours", 20L * 60L * 60L),
        GAME_DAYS("qshop_sellbox.unit.game_days", 24000L);

        private final String translationKey;
        private final long ticks;

        IntervalUnit(String translationKey, long ticks) {
            this.translationKey = translationKey;
            this.ticks = ticks;
        }

        private IntervalUnit next() {
            IntervalUnit[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        private long toTicks(long amount) {
            return Math.max(1L, amount) * ticks;
        }

        private long fromTicks(long ticks) {
            return Math.max(1L, Math.round((double) ticks / this.ticks));
        }
    }

    private int tab;
    private IntervalUnit intervalUnit = IntervalUnit.SECONDS;
    private LayeredEditBox intervalInput;
    private boolean settingsCommitted;

    private static final class LayeredEditBox extends EditBox {
        private boolean manualRender;

        private LayeredEditBox(Font font, int x, int y, int width, int height, Component message) {
            super(font, x, y, width, height, message);
        }

        private void renderManually(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            manualRender = true;
            render(graphics, mouseX, mouseY, partialTick);
            manualRender = false;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (manualRender) {
                super.renderWidget(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    public SellBoxScreen(SellBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 74;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        SellBoxLayoutDebug.beginScreen();
        // Keep the widget slightly inside the 96x12 texture, matching Q-shop's input layout.
        intervalInput = new LayeredEditBox(font, screenX(SellBoxLayoutDebug.Widget.INTERVAL_INPUT, 10),
                screenY(SellBoxLayoutDebug.Widget.INTERVAL_INPUT, 123), 92, 14,
                Component.translatable("qshop_sellbox.setting.interval_input"));
        intervalInput.setMaxLength(9);
        intervalInput.setFilter(value -> value.matches("\\d*"));
        intervalInput.setBordered(false);
        intervalInput.setTextColor(TEXT_COLOR);
        intervalInput.setTextColorUneditable(TEXT_COLOR);
        intervalInput.setCanLoseFocus(true);
        intervalInput.setValue(Long.toString(intervalUnit.fromTicks(menu.saleIntervalTicks())));
        // The input overlaps the player inventory's second row on the item tab.
        // Keep it out of the widget hit-test tree until the settings tab is active.
        intervalInput.setVisible(tab == 1);
        addRenderableWidget(intervalInput);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Match CreativeModeInventoryScreen: unselected top tabs are behind the panel.
        for (int page = 0; page < 2; page++) {
            if (page != tab) {
                SellBoxTextures.tab(graphics, tabX(page), tabY(page), page, false);
            }
        }
        SellBoxTextures.background(graphics, leftPos, topPos);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (tab == 0) {
            // Match the vanilla container labels: dark text without a drop shadow.
            graphics.drawString(font, title,
                    layoutX(SellBoxLayoutDebug.Widget.ITEM_TITLE, 8),
                    layoutY(SellBoxLayoutDebug.Widget.ITEM_TITLE, 6),
                    DARK_TEXT_COLOR, false);
            graphics.drawString(font, Component.translatable("container.inventory"),
                    layoutX(SellBoxLayoutDebug.Widget.ITEM_INVENTORY, 8),
                    layoutY(SellBoxLayoutDebug.Widget.ITEM_INVENTORY, inventoryLabelY - 1),
                    DARK_TEXT_COLOR, false);
        } else {
            drawText(graphics, Component.translatable("qshop_sellbox.tab.owner"), 8, 6, TEXT_COLOR);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        SellBoxEmiCompat.setSettingsSuppressed(tab == 1);
        syncIntervalInputPosition();
        renderBackground(graphics);
        if (tab == 0) {
            // Keep the vanilla container path on the item page, including item tooltips.
            super.render(graphics, mouseX, mouseY, partialTick);
            flushAll(graphics);
        } else {
            // The settings page has no visible slots; avoid rendering their item decorations
            // in the first place instead of trying to cover them afterward.
            renderBg(graphics, partialTick, mouseX, mouseY);
            graphics.flush();
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        if (tab == 1) {
            // Submit the opaque page background after the slot item buffer, then render content.
            SellBoxTextures.ownerBackground(graphics, leftPos, topPos);
            flushAll(graphics);
            renderOwnerPage(graphics, mouseX, mouseY);
        }
        renderSelectedTab(graphics);
        graphics.flush();
        graphics.pose().popPose();

        if (tab == 1 && intervalInput != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);
            intervalInput.renderManually(graphics, mouseX, mouseY, partialTick);
            graphics.flush();
            graphics.pose().popPose();
        }

        if (tab == 1 && menu.owner() != null
                && inside(mouseX, mouseY,
                screenX(SellBoxLayoutDebug.Widget.OWNER_AVATAR, 8),
                screenY(SellBoxLayoutDebug.Widget.OWNER_AVATAR, 20), 160, 20)) {
            graphics.renderTooltip(font,
                    Component.literal("UUID: " + menu.owner()), mouseX, mouseY);
            graphics.flush();
        }

        if (tab == 0) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 700);
            super.renderTooltip(graphics, mouseX, mouseY);
            graphics.flush();
            graphics.pose().popPose();
        }

        renderDebugOverlay(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        // Render after the custom tab layer so item tooltips cannot be covered by it.
    }

    private void renderSelectedTab(GuiGraphics graphics) {
        int x = tabX(tab);
        SellBoxTextures.tab(graphics, x, tabY(tab), tab, true);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100);
        for (int page = 0; page < 2; page++) {
            graphics.renderItem(tabIcon(page), tabX(page) + 5, tabY(page) + 8);
        }
        graphics.pose().popPose();
        flushAll(graphics);
    }

    private ItemStack tabIcon(int page) {
        return page == 0 ? SellBoxMod.SELL_BOX_ITEM.get().getDefaultInstance()
                : new ItemStack(Items.PLAYER_HEAD);
    }

    private void renderOwnerPage(GuiGraphics graphics, int mouseX, int mouseY) {
        drawText(graphics, Component.translatable("qshop_sellbox.tab.owner"),
                screenX(SellBoxLayoutDebug.Widget.OWNER_TITLE, 8),
                screenY(SellBoxLayoutDebug.Widget.OWNER_TITLE, 6), TEXT_COLOR);

        drawAvatar(graphics,
                screenX(SellBoxLayoutDebug.Widget.OWNER_AVATAR, 8),
                screenY(SellBoxLayoutDebug.Widget.OWNER_AVATAR, 20), menu.owner());
        String name = menu.owner() == null || menu.ownerName().isBlank()
                ? Component.translatable("qshop_sellbox.owner.none").getString() : menu.ownerName();
        String displayName = trim(name, 13);
        int nameX = screenX(SellBoxLayoutDebug.Widget.OWNER_INFO, 34);
        int nameY = screenY(SellBoxLayoutDebug.Widget.OWNER_INFO, 24);
        drawText(graphics, displayName, nameX, nameY, TEXT_COLOR);
        boolean online = menu.owner() != null && Minecraft.getInstance().getConnection() != null
                && Minecraft.getInstance().getConnection().getPlayerInfo(menu.owner()) != null;
        String status = Component.translatable(online
                ? "qshop_sellbox.owner.online" : "qshop_sellbox.owner.offline").getString();
        int statusX = Math.min(nameX + font.width(displayName) + 5, leftPos + 116);
        drawText(graphics, status, statusX, nameY,
                online ? ONLINE_COLOR : OFFLINE_COLOR);

        int ownerButtonX = screenX(SellBoxLayoutDebug.Widget.OWNER_BUTTON, 8);
        int ownerButtonY = screenY(SellBoxLayoutDebug.Widget.OWNER_BUTTON, 45);
        Component ownerLabel = Component.translatable("qshop_sellbox.owner.claim");
        int ownerButtonWidth = buttonWidth(ownerLabel, BUTTON_MIN_WIDTH, imageWidth - 16);
        drawButton(graphics, ownerButtonX, ownerButtonY,
                ownerLabel, ownerButtonWidth,
                inside(mouseX, mouseY, ownerButtonX, ownerButtonY,
                        ownerButtonWidth, BUTTON_HEIGHT));

        drawText(graphics, Component.translatable("qshop_sellbox.setting.mode"),
                screenX(SellBoxLayoutDebug.Widget.MODE_LABEL, 8),
                screenY(SellBoxLayoutDebug.Widget.MODE_LABEL, 69), TEXT_COLOR);
        int modeY = screenY(SellBoxLayoutDebug.Widget.MODE_INTERVAL, 79);
        Component intervalLabel = Component.translatable("qshop_sellbox.mode.interval");
        Component closedGuiLabel = Component.translatable("qshop_sellbox.mode.closed_gui");
        int[] modeWidths = modeButtonWidths(intervalLabel, closedGuiLabel);
        int firstModeX = screenX(SellBoxLayoutDebug.Widget.MODE_INTERVAL, 8);
        drawSegment(graphics, firstModeX, modeY,
                intervalLabel, modeWidths[0],
                menu.sellMode() == SellMode.INTERVAL,
                inside(mouseX, mouseY, firstModeX, modeY, modeWidths[0], 16));
        int secondModeX = screenX(SellBoxLayoutDebug.Widget.MODE_CLOSED_GUI,
                8 + modeWidths[0] + 4);
        int secondModeY = screenY(SellBoxLayoutDebug.Widget.MODE_CLOSED_GUI, 79);
        drawSegment(graphics, secondModeX, secondModeY,
                closedGuiLabel, modeWidths[1],
                menu.sellMode() == SellMode.CLOSED_GUI,
                inside(mouseX, mouseY, secondModeX, secondModeY, modeWidths[1], 16));

        int intervalLabelX = screenX(SellBoxLayoutDebug.Widget.INTERVAL_LABEL, 8);
        int intervalLabelY = screenY(SellBoxLayoutDebug.Widget.INTERVAL_LABEL, 103);
        drawText(graphics, Component.translatable("qshop_sellbox.setting.interval"),
                intervalLabelX, intervalLabelY, TEXT_COLOR);
        int inputX = screenX(SellBoxLayoutDebug.Widget.INTERVAL_INPUT, 8);
        int inputY = screenY(SellBoxLayoutDebug.Widget.INTERVAL_INPUT, 122);
        SellBoxTextures.input(graphics, inputX, inputY,
                intervalInput != null && intervalInput.isFocused());
        Component unitLabel = Component.translatable(intervalUnit.translationKey);
        int unitButtonWidth = buttonWidth(unitLabel, BUTTON_MIN_WIDTH, imageWidth - 112);
        int unitButtonX = screenX(SellBoxLayoutDebug.Widget.INTERVAL_UNIT, 104);
        int unitButtonY = screenY(SellBoxLayoutDebug.Widget.INTERVAL_UNIT, 120);
        SellBoxTextures.button(graphics, unitButtonX, unitButtonY, unitButtonWidth, BUTTON_HEIGHT,
                inside(mouseX, mouseY, unitButtonX, unitButtonY, unitButtonWidth, BUTTON_HEIGHT), true);
        drawButtonLabel(graphics, unitLabel, unitButtonX, unitButtonY,
                unitButtonWidth, BUTTON_HEIGHT);

        drawNotificationRow(graphics, mouseX, mouseY, SellBoxLayoutDebug.Widget.ACTION_BAR_NOTIFICATION,
                Component.translatable("qshop_sellbox.setting.action_bar"),
                menu.showActionBarNotification());
        drawNotificationRow(graphics, mouseX, mouseY, SellBoxLayoutDebug.Widget.CHAT_NOTIFICATION,
                Component.translatable("qshop_sellbox.setting.chat"),
                menu.showChatNotification());
    }

    private void drawNotificationRow(GuiGraphics graphics, int mouseX, int mouseY,
                                     SellBoxLayoutDebug.Widget widget, Component label, boolean checked) {
        int x = screenX(widget, 8);
        int y = screenY(widget, widget == SellBoxLayoutDebug.Widget.ACTION_BAR_NOTIFICATION ? 141 : 153);
        boolean hovered = inside(mouseX, mouseY, x, y, imageWidth - 16, 12);
        SellBoxTextures.checkbox(graphics, x, y, checked, hovered);
        drawText(graphics, label, x + 16, y + 2, TEXT_COLOR);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, Component label,
                            int width, boolean hovered) {
        SellBoxTextures.button(graphics, x, y, width, BUTTON_HEIGHT, hovered, true);
        drawButtonLabel(graphics, label, x, y, width, BUTTON_HEIGHT);
    }

    private void drawSegment(GuiGraphics graphics, int x, int y, Component label,
                             int width, boolean selected, boolean hovered) {
        SellBoxTextures.button(graphics, x, y, width, BUTTON_HEIGHT, hovered, selected);
        drawButtonLabel(graphics, label, x, y, width, BUTTON_HEIGHT);
    }

    private void drawButtonLabel(GuiGraphics graphics, Component label,
                                 int x, int y, int width, int height) {
        int maxTextWidth = Math.max(1, width - 8);
        String text = font.plainSubstrByWidth(label.getString(), maxTextWidth);
        drawCenteredText(graphics, text, x + width / 2,
                y + Math.max(0, (height - font.lineHeight) / 2), TEXT_COLOR);
    }

    private int buttonWidth(Component label, int minimum, int maximum) {
        return Mth.clamp(font.width(label) + 12, minimum, maximum);
    }

    private int[] modeButtonWidths(Component first, Component second) {
        int available = imageWidth - 16;
        int firstWidth = buttonWidth(first, BUTTON_MIN_WIDTH, available);
        int secondWidth = buttonWidth(second, BUTTON_MIN_WIDTH, available);
        if (firstWidth + 4 + secondWidth <= available) {
            return new int[]{firstWidth, secondWidth};
        }

        int firstLimit = Math.max(BUTTON_MIN_WIDTH, (available - 4) / 2);
        firstWidth = Math.min(firstWidth, firstLimit);
        secondWidth = available - 4 - firstWidth;
        return new int[]{firstWidth, Math.max(BUTTON_MIN_WIDTH, secondWidth)};
    }

    private void drawSmallButton(GuiGraphics graphics, int x, int y, String label, boolean hovered) {
        SellBoxTextures.smallButton(graphics, x, y, hovered);
        drawCenteredText(graphics, label, x + 10, y + 5, TEXT_COLOR);
    }

    private void drawAvatar(GuiGraphics graphics, int x, int y, UUID owner) {
        ResourceLocation skin = ResourceLocation.fromNamespaceAndPath(
                "minecraft", "textures/entity/steve.png");
        if (owner != null && Minecraft.getInstance().getConnection() != null) {
            PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(owner);
            if (info != null) skin = info.getSkinLocation();
        }
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, skin);
        graphics.blit(skin, x, y, 20, 20, 8, 8, 8, 8, 64, 64);
        graphics.blit(skin, x, y, 20, 20, 40, 8, 8, 8, 64, 64);
    }

    private void sendSettings(SellMode mode, int intervalTicks) {
        int normalized = Math.max(20, Math.min(intervalTicks, SellBoxBlockEntity.MAX_INTERVAL_TICKS));
        menu.setSettingsData(mode, normalized, menu.showActionBarNotification(),
                menu.showChatNotification());
        SellBoxNetwork.sendSettings(menu.pos(), mode, normalized,
                menu.showActionBarNotification(), menu.showChatNotification());
    }

    private int readIntervalTicks() {
        if (intervalInput == null) return menu.saleIntervalTicks();
        String value = intervalInput.getValue().trim();
        long amount;
        try {
            amount = value.isEmpty() ? 1L : Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            amount = 1L;
        }
        return (int) Math.min(intervalUnit.toTicks(amount), SellBoxBlockEntity.MAX_INTERVAL_TICKS);
    }

    private void commitSettings() {
        if (settingsCommitted) return;
        settingsCommitted = true;
        sendSettings(menu.sellMode(), readIntervalTicks());
    }

    private void updateIntervalInputFromTicks(int ticks) {
        if (intervalInput != null && !intervalInput.isFocused()) {
            intervalInput.setValue(Long.toString(intervalUnit.fromTicks(ticks)));
        }
    }

    public void refreshIntervalInput() {
        updateIntervalInputFromTicks(menu.saleIntervalTicks());
    }

    private void cycleIntervalUnit() {
        intervalUnit = intervalUnit.next();
        if (intervalInput != null) intervalInput.setFocused(false);
    }

    private void setTab(int nextTab) {
        tab = nextTab;
        SellBoxEmiCompat.setSettingsSuppressed(nextTab == 1);
        SellBoxLayoutDebug.ensureSelected(tab);
        if (intervalInput != null) {
            intervalInput.setVisible(nextTab == 1);
            if (nextTab != 1) intervalInput.setFocused(false);
        }
    }

    private int layoutX(SellBoxLayoutDebug.Widget widget, int normalX) {
        return SellBoxLayoutDebug.x(widget, normalX);
    }

    private int layoutY(SellBoxLayoutDebug.Widget widget, int normalY) {
        return SellBoxLayoutDebug.y(widget, normalY);
    }

    private int screenX(SellBoxLayoutDebug.Widget widget, int normalX) {
        return leftPos + layoutX(widget, normalX);
    }

    private int screenY(SellBoxLayoutDebug.Widget widget, int normalY) {
        return topPos + layoutY(widget, normalY);
    }

    private int tabX(int page) {
        SellBoxLayoutDebug.Widget widget = page == 0
                ? SellBoxLayoutDebug.Widget.TAB_ITEMS : SellBoxLayoutDebug.Widget.TAB_OWNER;
        return screenX(widget, page * 27);
    }

    private int tabY(int page) {
        SellBoxLayoutDebug.Widget widget = page == 0
                ? SellBoxLayoutDebug.Widget.TAB_ITEMS : SellBoxLayoutDebug.Widget.TAB_OWNER;
        return screenY(widget, -28);
    }

    private void syncIntervalInputPosition() {
        if (intervalInput == null) return;
        intervalInput.setX(screenX(SellBoxLayoutDebug.Widget.INTERVAL_INPUT, 10));
        intervalInput.setY(screenY(SellBoxLayoutDebug.Widget.INTERVAL_INPUT, 123));
    }

    private void renderDebugOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!SellBoxLayoutDebug.isEnabled()) return;
        DebugBounds bounds = debugBounds(SellBoxLayoutDebug.selected());
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 900);
        SellBoxLayoutDebug.renderOverlay(graphics, font, bounds.x(), bounds.y(),
                bounds.width(), bounds.height());
        graphics.flush();
        graphics.pose().popPose();
    }

    private DebugBounds debugBounds(SellBoxLayoutDebug.Widget widget) {
        return switch (widget) {
            case ITEM_TITLE -> new DebugBounds(leftPos + layoutX(widget, 8),
                    topPos + layoutY(widget, 6), font.width(title), font.lineHeight);
            case ITEM_INVENTORY -> new DebugBounds(leftPos + layoutX(widget, 8),
                    topPos + layoutY(widget, inventoryLabelY - 1),
                    font.width(Component.translatable("container.inventory")), font.lineHeight);
            case TAB_ITEMS, TAB_OWNER -> {
                int page = widget == SellBoxLayoutDebug.Widget.TAB_ITEMS ? 0 : 1;
                yield new DebugBounds(tabX(page), tabY(page), 26, 32);
            }
            case OWNER_TITLE -> new DebugBounds(screenX(widget, 8), screenY(widget, 6),
                    font.width(Component.translatable("qshop_sellbox.tab.owner")), font.lineHeight);
            case OWNER_AVATAR -> new DebugBounds(screenX(widget, 8), screenY(widget, 20), 20, 20);
            case OWNER_INFO -> new DebugBounds(screenX(widget, 34), screenY(widget, 24), 120, font.lineHeight);
            case OWNER_BUTTON -> {
                Component label = Component.translatable("qshop_sellbox.owner.claim");
                yield new DebugBounds(screenX(widget, 8), screenY(widget, 45),
                        buttonWidth(label, BUTTON_MIN_WIDTH, imageWidth - 16), BUTTON_HEIGHT);
            }
            case MODE_LABEL -> new DebugBounds(screenX(widget, 8), screenY(widget, 69),
                    font.width(Component.translatable("qshop_sellbox.setting.mode")), font.lineHeight);
            case MODE_INTERVAL, MODE_CLOSED_GUI -> {
                Component first = Component.translatable("qshop_sellbox.mode.interval");
                Component second = Component.translatable("qshop_sellbox.mode.closed_gui");
                int[] widths = modeButtonWidths(first, second);
                int pageX = widget == SellBoxLayoutDebug.Widget.MODE_INTERVAL ? 8 : 8 + widths[0] + 4;
                yield new DebugBounds(screenX(widget, pageX), screenY(widget, 79),
                        widget == SellBoxLayoutDebug.Widget.MODE_INTERVAL ? widths[0] : widths[1], BUTTON_HEIGHT);
            }
            case INTERVAL_LABEL -> new DebugBounds(screenX(widget, 8), screenY(widget, 103),
                    font.width(Component.translatable("qshop_sellbox.setting.interval")), font.lineHeight);
            case INTERVAL_INPUT -> new DebugBounds(screenX(widget, 8), screenY(widget, 122), 96, 14);
            case INTERVAL_UNIT -> {
                Component unit = Component.translatable(intervalUnit.translationKey);
                yield new DebugBounds(screenX(widget, 104), screenY(widget, 120),
                        buttonWidth(unit, BUTTON_MIN_WIDTH, imageWidth - 112), BUTTON_HEIGHT);
            }
            case ACTION_BAR_NOTIFICATION, CHAT_NOTIFICATION -> {
                int normalY = widget == SellBoxLayoutDebug.Widget.ACTION_BAR_NOTIFICATION ? 141 : 153;
                yield new DebugBounds(screenX(widget, 8), screenY(widget, normalY), imageWidth - 16, 12);
            }
        };
    }

    private record DebugBounds(int x, int y, int width, int height) {}

    private void drawText(GuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, true);
    }

    private void drawText(GuiGraphics graphics, String text, int x, int y, int color) {
        drawText(graphics, Component.literal(text), x, y, color);
    }

    private void drawCenteredText(GuiGraphics graphics, Component text, int x, int y, int color) {
        int halfWidth = font.width(text) / 2;
        graphics.drawString(font, text, x - halfWidth, y, color, true);
    }

    private void drawCenteredText(GuiGraphics graphics, String text, int x, int y, int color) {
        drawCenteredText(graphics, Component.literal(text), x, y, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        syncIntervalInputPosition();
        if (button != 0) {
            // Keep vanilla slot handling, including middle-click clone, on the item page.
            return tab == 0 ? super.mouseClicked(mouseX, mouseY, button) : true;
        }
        if (inside(mouseX, mouseY, tabX(0), tabY(0), 26, 32)) {
            setTab(0);
            return true;
        }
        if (inside(mouseX, mouseY, tabX(1), tabY(1), 26, 32)) {
            setTab(1);
            return true;
        }
        if (tab == 1) {
            Component claimLabel = Component.translatable("qshop_sellbox.owner.claim");
            int claimWidth = buttonWidth(claimLabel, BUTTON_MIN_WIDTH, imageWidth - 16);
            int claimX = screenX(SellBoxLayoutDebug.Widget.OWNER_BUTTON, 8);
            int claimY = screenY(SellBoxLayoutDebug.Widget.OWNER_BUTTON, 45);
            if (inside(mouseX, mouseY, claimX, claimY, claimWidth, BUTTON_HEIGHT)) {
                SellBoxNetwork.sendClaimOwner(menu.pos());
                return true;
            }
            Component unitLabel = Component.translatable(intervalUnit.translationKey);
            int unitButtonWidth = buttonWidth(unitLabel, BUTTON_MIN_WIDTH, imageWidth - 112);
            int unitButtonX = screenX(SellBoxLayoutDebug.Widget.INTERVAL_UNIT, 104);
            int unitButtonY = screenY(SellBoxLayoutDebug.Widget.INTERVAL_UNIT, 120);
            if (inside(mouseX, mouseY, unitButtonX, unitButtonY, unitButtonWidth, BUTTON_HEIGHT)) {
                cycleIntervalUnit();
                return true;
            }
            if (intervalInput != null && intervalInput.isFocused()
                    && !inside(mouseX, mouseY,
                    screenX(SellBoxLayoutDebug.Widget.INTERVAL_INPUT, 8),
                    screenY(SellBoxLayoutDebug.Widget.INTERVAL_INPUT, 122), 96, 12)) {
                intervalInput.setFocused(false);
            }
            Component intervalLabel = Component.translatable("qshop_sellbox.mode.interval");
            Component closedGuiLabel = Component.translatable("qshop_sellbox.mode.closed_gui");
            int[] modeWidths = modeButtonWidths(intervalLabel, closedGuiLabel);
            int firstModeX = screenX(SellBoxLayoutDebug.Widget.MODE_INTERVAL, 8);
            int firstModeY = screenY(SellBoxLayoutDebug.Widget.MODE_INTERVAL, 79);
            if (inside(mouseX, mouseY, firstModeX, firstModeY, modeWidths[0], 16)) {
                menu.setSettingsData(SellMode.INTERVAL, menu.saleIntervalTicks(),
                        menu.showActionBarNotification(), menu.showChatNotification());
                return true;
            }
            int secondModeX = screenX(SellBoxLayoutDebug.Widget.MODE_CLOSED_GUI,
                    8 + modeWidths[0] + 4);
            int secondModeY = screenY(SellBoxLayoutDebug.Widget.MODE_CLOSED_GUI, 79);
            if (inside(mouseX, mouseY, secondModeX, secondModeY, modeWidths[1], 16)) {
                menu.setSettingsData(SellMode.CLOSED_GUI, menu.saleIntervalTicks(),
                        menu.showActionBarNotification(), menu.showChatNotification());
                return true;
            }
            if (inside(mouseX, mouseY,
                    screenX(SellBoxLayoutDebug.Widget.ACTION_BAR_NOTIFICATION, 8),
                    screenY(SellBoxLayoutDebug.Widget.ACTION_BAR_NOTIFICATION, 141),
                    imageWidth - 16, 12)) {
                menu.setSettingsData(menu.sellMode(), menu.saleIntervalTicks(),
                        !menu.showActionBarNotification(), menu.showChatNotification());
                return true;
            }
            if (inside(mouseX, mouseY,
                    screenX(SellBoxLayoutDebug.Widget.CHAT_NOTIFICATION, 8),
                    screenY(SellBoxLayoutDebug.Widget.CHAT_NOTIFICATION, 153),
                    imageWidth - 16, 12)) {
                menu.setSettingsData(menu.sellMode(), menu.saleIntervalTicks(),
                        menu.showActionBarNotification(), !menu.showChatNotification());
                return true;
            }
            if (intervalInput != null && intervalInput.mouseClicked(mouseX, mouseY, button)) {
                intervalInput.setFocused(true);
                return true;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F8) {
            if (!SellBoxLayoutDebug.isConfiguredEnabled()) return super.keyPressed(keyCode, scanCode, modifiers);
            SellBoxLayoutDebug.toggle();
            SellBoxLayoutDebug.ensureSelected(tab);
            return true;
        }
        if (SellBoxLayoutDebug.isEnabled()) {
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                SellBoxLayoutDebug.selectNext(tab, hasShiftDown());
                return true;
            }
            int dx = 0;
            int dy = 0;
            if (keyCode == GLFW.GLFW_KEY_LEFT) dx = -1;
            if (keyCode == GLFW.GLFW_KEY_RIGHT) dx = 1;
            if (keyCode == GLFW.GLFW_KEY_UP) dy = -1;
            if (keyCode == GLFW.GLFW_KEY_DOWN) dy = 1;
            if (dx != 0 || dy != 0) {
                int step = hasAltDown() ? 1 : 5;
                SellBoxLayoutDebug.moveSelected(tab,
                        dx * step, dy * step);
                syncIntervalInputPosition();
                return true;
            }
        }
        if (tab == 1 && intervalInput != null && intervalInput.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                intervalInput.setFocused(false);
                return true;
            }
            if (intervalInput.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        commitSettings();
        SellBoxEmiCompat.restore();
        super.removed();
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (tab == 1 && intervalInput != null && intervalInput.isFocused()
                && intervalInput.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 1)) + "...";
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void flushAll(GuiGraphics graphics) {
        graphics.flush();
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
    }
}
