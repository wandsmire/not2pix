package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mirwanda.not2pix.EraserTool;
import com.mirwanda.not2pix.Not2Pix;
import com.mirwanda.not2pix.PencilTool;
import com.mirwanda.not2pix.Tool;

/**
 * Small popup showing brush size +/- buttons. Opens on long-press of pencil/eraser tool.
 */
public class BrushSizePopup {

    private Not2Pix app;
    public boolean open = false;
    private float x, y, w, h;
    private float dp;
    private float btnSize;

    public BrushSizePopup(Not2Pix app, float dp) {
        this.app = app;
        this.dp = dp;
        this.btnSize = 40 * dp;
        this.w = 160 * dp;
        this.h = 50 * dp;
    }

    public void show(float bx, float by) {
        this.x = bx;
        this.y = by;
        open = true;
    }

    public int getCurrentSize() {
        Tool t = app.tools[app.activeToolIndex];
        if (t instanceof PencilTool) return ((PencilTool) t).brushSize;
        if (t instanceof EraserTool) return ((EraserTool) t).brushSize;
        return 1;
    }

    private void setSize(int s) {
        if (s < 1) s = 1;
        if (s > 16) s = 16;
        Tool t = app.tools[app.activeToolIndex];
        if (t instanceof PencilTool) ((PencilTool) t).brushSize = s;
        if (t instanceof EraserTool) ((EraserTool) t).brushSize = s;
    }

    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!open) return;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.18f, 0.18f, 0.18f, 1);
        sr.rect(x, y, w, h);
        sr.end();

        // - button
        float bx = x + 8 * dp;
        float by2 = y + (h - btnSize) / 2f;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.3f, 0.3f, 0.3f, 1);
        sr.rect(bx, by2, btnSize, btnSize);
        sr.end();

        // Size display
        int size = getCurrentSize();
        if (font != null) {
            batch.begin();
            font.setColor(Color.WHITE);
            GlyphLayout gl = new GlyphLayout(font, "-");
            font.draw(batch, "-", bx + (btnSize - gl.width) / 2f, by2 + (btnSize + gl.height) / 2f);
            // Size number
            String sizeStr = String.valueOf(size);
            gl.setText(font, sizeStr);
            font.draw(batch, sizeStr, x + w / 2f - gl.width / 2f, y + h / 2f + gl.height / 2f);
            batch.end();
        }

        // + button
        bx = x + w - btnSize - 8 * dp;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.3f, 0.3f, 0.3f, 1);
        sr.rect(bx, by2, btnSize, btnSize);
        sr.end();
        if (font != null) {
            batch.begin();
            font.setColor(Color.WHITE);
            GlyphLayout gl = new GlyphLayout(font, "+");
            font.draw(batch, "+", bx + (btnSize - gl.width) / 2f, by2 + (btnSize + gl.height) / 2f);
            batch.end();
        }

        // Size preview dot
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(Color.WHITE);
        sr.circle(x + w / 2f, y + h - 8 * dp, Math.max(1, size / 2f) * dp);
        sr.end();
    }

    public boolean handleTouch(float tx, float ty) {
        if (!open) return false;
        if (tx < x || tx > x + w || ty < y || ty > y + h) {
            open = false;
            return true;
        }
        float bx = x + 8 * dp;
        float by2 = y + (h - btnSize) / 2f;
        // - button
        if (tx >= bx && tx <= bx + btnSize && ty >= by2 && ty <= by2 + btnSize) {
            setSize(getCurrentSize() - 1);
            return true;
        }
        // + button
        bx = x + w - btnSize - 8 * dp;
        if (tx >= bx && tx <= bx + btnSize && ty >= by2 && ty <= by2 + btnSize) {
            setSize(getCurrentSize() + 1);
            return true;
        }
        return true;
    }
}
