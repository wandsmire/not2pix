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
    public Runnable onCloseDoc; // called with app.activeDocIndex set to the target

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

        // Draw + button after all tabs
        float plusX = x + app.documents.size() * tabW - scrollX;
        if (plusX >= x && plusX < x + width) {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0.25f, 0.25f, 0.25f, 1);
            float btnSize = height - 4 * dp;
            sr.rect(plusX + 2, y + 2 * dp, btnSize, btnSize);
            sr.end();
        }

        batch.begin();
        for (int i = 0; i < app.documents.size(); i++) {
            float tx2 = x + i * tabW - scrollX + 6 * dp;
            if (tx2 + tabW - 6 * dp < x || tx2 > x + width) continue;
            Document doc = app.documents.get(i);
            font.setColor(i == app.activeDocIndex ? Color.WHITE : Color.GRAY);
            String label = doc.name;
            if (label.length() > 8) label = label.substring(0, 8);
            font.draw(batch, label, tx2, y + height / 2f + 5 * dp);
            // X close button
            font.setColor(i == app.activeDocIndex ? new Color(1f, 0.5f, 0.5f, 1f) : Color.DARK_GRAY);
            font.draw(batch, "x", x + i * tabW - scrollX + tabW - 14 * dp, y + height - 4 * dp);
        }
        // Draw + text on the button
        if (plusX >= x && plusX < x + width) {
            float btnSize = height - 4 * dp;
            font.setColor(Color.WHITE);
            font.draw(batch, "+", plusX + btnSize * 0.35f, y + height / 2f + 5 * dp);
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
            int idx = (int) ((tx - x + scrollX) / tabW);
            if (idx >= app.documents.size()) {
                app.newDocument();
            } else if (idx >= 0) {
                // Check if tap is on the X close area (right 16dp of tab)
                float tabLeft = x + idx * tabW - scrollX;
                if (tx >= tabLeft + tabW - 16 * dp) {
                    // Close this doc (switch to it first so callback knows which)
                    if (idx != app.activeDocIndex) app.switchDocument(idx);
                    if (onCloseDoc != null) onCloseDoc.run();
                } else {
                    app.switchDocument(idx);
                    scrollToActive();
                }
            }
        }
        lastDragX = -1f;
        dragging = false;
    }
}
