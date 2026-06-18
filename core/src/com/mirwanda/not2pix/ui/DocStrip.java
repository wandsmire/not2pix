package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.mirwanda.not2pix.Document;
import com.mirwanda.not2pix.Not2Pix;

public class DocStrip extends UIPanel {

    private Not2Pix app;
    private float dp;
    private float tabW;

    // Horizontal scroll
    private float scrollX = 0f;
    private float lastDragX = -1f;
    private boolean dragging = false;

    public DocStrip(Not2Pix app, float screenWidth, float screenHeight, float dp) {
        super(0, screenHeight - 28 * dp - 28 * dp, screenWidth, 28 * dp);
        this.app = app;
        this.dp = dp;
        this.tabW = 90 * dp;
    }

    /** Ensure the active tab is always visible (call after switching documents). */
    public void scrollToActive() {
        float contentW = app.documents.size() * tabW;
        float maxScroll = Math.max(0, contentW - width);
        float tabLeft = app.activeDocIndex * tabW;
        float tabRight = tabLeft + tabW;
        if (tabLeft - scrollX < 0) {
            scrollX = tabLeft;
        } else if (tabRight - scrollX > width) {
            scrollX = tabRight - width;
        }
        scrollX = MathUtils.clamp(scrollX, 0, maxScroll);
    }

    @Override
    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {

        // Background
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.12f, 0.12f, 0.12f, 0.95f);
        sr.rect(x, y, width, height);

        for (int i = 0; i < app.documents.size(); i++) {
            float tx = x + i * tabW - scrollX;
            // Clip to strip bounds
            if (tx + tabW < x || tx > x + width) continue;
            if (i == app.activeDocIndex) {
                sr.setColor(0.3f, 0.3f, 0.4f, 1);
            } else {
                sr.setColor(0.18f, 0.18f, 0.18f, 1);
            }
            sr.rect(tx + 1, y + 2 * dp, tabW - 2, height - 4 * dp);
        }
        sr.end();

        batch.begin();
        for (int i = 0; i < app.documents.size(); i++) {
            float tx = x + i * tabW - scrollX + 6 * dp;
            // Clip to strip bounds
            if (tx + tabW - 6 * dp < x || tx > x + width) continue;
            Document doc = app.documents.get(i);
            font.setColor(i == app.activeDocIndex ? Color.WHITE : Color.GRAY);
            String label = doc.name;
            if (label.length() > 10) label = label.substring(0, 10);
            font.draw(batch, label, tx, y + height / 2f + 5 * dp);
        }
        batch.end();

        // Scrollbar — only when content overflows
        float contentW = app.documents.size() * tabW;
        if (contentW > width) {
            float thumbW = width / contentW * width;
            float thumbX = x + (scrollX / contentW) * width;
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0.4f, 0.4f, 0.4f, 0.6f);
            sr.rect(thumbX, y, thumbW, 2 * dp);
            sr.end();
        }
    }

    public boolean handleTouch(float tx, float ty) {
        if (!hit(tx, ty)) return false;
        // Begin tracking for potential drag
        lastDragX = tx;
        dragging = false;
        return true;
    }

    public void handleDrag(float tx, float ty) {
        if (lastDragX < 0) return;
        float dx = tx - lastDragX;
        if (Math.abs(dx) > 4 * dp) dragging = true;
        if (dragging) {
            float contentW = app.documents.size() * tabW;
            float maxScroll = Math.max(0, contentW - width);
            scrollX = MathUtils.clamp(scrollX - dx, 0, maxScroll);
        }
        lastDragX = tx;
    }

    public void handleUp(float tx, float ty) {
        if (!dragging) {
            // It was a tap — switch to tapped document
            int idx = (int) ((tx - x + scrollX) / tabW);
            if (idx >= 0 && idx < app.documents.size()) {
                app.switchDocument(idx);
                scrollToActive();
            }
        }
        lastDragX = -1f;
        dragging = false;
    }
}
