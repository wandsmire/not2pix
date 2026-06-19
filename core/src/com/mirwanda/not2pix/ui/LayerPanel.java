package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
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
    public Runnable onOpen;
    public boolean dragging = false;
    private float lastDragY = -1;

    // Longpress reorder state
    private int reorderDragIndex = -1;
    private float reorderDragY = -1;
    private float layerTouchDownTime = 0;
    private int layerTouchDownIdx = -1;
    private boolean layerLongPressed = false;
    private static final float LAYER_LONGPRESS_TIME = 0.4f;
    private float layerTouchDownY = -1;

    // Solo mode
    private boolean soloMode = false;
    private boolean[] savedVisibility = null;

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
        // When open, position toggle button at the left edge of the panel; when closed, next to palette
        if (open) {
            toggleBtn.x = x - toggleBtn.width - 2 * dp;
        } else {
            toggleBtn.x = screenWidth - paletteW - 30 * dp;
        }
        toggleBtn.draw(sr, batch, font);
        if (!open) return;

        checkLayerLongPress();

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(bgColor);
        sr.rect(x, y, width, height);
        sr.end();

        float listBottom = y + btnRowHeight;
        float listTop = y + height;
        float lh = 34 * dp;
        float startY = listBottom + scrollOffsetY;

        for (int i = 0; i < app.layers.size(); i++) {
            Layer layer = app.layers.get(i);
            boolean active = (i == app.activeLayerIndex);
            float itemY = startY;

            if (itemY + lh > listBottom && itemY < listTop) {
                if (itemY >= listBottom && itemY + lh <= listTop) {
                    boolean isDragged = (i == reorderDragIndex);
                    float alpha = isDragged ? 0.5f : 1f;

                    sr.begin(ShapeRenderer.ShapeType.Filled);
                    Color bg = active ? new Color(0.3f, 0.4f, 0.55f, alpha) : new Color(0.22f, 0.22f, 0.22f, alpha);
                    sr.setColor(bg);
                    sr.rect(x + 4 * dp, itemY, panelWidth - 8 * dp, lh - 2 * dp);
                    sr.end();

                    // Eye icon
                    drawEyeIcon(sr, x + 16 * dp, itemY + lh / 2f, 5 * dp, layer.visible);

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

        // Draw reorder drop indicator
        if (reorderDragIndex >= 0 && reorderDragY >= 0) {
            int targetIdx = getReorderTargetIndex();
            float lineY = listBottom + scrollOffsetY + targetIdx * lh;
            if (lineY >= listBottom && lineY <= listTop) {
                sr.begin(ShapeRenderer.ShapeType.Filled);
                sr.setColor(Color.WHITE);
                sr.rect(x + 4 * dp, lineY - 1 * dp, panelWidth - 8 * dp, 2 * dp);
                sr.end();
            }
        }

        // Bottom buttons
        float btnW = 34 * dp, btnH = 32 * dp;
        float bx = x + 4 * dp;
        float btnY = y + 4 * dp;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(bgColor);
        sr.rect(x, y, panelWidth, btnRowHeight);
        sr.end();
        drawSmallBtn(sr, bx, btnY, btnW, btnH, 0.4f, 0.7f, 0.4f); // +
        // - red
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.6f, 0.3f, 0.3f, 1);
        sr.rect(bx + (btnW + 4 * dp), btnY, btnW, btnH);
        sr.end();
        drawUpArrow(sr, bx + (btnW + 4 * dp) * 2, btnY, btnW, btnH);
        drawDownArrow(sr, bx + (btnW + 4 * dp) * 3, btnY, btnW, btnH);
        // Solo button
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(soloMode ? new Color(0.7f, 0.6f, 0.2f, 1) : new Color(0.3f, 0.3f, 0.3f, 1));
        sr.rect(bx + (btnW + 4 * dp) * 4, btnY, btnW, btnH);
        sr.end();

        if (font != null) {
            batch.begin();
            font.setColor(Color.WHITE);
            GlyphLayout gl = new GlyphLayout(font, "+");
            font.draw(batch, "+", bx + (btnW - gl.width) / 2f, btnY + (btnH + gl.height) / 2f);
            gl.setText(font, "-");
            font.draw(batch, "-", bx + (btnW + 4 * dp) + (btnW - gl.width) / 2f, btnY + (btnH + gl.height) / 2f);
            gl.setText(font, "S");
            font.draw(batch, "S", bx + (btnW + 4 * dp) * 4 + (btnW - gl.width) / 2f, btnY + (btnH + gl.height) / 2f);
            batch.end();
        }
    }

    private void drawEyeIcon(ShapeRenderer sr, float cx, float cy, float r, boolean visible) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        if (visible) {
            sr.setColor(Color.WHITE);
        } else {
            sr.setColor(Color.DARK_GRAY);
        }
        // Almond/eye shape: two triangles forming a diamond-like shape
        sr.triangle(cx - r * 1.6f, cy, cx, cy + r * 0.7f, cx + r * 1.6f, cy);
        sr.triangle(cx - r * 1.6f, cy, cx, cy - r * 0.7f, cx + r * 1.6f, cy);
        // Inner circle (pupil)
        sr.setColor(visible ? new Color(0.2f, 0.3f, 0.5f, 1) : new Color(0.15f, 0.15f, 0.15f, 1));
        sr.circle(cx, cy, r * 0.5f);
        sr.end();

        // Cross out if not visible
        if (!visible) {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(Color.RED);
            float len = r * 1.4f;
            sr.rectLine(cx - len, cy - len * 0.6f, cx + len, cy + len * 0.6f, 1.5f * dp);
            sr.end();
        }
    }

    private void checkLayerLongPress() {
        if (layerTouchDownIdx < 0 || layerLongPressed) return;
        if (!Gdx.input.isTouched()) { layerTouchDownIdx = -1; return; }
        float elapsed = (System.nanoTime() / 1_000_000_000f) - layerTouchDownTime;
        float currentY = Gdx.input.getY();
        if (Math.abs(currentY - layerTouchDownY) > 10 * dp) { layerTouchDownIdx = -1; return; }
        if (elapsed >= LAYER_LONGPRESS_TIME) {
            layerLongPressed = true;
            reorderDragIndex = layerTouchDownIdx;
            reorderDragY = Gdx.graphics.getHeight() - Gdx.input.getY();
        }
    }

    private int getReorderTargetIndex() {
        float listBottom = y + btnRowHeight;
        float lh = 34 * dp;
        float relY = reorderDragY - listBottom - scrollOffsetY;
        int target = MathUtils.clamp((int) (relY / lh), 0, app.layers.size());
        return target;
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
        if (toggleBtn.hit(touchX, touchY)) { open = !open; if (open && onOpen != null) onOpen.run(); return true; }
        if (!open) return false;
        if (touchX < x) return false;

        float btnW = 34 * dp, btnH = 32 * dp;
        float bx = x + 4 * dp;
        float btnY = y + 4 * dp;
        if (touchY >= btnY && touchY <= btnY + btnH) {
            int btnIdx = (int) ((touchX - bx) / (btnW + 4 * dp));
            if (btnIdx == 0) app.addLayer();
            else if (btnIdx == 1) { if (onDeleteLayer != null) onDeleteLayer.run(); else app.removeLayer(); }
            else if (btnIdx == 2) app.moveLayerUp();
            else if (btnIdx == 3) app.moveLayerDown();
            else if (btnIdx == 4) toggleSoloMode();
            return true;
        }

        // Layer list
        float listBottom = y + btnRowHeight;
        if (touchY < listBottom) return true;

        float lh = 34 * dp;
        float startY2 = listBottom + scrollOffsetY;
        for (int i = 0; i < app.layers.size(); i++) {
            if (touchY >= startY2 && touchY <= startY2 + lh && startY2 >= listBottom) {
                if (touchX < x + 26 * dp) {
                    app.layers.get(i).visible = !app.layers.get(i).visible;
                    soloMode = false;
                    return true;
                } else {
                    app.activeLayerIndex = i;
                    // Start longpress tracking
                    layerTouchDownIdx = i;
                    layerTouchDownTime = System.nanoTime() / 1_000_000_000f;
                    layerTouchDownY = Gdx.input.getY();
                    layerLongPressed = false;
                    return true;
                }
            }
            startY2 += lh;
        }

        dragging = true;
        lastDragY = touchY;
        return true;
    }

    private void toggleSoloMode() {
        if (!soloMode) {
            savedVisibility = new boolean[app.layers.size()];
            for (int i = 0; i < app.layers.size(); i++) {
                savedVisibility[i] = app.layers.get(i).visible;
                app.layers.get(i).visible = (i == app.activeLayerIndex);
            }
            soloMode = true;
        } else {
            if (savedVisibility != null) {
                for (int i = 0; i < app.layers.size() && i < savedVisibility.length; i++) {
                    app.layers.get(i).visible = savedVisibility[i];
                }
            }
            savedVisibility = null;
            soloMode = false;
        }
    }

    public void handleDrag(float touchX, float touchY) {
        if (!open) return;
        if (layerLongPressed) {
            reorderDragY = touchY;
            return;
        }
        if (lastDragY >= 0) {
            float dy = touchY - lastDragY;
            float lh = 34 * dp;
            float listHeight = height - btnRowHeight;
            float contentHeight = app.layers.size() * lh;
            float maxScroll = Math.max(0, contentHeight - listHeight);
            scrollOffsetY = MathUtils.clamp(scrollOffsetY + dy, -maxScroll, 0);
        }
        lastDragY = touchY;
        dragging = true;
    }

    public void handleUp() {
        if (reorderDragIndex >= 0) {
            int targetIdx = getReorderTargetIndex();
            if (targetIdx != reorderDragIndex && targetIdx != reorderDragIndex + 1) {
                Layer moved = app.layers.remove(reorderDragIndex);
                int insertAt = targetIdx > reorderDragIndex ? targetIdx - 1 : targetIdx;
                insertAt = MathUtils.clamp(insertAt, 0, app.layers.size());
                app.layers.add(insertAt, moved);
                if (app.activeLayerIndex == reorderDragIndex) {
                    app.activeLayerIndex = insertAt;
                } else if (app.activeLayerIndex > reorderDragIndex && app.activeLayerIndex <= insertAt) {
                    app.activeLayerIndex--;
                } else if (app.activeLayerIndex < reorderDragIndex && app.activeLayerIndex >= insertAt) {
                    app.activeLayerIndex++;
                }
            }
        }
        reorderDragIndex = -1;
        reorderDragY = -1;
        layerTouchDownIdx = -1;
        layerLongPressed = false;
        dragging = false;
        lastDragY = -1;
    }
}
