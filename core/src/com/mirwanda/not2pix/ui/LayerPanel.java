package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.mirwanda.not2pix.Layer;
import com.mirwanda.not2pix.Not2Pix;

public class LayerPanel extends UIPanel {

    private Not2Pix app;
    private float dp;
    private float panelWidth;
    public boolean open = false;
    private float screenWidth;
    private UIButton toggleBtn;
    private float scrollOffsetY = 0;
    private float btnRowHeight;
    public Runnable onDeleteLayer;
    public boolean dragging = false;
    private float lastDragY = -1;

    public LayerPanel(Not2Pix app, float screenWidth, float screenHeight, float dp) {
        super(screenWidth, 0, 180 * dp, screenHeight - 52 * dp - 28 * dp);
        this.app = app;
        this.dp = dp;
        this.panelWidth = 180 * dp;
        this.screenWidth = screenWidth;
        this.btnRowHeight = 40 * dp;

        // Toggle button directly left of the palette swatch column
        float paletteW = 36 * dp + 6 * dp; // swatchSize + padding
        toggleBtn = new UIButton("L", screenWidth - paletteW - 30 * dp, 104 * dp, 28 * dp, 50 * dp);
        toggleBtn.iconDrawer = (sr, bx, by, w, h, sel) -> {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(Color.LIGHT_GRAY);
            sr.rect(bx + w * 0.2f, by + h * 0.2f, w * 0.6f, h * 0.12f);
            sr.rect(bx + w * 0.15f, by + h * 0.44f, w * 0.7f, h * 0.12f);
            sr.rect(bx + w * 0.1f, by + h * 0.68f, w * 0.8f, h * 0.12f);
            sr.end();
        };
    }

    @Override
    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        float paletteW = 36 * dp + 6 * dp;
        float targetX = open ? screenWidth - panelWidth : screenWidth;
        x = targetX;
        // Toggle button stays fixed next to palette
        toggleBtn.x = screenWidth - paletteW - 30 * dp;
        toggleBtn.draw(sr, batch, font);
        if (!open) return;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(bgColor);
        sr.rect(x, y, width, height);
        sr.end();

        // Layer list area (above buttons)
        float listBottom = y + btnRowHeight;
        float listTop = y + height;
        float lh = 34 * dp;
        // Layers drawn bottom-up: layer 0 (Background) at the bottom of the list,
        // highest index at the top — matching Aseprite/Photoshop convention.
        float startY = listBottom + scrollOffsetY;

        for (int i = 0; i < app.layers.size(); i++) {
            Layer layer = app.layers.get(i);
            boolean active = (i == app.activeLayerIndex);
            float itemY = startY;

            // Clip: only draw if within list bounds
            if (itemY + lh > listBottom && itemY < listTop) {
                if (itemY >= listBottom && itemY + lh <= listTop) {
                    sr.begin(ShapeRenderer.ShapeType.Filled);
                    sr.setColor(active ? new Color(0.3f, 0.4f, 0.55f, 1) : new Color(0.22f, 0.22f, 0.22f, 1));
                    sr.rect(x + 4 * dp, itemY, panelWidth - 8 * dp, lh - 2 * dp);
                    sr.end();

                    sr.begin(ShapeRenderer.ShapeType.Filled);
                    sr.setColor(layer.visible ? Color.WHITE : Color.DARK_GRAY);
                    sr.circle(x + 16 * dp, itemY + lh / 2f, 5 * dp);
                    sr.end();

                    if (font != null) {
                        batch.begin();
                        font.setColor(layer.visible ? Color.WHITE : Color.GRAY);
                        font.draw(batch, layer.name, x + 30 * dp, itemY + lh * 0.65f);
                        batch.end();
                    }
                }
            }
            startY += lh;
        }

