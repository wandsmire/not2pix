package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mirwanda.not2pix.Not2Pix;

/**
 * Bottom strip. Layout:
 * Left: [Fit to Width]
 * Center: [Move Toggle] [Zoom-] [Zoom+]
 * Right: [Undo] [Redo]
 * Zoom ratio label drawn above the strip, centered.
 */
public class BottomStrip extends UIPanel {

    private Not2Pix app;
    private float dp;
    public boolean movementMode = false;

    private float btnSize;
    private float toggleW;
    private float btnY;
    private float fitX, toggleX, zoomMinusX, zoomPlusX, undoX, redoX;

    public BottomStrip(Not2Pix app, float screenWidth, float screenHeight, float dp) {
        super(0, 0, screenWidth, 48 * dp);
        this.app = app;
        this.dp = dp;
        this.bgColor = new Color(0.12f, 0.12f, 0.12f, 0.95f);
        this.btnSize = 40 * dp;
        this.toggleW = 52 * dp;
        this.btnY = (height - btnSize) / 2f;

        float gap = 4 * dp;

        // Left: fit to width button
        fitX = 12 * dp;

        // Center group: toggle + zoom- + zoom+
        float centerW = toggleW + gap + btnSize + gap + btnSize;
        float centerX = (screenWidth - centerW) / 2f;
        toggleX = centerX;
        zoomMinusX = toggleX + toggleW + gap;
        zoomPlusX = zoomMinusX + btnSize + gap;

        // Right group: undo + redo
        float rightPad = 12 * dp;
        redoX = screenWidth - rightPad - btnSize;
        undoX = redoX - gap - btnSize;
    }

