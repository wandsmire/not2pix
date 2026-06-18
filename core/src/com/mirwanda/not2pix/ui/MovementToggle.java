package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mirwanda.not2pix.Not2Pix;

/**
 * Bottom-center toggle for movement mode, with zoom+ and zoom- buttons on each side.
 * Layout: [Zoom-] [Move Toggle] [Zoom+]
 */
public class MovementToggle extends UIElement {

    private Not2Pix app;
    private float dp;
    public boolean movementMode = false; // true = 1-finger pan, longpress draw, button zoom

    private float btnSize;
    private float toggleW;
    private float zoomMinusX, toggleX, zoomPlusX, btnY;

    public MovementToggle(Not2Pix app, float screenWidth, float screenHeight, float dp) {
        this.app = app;
        this.dp = dp;
        this.btnSize = 40 * dp;
        this.toggleW = 52 * dp;

        float totalW = btnSize + 4 * dp + toggleW + 4 * dp + btnSize;
        this.width = totalW;
        this.height = btnSize + 8 * dp;
        this.x = (screenWidth - totalW) / 2f;
        this.y = 4 * dp; // bottom center

        zoomMinusX = this.x;
        toggleX = zoomMinusX + btnSize + 4 * dp;
        zoomPlusX = toggleX + toggleW + 4 * dp;
        btnY = this.y + 4 * dp;
    }

    @Override
    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!visible) return;
        float lw = Math.max(2f, 1.5f * dp);

        // Zoom- button
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.22f, 0.22f, 0.22f, 0.95f);
        sr.rect(zoomMinusX, btnY, btnSize, btnSize);
        sr.setColor(Color.WHITE);
        sr.rect(zoomMinusX + btnSize * 0.25f, btnY + btnSize * 0.45f, btnSize * 0.5f, lw * 1.5f);
        sr.end();

        // Movement toggle button
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(movementMode ? new Color(0.3f, 0.6f, 0.3f, 1) : new Color(0.4f, 0.25f, 0.25f, 1));
        sr.rect(toggleX, btnY, toggleW, btnSize);
        // Draw move icon (4 arrows)
        sr.setColor(Color.WHITE);
        float cx = toggleX + toggleW / 2f;
        float cy = btnY + btnSize / 2f;
        float arrowLen = btnSize * 0.25f;
        // Up arrow
        sr.rectLine(cx, cy, cx, cy + arrowLen, lw);
        sr.triangle(cx - lw * 2, cy + arrowLen, cx + lw * 2, cy + arrowLen, cx, cy + arrowLen + lw * 2);
        // Down arrow
        sr.rectLine(cx, cy, cx, cy - arrowLen, lw);
        sr.triangle(cx - lw * 2, cy - arrowLen, cx + lw * 2, cy - arrowLen, cx, cy - arrowLen - lw * 2);
        // Right arrow
        sr.rectLine(cx, cy, cx + arrowLen, cy, lw);
        sr.triangle(cx + arrowLen, cy - lw * 2, cx + arrowLen, cy + lw * 2, cx + arrowLen + lw * 2, cy);
        // Left arrow
        sr.rectLine(cx, cy, cx - arrowLen, cy, lw);
        sr.triangle(cx - arrowLen, cy - lw * 2, cx - arrowLen, cy + lw * 2, cx - arrowLen - lw * 2, cy);
        sr.end();

        // Zoom+ button
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.22f, 0.22f, 0.22f, 0.95f);
        sr.rect(zoomPlusX, btnY, btnSize, btnSize);
        sr.setColor(Color.WHITE);
        sr.rect(zoomPlusX + btnSize * 0.25f, btnY + btnSize * 0.45f, btnSize * 0.5f, lw * 1.5f);
        sr.rect(zoomPlusX + btnSize * 0.45f, btnY + btnSize * 0.25f, lw * 1.5f, btnSize * 0.5f);
        sr.end();

        // Borders
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.4f, 0.4f, 0.4f, 1);
        sr.rect(zoomMinusX, btnY, btnSize, btnSize);
        sr.rect(toggleX, btnY, toggleW, btnSize);
        sr.rect(zoomPlusX, btnY, btnSize, btnSize);
        sr.end();
    }

    public boolean handleTouch(float touchX, float touchY) {
        // Zoom-
        if (touchX >= zoomMinusX && touchX <= zoomMinusX + btnSize &&
            touchY >= btnY && touchY <= btnY + btnSize) {
            app.zoomOut();
            return true;
        }
        // Toggle
        if (touchX >= toggleX && touchX <= toggleX + toggleW &&
            touchY >= btnY && touchY <= btnY + btnSize) {
            movementMode = !movementMode;
            return true;
        }
        // Zoom+
        if (touchX >= zoomPlusX && touchX <= zoomPlusX + btnSize &&
            touchY >= btnY && touchY <= btnY + btnSize) {
            app.zoomIn();
            return true;
        }
        return false;
    }

    @Override
    public boolean hit(float touchX, float touchY) {
        return visible && touchX >= x && touchX <= x + width && touchY >= y && touchY <= y + height;
    }
}