        // Bottom buttons (drawn AFTER layers so they appear on top)
        float btnW = 38 * dp, btnH = 32 * dp;
        float bx = x + 4 * dp;
        float btnY = y + 4 * dp;
        // Background for button row
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(bgColor);
        sr.rect(x, y, panelWidth, btnRowHeight);
        sr.end();
        drawSmallBtn(sr, bx, btnY, btnW, btnH, 0.4f, 0.7f, 0.4f); // + green
        drawUpArrow(sr, bx + (btnW + 4 * dp) * 2, btnY, btnW, btnH);
        drawDownArrow(sr, bx + (btnW + 4 * dp) * 3, btnY, btnW, btnH);
        // - red
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.6f, 0.3f, 0.3f, 1);
        sr.rect(bx + btnW + 4 * dp, btnY, btnW, btnH);
        sr.end();

        if (font != null) {
            batch.begin();
            font.setColor(Color.WHITE);
            GlyphLayout gl = new GlyphLayout(font, "+");
            font.draw(batch, "+", bx + (btnW - gl.width) / 2f, btnY + (btnH + gl.height) / 2f);
            gl.setText(font, "-");
            font.draw(batch, "-", bx + btnW + 4 * dp + (btnW - gl.width) / 2f, btnY + (btnH + gl.height) / 2f);
            batch.end();
        }
    }

    private void drawSmallBtn(ShapeRenderer sr, float bx, float by, float bw, float bh, float r, float g, float b) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(r, g, b, 1);
        sr.rect(bx, by, bw, bh);
        sr.end();
    }

    private void drawUpArrow(ShapeRenderer sr, float bx, float by, float bw, float bh) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.3f, 0.3f, 0.3f, 1);
        sr.rect(bx, by, bw, bh);
        sr.setColor(Color.WHITE);
        sr.triangle(bx + bw * 0.5f, by + bh * 0.8f, bx + bw * 0.25f, by + bh * 0.3f, bx + bw * 0.75f, by + bh * 0.3f);
        sr.end();
    }

    private void drawDownArrow(ShapeRenderer sr, float bx, float by, float bw, float bh) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.3f, 0.3f, 0.3f, 1);
        sr.rect(bx, by, bw, bh);
        sr.setColor(Color.WHITE);
        sr.triangle(bx + bw * 0.5f, by + bh * 0.2f, bx + bw * 0.25f, by + bh * 0.7f, bx + bw * 0.75f, by + bh * 0.7f);
        sr.end();
    }

    public boolean handleTouch(float touchX, float touchY) {
        if (toggleBtn.hit(touchX, touchY)) { open = !open; return true; }
        if (!open) return false;
        if (touchX < x) return false;

        float btnW = 38 * dp, btnH = 32 * dp;
        float bx = x + 4 * dp;
        float btnY = y + 4 * dp;
        if (touchY >= btnY && touchY <= btnY + btnH) {
            int btnIdx = (int) ((touchX - bx) / (btnW + 4 * dp));
            if (btnIdx == 0) app.addLayer();
            else if (btnIdx == 1) { if (onDeleteLayer != null) onDeleteLayer.run(); else app.removeLayer(); }
            else if (btnIdx == 2) app.moveLayerUp();
            else if (btnIdx == 3) app.moveLayerDown();
            return true;
        }

        // Layer list (bottom-up ordering: index 0 at bottom)
        float listBottom = y + btnRowHeight;
        if (touchY < listBottom) return true;

        float lh = 34 * dp;
        float startY2 = listBottom + scrollOffsetY;
        for (int i = 0; i < app.layers.size(); i++) {
            if (touchY >= startY2 && touchY <= startY2 + lh && startY2 >= listBottom) {
                if (touchX < x + 26 * dp) {
                    app.layers.get(i).visible = !app.layers.get(i).visible;
                } else {
                    app.activeLayerIndex = i;
                }
                return true;
            }
            startY2 += lh;
        }

        dragging = true;
        lastDragY = touchY;
        return true;
    }

    public void handleDrag(float touchX, float touchY) {
        if (!open) return;
        if (lastDragY >= 0) {
            float dy = touchY - lastDragY;
            float lh = 34 * dp;
            float listHeight = height - btnRowHeight;
            float contentHeight = app.layers.size() * lh;
            // scrollOffsetY shifts rows upward (negative) to reveal higher layers,
            // or 0 when already showing the bottom of the stack.
            float maxScroll = Math.max(0, contentHeight - listHeight);
            scrollOffsetY = MathUtils.clamp(scrollOffsetY + dy, -maxScroll, 0);
        }
        lastDragY = touchY;
        dragging = true;
    }

    public void handleUp() {
        dragging = false;
        lastDragY = -1;
    }
}
