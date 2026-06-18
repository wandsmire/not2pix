package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mirwanda.not2pix.Not2Pix;
import com.mirwanda.not2pix.Palette;

/**
 * Vertical palette bar on the right side, bottom-aligned, single column of 18 swatches.
 * Tap = select color, double-tap = open HSV picker, longpress = color picker tool.
 */
public class PaletteBar extends UIPanel {

    private Not2Pix app;
    private float dp;
    private float swatchSize;
    private static final int MAX_SWATCHES = 18;
    public Runnable onPickerOpen;
    public Runnable onColorPickerTool;

    // Double-tap / longpress detection
    private float lastTapTime = 0;
    private int lastTapIndex = -1;
    private float touchDownTime = 0;
    private float touchDownY = 0;
    private boolean longPressTriggered = false;
    private boolean needsLongPressCheck = false;

    private static final float DOUBLE_TAP_TIME = 0.35f;
    private static final float LONG_PRESS_TIME = 0.5f;

    public PaletteBar(Not2Pix app, float screenWidth, float screenHeight, float dp) {
        super(0, 0, 48 * dp, 0);
        this.app = app;
        this.dp = dp;
        this.swatchSize = 36 * dp;

        this.width = swatchSize + 6 * dp;
        this.x = screenWidth - this.width;
        this.height = MAX_SWATCHES * (swatchSize + 2 * dp) + 2 * dp;
        this.y = 0; // bottom-aligned
    }

    @Override
    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!visible) return;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(bgColor);
        sr.rect(x, y, width, height);
        sr.end();

        Palette pal = app.palette;
        float pad = (width - swatchSize) / 2f;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        int count = Math.min(MAX_SWATCHES, pal.colors.size());
        for (int i = 0; i < count; i++) {
            float sy = y + 2 * dp + i * (swatchSize + 2 * dp);
            sr.setColor(pal.colors.get(i));
            sr.rect(x + pad, sy, swatchSize, swatchSize);
        }
        sr.end();

        if (pal.selectedIndex >= 0 && pal.selectedIndex < count) {
            float sy = y + 2 * dp + pal.selectedIndex * (swatchSize + 2 * dp);
            sr.begin(ShapeRenderer.ShapeType.Line);
            sr.setColor(Color.WHITE);
            sr.rect(x + pad - 1, sy - 1, swatchSize + 2, swatchSize + 2);
            sr.end();
        }

        checkLongPress();
    }

    private void checkLongPress() {
        if (!needsLongPressCheck || longPressTriggered) return;
        if (!Gdx.input.isTouched(0)) {
            needsLongPressCheck = false;
            return;
        }
        float elapsed = (System.nanoTime() / 1e9f) - touchDownTime;
        float dy = Math.abs(Gdx.input.getY(0) - touchDownY);
        if (elapsed >= LONG_PRESS_TIME && dy < 12 * dp) {
            longPressTriggered = true;
            needsLongPressCheck = false;
            if (onColorPickerTool != null) onColorPickerTool.run();
        }
    }

    public boolean handleTouch(float touchX, float touchY) {
        if (!hit(touchX, touchY)) return false;

        Palette pal = app.palette;
        int count = Math.min(MAX_SWATCHES, pal.colors.size());
        int vi = (int) ((touchY - y - 2 * dp) / (swatchSize + 2 * dp));
        if (vi < 0 || vi >= count) return true;

        float now = System.nanoTime() / 1e9f;

        if (vi == lastTapIndex && (now - lastTapTime) < DOUBLE_TAP_TIME) {
            pal.selectedIndex = vi;
            if (onPickerOpen != null) onPickerOpen.run();
            lastTapIndex = -1;
            needsLongPressCheck = false;
            return true;
        }

        touchDownTime = now;
        touchDownY = Gdx.input.getY(0);
        longPressTriggered = false;
        needsLongPressCheck = true;

        lastTapTime = now;
        lastTapIndex = vi;
        pal.selectedIndex = vi;

        return true;
    }

    public void touchReleased() {
        needsLongPressCheck = false;
    }
}
