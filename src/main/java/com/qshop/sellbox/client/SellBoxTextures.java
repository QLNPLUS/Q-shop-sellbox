package com.qshop.sellbox.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class SellBoxTextures {
    public static final ResourceLocation BACKGROUND = texture("background.png");
    public static final ResourceLocation OWNER_BACKGROUND = texture("owner_background.png");
    public static final ResourceLocation SLOT = texture("slot.png");
    public static final ResourceLocation TABS = texture("tabs.png");
    public static final ResourceLocation BUTTON = texture("button.png");
    public static final ResourceLocation BUTTON_DISABLED = texture("button_disabled.png");
    public static final ResourceLocation BUTTON_HOVER = texture("button_hover.png");
    public static final ResourceLocation SEGMENT = texture("segment.png");
    public static final ResourceLocation INPUT = texture("input.png");
    public static final ResourceLocation INPUT_FOCUS = texture("input_focus.png");
    public static final ResourceLocation SMALL_BUTTON = texture("small_button.png");
    public static final ResourceLocation DROPDOWN = texture("dropdown.png");
    public static final ResourceLocation DROPDOWN_HOVER = texture("dropdown_hover.png");
    public static final ResourceLocation CHECKBOX_ON = texture("checkbox_on.png");
    public static final ResourceLocation CHECKBOX_ON_HOVER = texture("checkbox_on_hover.png");
    public static final ResourceLocation CHECKBOX_OFF = texture("checkbox_off.png");
    public static final ResourceLocation CHECKBOX_OFF_HOVER = texture("checkbox_off_hover.png");

    private SellBoxTextures() {}

    public static void background(GuiGraphics graphics, int x, int y) {
        graphics.blit(BACKGROUND, x, y, 0, 0, 176, 166, 176, 166);
    }

    public static void ownerBackground(GuiGraphics graphics, int x, int y) {
        graphics.blit(OWNER_BACKGROUND, x, y, 0, 0, 176, 166, 176, 166);
    }

    public static void slot(GuiGraphics graphics, int x, int y) {
        graphics.blit(SLOT, x, y, 0, 0, 18, 18, 18, 18);
    }

    public static void tab(GuiGraphics graphics, int x, int y, boolean selected) {
        tab(graphics, x, y, 0, selected);
    }

    public static void tab(GuiGraphics graphics, int x, int y, int column, boolean selected) {
        int sourceX = Math.max(0, Math.min(column, 6)) * 26;
        int sourceY = selected ? 32 : 0;
        graphics.blit(TABS, x, y, sourceX, sourceY, 26, 32, 182, 128);
    }

    public static void button(GuiGraphics graphics, int x, int y, boolean hovered) {
        button(graphics, x, y, 160, 20, hovered, true);
    }

    public static void button(GuiGraphics graphics, int x, int y, int width, int height,
                              boolean hovered, boolean enabled) {
        ResourceLocation state = hovered ? BUTTON_HOVER : !enabled ? BUTTON_DISABLED : BUTTON;
        blitNineSlice(graphics, state, x, y, width, height, 60, 16, 4);
    }

    private static void blitNineSlice(GuiGraphics graphics, ResourceLocation texture,
                                      int x, int y, int width, int height,
                                      int textureWidth, int textureHeight, int border) {
        int innerTextureWidth = textureWidth - border * 2;
        int innerTextureHeight = textureHeight - border * 2;
        int innerWidth = width - border * 2;
        int innerHeight = height - border * 2;

        graphics.blit(texture, x, y, 0, 0, border, border, textureWidth, textureHeight);
        graphics.blit(texture, x + width - border, y, textureWidth - border, 0,
                border, border, textureWidth, textureHeight);
        graphics.blit(texture, x, y + height - border, 0, textureHeight - border,
                border, border, textureWidth, textureHeight);
        graphics.blit(texture, x + width - border, y + height - border,
                textureWidth - border, textureHeight - border, border, border,
                textureWidth, textureHeight);

        if (innerWidth > 0) {
            graphics.blit(texture, x + border, y, innerWidth, border,
                    border, 0, innerTextureWidth, border, textureWidth, textureHeight);
            graphics.blit(texture, x + border, y + height - border, innerWidth, border,
                    border, textureHeight - border, innerTextureWidth, border,
                    textureWidth, textureHeight);
        }
        if (innerHeight > 0) {
            graphics.blit(texture, x, y + border, border, innerHeight,
                    0, border, border, innerTextureHeight, textureWidth, textureHeight);
            graphics.blit(texture, x + width - border, y + border, border, innerHeight,
                    textureWidth - border, border, border, innerTextureHeight,
                    textureWidth, textureHeight);
        }
        if (innerWidth > 0 && innerHeight > 0) {
            graphics.blit(texture, x + border, y + border, innerWidth, innerHeight,
                    border, border, innerTextureWidth, innerTextureHeight,
                    textureWidth, textureHeight);
        }
    }

    public static void input(GuiGraphics graphics, int x, int y, boolean focused) {
        graphics.blit(focused ? INPUT_FOCUS : INPUT, x, y, 0, 0, 96, 12, 96, 12);
    }

    public static void smallButton(GuiGraphics graphics, int x, int y, boolean hovered) {
        graphics.blit(SMALL_BUTTON, x, y, 0, hovered ? 20 : 0, 20, 20, 20, 60);
    }

    public static void dropdown(GuiGraphics graphics, int x, int y) {
        graphics.blit(DROPDOWN, x, y, 0, 0, 160, 94, 160, 94);
    }

    public static void dropdownHover(GuiGraphics graphics, int x, int y) {
        graphics.blit(DROPDOWN_HOVER, x, y, 0, 0, 160, 18, 160, 18);
    }

    public static void checkbox(GuiGraphics graphics, int x, int y,
                                boolean checked, boolean hovered) {
        ResourceLocation texture = checked
                ? (hovered ? CHECKBOX_ON_HOVER : CHECKBOX_ON)
                : (hovered ? CHECKBOX_OFF_HOVER : CHECKBOX_OFF);
        graphics.blit(texture, x, y, 0, 0, 12, 12, 12, 12);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("qshop_sellbox", "textures/gui/" + name);
    }
}
