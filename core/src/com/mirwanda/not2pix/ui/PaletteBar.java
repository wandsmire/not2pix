package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
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
    private static final int MAX_SWATCHES = 15;
    private static final int COLLAPSED_COUNT = 6;
    private boolean expanded = false;
    private float arrowAreaHeight;
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
        this(app, screenWidth, screenHeight, dp, 0);
    }

    public PaletteBar(Not2Pix app, float screenWidth, float screenHeight, float dp, float bottomOffset) {
        super(0, 0, 48 * dp, 0);
        this.app = app;
        this.dp = dp;
        this.swatchSize = 36 * dp;

        this.width = swatchSize + 6 * dp;
        this.x = screenWidth - this.width;
        this.arrowAreaHeight = 20 * dp;
        this.height = COLLAPSED_COUNT * (swatchSize + 2 * dp) + 2 * dp + arrowAreaHeight;
        this.y = bottomOffset;
    }

    @Override
    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!visible) return;

        int visibleCount = expanded ? MAX_SWATCHES : COLLAPSED_COUNT;
        this.height = visibleCount * (swatchSize + 2 * dp) + 2 * dp + arrowAreaHeight;

        float sh = Gdx.graphics.getHeight();
        float stripH = 40 * dp;
        float topLimit = sh - 96 * dp;
        float availableH = topLimit - stripH;
        float maxScrollY = availableH - this.height;
        float targetY = stripH + 8 * dp + app.colorPosition * maxScrollY;
        this.y = targetY;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(bgColor);
        sr.rect(x, y, width, height);
        sr.end();

        Palette pal = app.palette;
        float pad = (width - swatchSize) / 2f;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        int count = Math.min(visibleCount, pal.colors.size());
        for (int i = 0; i < count; i++) {
            Color color = pal.colors.get(i);
            float sy = y + 2 * dp + i * (swatchSize + 2 * dp);
            float sx = x + pad;
            
            // Draw a small 2x2 grid checkerboard inside each swatch
            float halfSize = swatchSize / 2f;
            sr.setColor(0.9f, 0.9f, 0.9f, 1f);
            sr.rect(sx, sy, halfSize, halfSize);
            sr.rect(sx + halfSize, sy + halfSize, halfSize, halfSize);
            sr.setColor(0.75f, 0.75f, 0.75f, 1f);
            sr.rect(sx + halfSize, sy, halfSize, halfSize);
            sr.rect(sx, sy + halfSize, halfSize, halfSize);
            
            // Draw color on top with blending
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            sr.setColor(color);
            sr.rect(sx, sy, swatchSize, swatchSize);
        }
        sr.end();

        if (pal.selectedIndex >= 0 && pal.selectedIndex < count) {
            float sy = y + 2 * dp + pal.selectedIndex * (swatchSize + 2 * dp);
            float sx = x + pad;
            // Task 8: Thicker cyan double-border for selection
            sr.begin(ShapeRenderer.ShapeType.Line);
            sr.setColor(Color.CYAN);
            sr.rect(sx - 2, sy - 2, swatchSize + 4, swatchSize + 4);
            sr.rect(sx - 1, sy - 1, swatchSize + 2, swatchSize + 2);
            sr.end();

            // Task 7: Crosshair overlay when color picker is active
            if (app.colorPickerActive) {
                float cx = sx + swatchSize / 2f;
                float cy = sy + swatchSize / 2f;
                float arm = swatchSize * 0.3f;
                sr.begin(ShapeRenderer.ShapeType.Line);
                sr.setColor(Color.WHITE);
                sr.line(cx - arm, cy, cx + arm, cy);
                sr.line(cx, cy - arm, cx, cy + arm);
                sr.circle(cx, cy, arm * 0.6f);
                sr.end();
            }
        }

        // Draw arrow indicator at the top of the palette bar
        float arrowCx = x + width / 2f;
        float arrowCy = y + height - arrowAreaHeight / 2f;
        float arrowSize = 6 * dp;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(Color.LIGHT_GRAY);
        if (expanded) {
            // Down arrow (tap to collapse)
            sr.triangle(arrowCx - arrowSize, arrowCy + arrowSize * 0.5f,
                        arrowCx + arrowSize, arrowCy + arrowSize * 0.5f,
                        arrowCx, arrowCy - arrowSize * 0.5f);
        } else {
            // Up arrow (tap to expand)
            sr.triangle(arrowCx - arrowSize, arrowCy - arrowSize * 0.5f,
                        arrowCx + arrowSize, arrowCy - arrowSize * 0.5f,
                        arrowCx, arrowCy + arrowSize * 0.5f);
        }
        sr.end();

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
            if (onColorPickerTool != null) {
                // Don't activate picker when selection tool has active selection
                if (app.tools[app.activeToolIndex] instanceof com.mirwanda.not2pix.SelectionTool) {
                    com.mirwanda.not2pix.SelectionTool sel = (com.mirwanda.not2pix.SelectionTool) app.tools[app.activeToolIndex];
                    if (sel.buffer != null) return;
                }
                onColorPickerTool.run();
            }
        }
    }

    public boolean handleTouch(float touchX, float touchY) {
        if (!hit(touchX, touchY)) return false;

        // Check arrow area at the top
        float arrowTop = y + height;
        float arrowBottom = y + height - arrowAreaHeight;
        if (touchY >= arrowBottom && touchY <= arrowTop) {
            expanded = !expanded;
            return true;
        }

        Palette pal = app.palette;
        int visibleCount = expanded ? MAX_SWATCHES : COLLAPSED_COUNT;
        int count = Math.min(visibleCount, pal.colors.size());
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
        // Deactivate color picker if it was active
        if (app.colorPickerActive) {
            app.colorPickerActive = false;
        }

        return true;
    }

    public void touchReleased() {
        needsLongPressCheck = false;
    }
}
