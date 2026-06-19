package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mirwanda.not2pix.Not2Pix;
import com.mirwanda.not2pix.ShapeTool;

/**
 * Popup to select shape type (Line, Rect, Diamond, Ellipse).
 */
public class ShapeSelector {

    private Not2Pix app;
    public boolean open = false;
    private float x, y, w, h;
    private float itemH;
    private float dp;
    private String[] labels = {"Line", "Rectangle", "Diamond", "Ellipse", "Lasso Fill"};

    public ShapeSelector(Not2Pix app, float dp) {
        this.app = app;
        this.dp = dp;
        this.w = 150 * dp;
        this.itemH = 40 * dp;
        this.h = labels.length * itemH;
    }

    public void show(float bx, float by) {
        this.x = bx;
        this.y = by;
        open = true;
    }

    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!open) return;
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0, 0, 0, 0.5f);
        sr.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        sr.setColor(0.18f, 0.18f, 0.18f, 1);
        sr.rect(x, y, w, h);
        sr.end();

        ShapeTool.Shape current = getShapeTool() != null ? getShapeTool().currentShape : ShapeTool.Shape.LINE;

        // Draw shape icons
        float iconSize = 16 * dp;
        float iconX = x + 10 * dp;
        sr.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < labels.length; i++) {
            float iy = y + h - (i + 1) * itemH;
            float icY = iy + (itemH - iconSize) / 2f;
            boolean active = (i == current.ordinal());
            sr.setColor(active ? Color.CYAN : Color.WHITE);
            if (i == 0) { // Line - diagonal
                sr.line(iconX, icY, iconX + iconSize, icY + iconSize);
            } else if (i == 1) { // Rectangle
                sr.rect(iconX, icY, iconSize, iconSize);
            } else if (i == 2) { // Diamond
                float cx = iconX + iconSize / 2f, cy = icY + iconSize / 2f, r = iconSize / 2f;
                sr.line(cx, cy + r, cx + r, cy);
                sr.line(cx + r, cy, cx, cy - r);
                sr.line(cx, cy - r, cx - r, cy);
                sr.line(cx - r, cy, cx, cy + r);
            } else if (i == 3) { // Ellipse
                sr.ellipse(iconX, icY, iconSize, iconSize);
            } else { // Lasso Fill - wavy closed shape
                float cx2 = iconX + iconSize / 2f, cy2 = icY + iconSize / 2f;
                float r2 = iconSize / 2.5f;
                sr.line(cx2 - r2, cy2, cx2, cy2 + r2);
                sr.line(cx2, cy2 + r2, cx2 + r2, cy2 + r2 * 0.3f);
                sr.line(cx2 + r2, cy2 + r2 * 0.3f, cx2 + r2 * 0.5f, cy2 - r2);
                sr.line(cx2 + r2 * 0.5f, cy2 - r2, cx2 - r2, cy2);
            }
        }
        sr.end();

        batch.begin();
        for (int i = 0; i < labels.length; i++) {
            float iy = y + h - (i + 1) * itemH;
            boolean active = (i == current.ordinal());
            font.setColor(active ? Color.CYAN : Color.WHITE);
            GlyphLayout gl = new GlyphLayout(font, labels[i]);
            font.draw(batch, labels[i], iconX + iconSize + 8 * dp, iy + (itemH + gl.height) / 2f);
        }
        batch.end();
    }

    public boolean handleTouch(float tx, float ty) {
        if (!open) return false;
        if (tx >= x && tx <= x + w && ty >= y && ty <= y + h) {
            int idx = (int) ((y + h - ty) / itemH);
            ShapeTool st = getShapeTool();
            if (st != null && idx >= 0 && idx < ShapeTool.Shape.values().length) {
                st.currentShape = ShapeTool.Shape.values()[idx];
            }
            open = false;
            return true;
        }
        open = false;
        return true;
    }

    private ShapeTool getShapeTool() {
        for (com.mirwanda.not2pix.Tool t : app.tools) {
            if (t instanceof ShapeTool) return (ShapeTool) t;
        }
        return null;
    }
}
