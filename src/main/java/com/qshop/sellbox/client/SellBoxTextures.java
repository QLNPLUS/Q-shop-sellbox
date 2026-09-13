package com.qshop.sellbox.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class SellBoxTextures {
    public static final Identifier BACKGROUND = texture("background.png");
    public static final Identifier OWNER_BACKGROUND = texture("owner_background.png");
    public static final Identifier SLOT = texture("slot.png");
    public static final Identifier TABS = texture("tabs.png");
    public static final Identifier BUTTON = texture("button.png");
    public static final Identifier BUTTON_DISABLED = texture("button_disabled.png");
    public static final Identifier BUTTON_HOVER = texture("button_hover.png");
    public static final Identifier SEGMENT = texture("segment.png");
    public static final Identifier INPUT = texture("input.png");
    public static final Identifier INPUT_FOCUS = texture("input_focus.png");
    public static final Identifier SMALL_BUTTON = texture("small_button.png");
    public static final Identifier DROPDOWN = texture("dropdown.png");
    public static final Identifier DROPDOWN_HOVER = texture("dropdown_hover.png");
    public static final Identifier CHECKBOX_ON = texture("checkbox_on.png");
    public static final Identifier CHECKBOX_ON_HOVER = texture("checkbox_on_hover.png");
    public static final Identifier CHECKBOX_OFF = texture("checkbox_off.png");
    public static final Identifier CHECKBOX_OFF_HOVER = texture("checkbox_off_hover.png");

    private SellBoxTextures() {}

    public static void background(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 0, 0, 176, 166, 176, 166);
    }

    public static void ownerBackground(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, OWNER_BACKGROUND, x, y, 0, 0, 176, 166, 176, 166);
    }

    public static void slot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT, x, y, 0, 0, 18, 18, 18, 18);
    }

    public static void tab(GuiGraphicsExtractor graphics, int x, int y, boolean selected) {
        tab(graphics, x, y, 0, selected);
    }

    public static void tab(GuiGraphicsExtractor graphics, int x, int y, int column, boolean selected) {
        int sourceX = Math.max(0, Math.min(column, 6)) * 26;
        int sourceY = selected ? 32 : 0;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TABS, x, y, sourceX, sourceY, 26, 32, 182, 128);
    }

    public static void button(GuiGraphicsExtractor graphics, int x, int y, boolean hovered) {
        button(graphics, x, y, 160, 20, hovered, true);
    }

    public static void button(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                              boolean hovered, boolean enabled) {
        Identifier state = hovered ? BUTTON_HOVER : !enabled ? BUTTON_DISABLED : BUTTON;
        blitNineSlice(graphics, state, x, y, width, height, 60, 16, 4);
    }

    private static void blitNineSlice(GuiGraphicsExtractor graphics, Identifier texture,
                                      int x, int y, int width, int height,
                                      int textureWidth, int textureHeight, int border) {
        int innerTextureWidth = textureWidth - border * 2;
        int innerTextureHeight = textureHeight - border * 2;
        int innerWidth = width - border * 2;
        int innerHeight = height - border * 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, border, border, textureWidth, textureHeight);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + width - border, y, textureWidth - border, 0,
                border, border, textureWidth, textureHeight);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y + height - border, 0, textureHeight - border,
                border, border, textureWidth, textureHeight);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + width - border, y + height - border,
                textureWidth - border, textureHeight - border, border, border,
                textureWidth, textureHeight);

        if (innerWidth > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + border, y, innerWidth, border,
                    border, 0, innerTextureWidth, border, textureWidth, textureHeight);
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + border, y + height - border, innerWidth, border,
                    border, textureHeight - border, innerTextureWidth, border,
                    textureWidth, textureHeight);
        }
        if (innerHeight > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y + border, border, innerHeight,
                    0, border, border, innerTextureHeight, textureWidth, textureHeight);
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + width - border, y + border, border, innerHeight,
                    textureWidth - border, border, border, innerTextureHeight,
                    textureWidth, textureHeight);
        }
        if (innerWidth > 0 && innerHeight > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + border, y + border, innerWidth, innerHeight,
                    border, border, innerTextureWidth, innerTextureHeight,
                    textureWidth, textureHeight);
        }
    }

    public static void input(GuiGraphicsExtractor graphics, int x, int y, boolean focused) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, focused ? INPUT_FOCUS : INPUT, x, y, 0, 0, 96, 12, 96, 12);
    }

    public static void smallButton(GuiGraphicsExtractor graphics, int x, int y, boolean hovered) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, SMALL_BUTTON, x, y, 0, hovered ? 20 : 0, 20, 20, 20, 60);
    }

    public static void dropdown(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, DROPDOWN, x, y, 0, 0, 160, 94, 160, 94);
    }

    public static void dropdownHover(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, DROPDOWN_HOVER, x, y, 0, 0, 160, 18, 160, 18);
    }

    public static void checkbox(GuiGraphicsExtractor graphics, int x, int y,
                                boolean checked, boolean hovered) {
        Identifier texture = checked
                ? (hovered ? CHECKBOX_ON_HOVER : CHECKBOX_ON)
                : (hovered ? CHECKBOX_OFF_HOVER : CHECKBOX_OFF);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, 12, 12, 12, 12);
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath("qshop_sellbox", "textures/gui/" + name);
    }
}