    @Override
    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!visible) return;
        float lw = Math.max(2f, 1.5f * dp);

        // Background strip
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(bgColor);
        sr.rect(x, y, width, height);
        sr.end();


        // Movement toggle (center group)
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(movementMode ? new Color(0.3f, 0.6f, 0.3f, 1) : new Color(0.4f, 0.25f, 0.25f, 1));
        sr.rect(toggleX, btnY, toggleW, btnSize);
        sr.setColor(Color.WHITE);
        float cx = toggleX + toggleW / 2f;
        float cy = btnY + btnSize / 2f;
        float arrowLen = btnSize * 0.25f;
        sr.rectLine(cx, cy, cx, cy + arrowLen, lw);
        sr.triangle(cx - lw * 2, cy + arrowLen, cx + lw * 2, cy + arrowLen, cx, cy + arrowLen + lw * 2);
        sr.rectLine(cx, cy, cx, cy - arrowLen, lw);
        sr.triangle(cx - lw * 2, cy - arrowLen, cx + lw * 2, cy - arrowLen, cx, cy - arrowLen - lw * 2);
        sr.rectLine(cx, cy, cx + arrowLen, cy, lw);
        sr.triangle(cx + arrowLen, cy - lw * 2, cx + arrowLen, cy + lw * 2, cx + arrowLen + lw * 2, cy);
        sr.rectLine(cx, cy, cx - arrowLen, cy, lw);
        sr.triangle(cx - arrowLen, cy - lw * 2, cx - arrowLen, cy + lw * 2, cx - arrowLen - lw * 2, cy);
        sr.end();

        // Zoom+ button (left of pair)
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.22f, 0.22f, 0.22f, 0.95f);
        sr.rect(zoomMinusX, btnY, btnSize, btnSize);
        sr.setColor(Color.WHITE);
        sr.rect(zoomMinusX + btnSize * 0.25f, btnY + btnSize * 0.45f, btnSize * 0.5f, lw * 1.5f);
        sr.rect(zoomMinusX + btnSize * 0.45f, btnY + btnSize * 0.25f, lw * 1.5f, btnSize * 0.5f);
        sr.end();

        // Zoom- button (right of pair)
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.22f, 0.22f, 0.22f, 0.95f);
        sr.rect(zoomPlusX, btnY, btnSize, btnSize);
        sr.setColor(Color.WHITE);
        sr.rect(zoomPlusX + btnSize * 0.25f, btnY + btnSize * 0.45f, btnSize * 0.5f, lw * 1.5f);
        sr.end();

        // Undo button
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.22f, 0.22f, 0.22f, 0.95f);
        sr.rect(undoX, btnY, btnSize, btnSize);
        sr.setColor(Color.LIGHT_GRAY);
        sr.rectLine(undoX + btnSize * 0.65f, btnY + btnSize * 0.5f, undoX + btnSize * 0.3f, btnY + btnSize * 0.5f, lw * 1.5f);
        sr.triangle(undoX + btnSize * 0.3f, btnY + btnSize * 0.32f, undoX + btnSize * 0.3f, btnY + btnSize * 0.68f, undoX + btnSize * 0.17f, btnY + btnSize * 0.5f);
        sr.end();

        // Redo button
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.22f, 0.22f, 0.22f, 0.95f);
        sr.rect(redoX, btnY, btnSize, btnSize);
        sr.setColor(Color.LIGHT_GRAY);
        sr.rectLine(redoX + btnSize * 0.35f, btnY + btnSize * 0.5f, redoX + btnSize * 0.7f, btnY + btnSize * 0.5f, lw * 1.5f);
        sr.triangle(redoX + btnSize * 0.7f, btnY + btnSize * 0.32f, redoX + btnSize * 0.7f, btnY + btnSize * 0.68f, redoX + btnSize * 0.83f, btnY + btnSize * 0.5f);
        sr.end();

        // Borders
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.4f, 0.4f, 0.4f, 1);
        sr.rect(toggleX, btnY, toggleW, btnSize);
        sr.rect(zoomMinusX, btnY, btnSize, btnSize);
        sr.rect(zoomPlusX, btnY, btnSize, btnSize);
        sr.rect(undoX, btnY, btnSize, btnSize);
        sr.rect(redoX, btnY, btnSize, btnSize);
        sr.end();

        // Zoom ratio label above strip, centered (hide during selection)
        if (!isSelectionActive()) {
            String zoomLabel = (int) app.getZoom() + "x";
            GlyphLayout gl = new GlyphLayout(font, zoomLabel);
            float zlX = width / 2f - gl.width / 2f;
            float zlY = height + 2 * dp;
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0, 0, 0, 0.5f);
            sr.rect(zlX - 4 * dp, zlY - 2 * dp, gl.width + 8 * dp, gl.height + 4 * dp);
            sr.end();
            batch.begin();
            font.setColor(Color.WHITE);
            font.draw(batch, zoomLabel, zlX, zlY + gl.height);
            batch.end();
        }
    }

    public boolean handleTouch(float touchX, float touchY) {
        if (!hit(touchX, touchY)) return false;
        if (touchY < btnY || touchY > btnY + btnSize) return true;


        if (touchX >= toggleX && touchX <= toggleX + toggleW) { movementMode = !movementMode; return true; }
        if (touchX >= zoomMinusX && touchX <= zoomMinusX + btnSize) { app.zoomIn(); return true; }
        if (touchX >= zoomPlusX && touchX <= zoomPlusX + btnSize) { app.zoomOut(); return true; }
        if (touchX >= undoX && touchX <= undoX + btnSize) { if (!isSelectionActive()) app.undo(); return true; }
        if (touchX >= redoX && touchX <= redoX + btnSize) { if (!isSelectionActive()) app.redo(); return true; }
        return true;
    }

    private boolean isSelectionActive() {
        if (app.tools[app.activeToolIndex] instanceof com.mirwanda.not2pix.SelectionTool) {
            com.mirwanda.not2pix.SelectionTool sel = (com.mirwanda.not2pix.SelectionTool) app.tools[app.activeToolIndex];
            return sel.hasSelection || sel.buffer != null;
        }
        return false;
    }
}
