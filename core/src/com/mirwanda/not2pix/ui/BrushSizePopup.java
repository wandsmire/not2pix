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
    private boolean isPencil = false;

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
        this.isPencil = (app.tools[app.activeToolIndex] instanceof PencilTool);
        this.h = isPencil ? 90 * dp : 50 * dp;
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
        float brushAreaY = isPencil ? y + 34 * dp : y;
        float brushAreaH = isPencil ? h - 34 * dp : h;
        float by2 = brushAreaY + (brushAreaH - btnSize) / 2f;
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
            font.draw(batch, sizeStr, x + w / 2f - gl.width / 2f, brushAreaY + brushAreaH / 2f + gl.height / 2f);
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

        // Pixel Perfect toggle (pencil only)
        if (isPencil) {
            boolean pp = ((PencilTool) app.tools[0]).pixelPerfect;
            float rowY = y + 4 * dp;
            float boxSize = 16 * dp;
            float boxX = x + 8 * dp;
            float boxY = rowY + (30 * dp - boxSize) / 2f;
            sr.begin(ShapeRenderer.ShapeType.Filled);
            if (pp) {
                sr.setColor(Color.CYAN);
                sr.rect(boxX, boxY, boxSize, boxSize);
            } else {
                sr.setColor(0.4f, 0.4f, 0.4f, 1);
                sr.rect(boxX, boxY, boxSize, boxSize);
            }
            sr.end();
            batch.begin();
            font.setColor(Color.WHITE);
            font.draw(batch, "Pixel Perfect", boxX + boxSize + 6 * dp, rowY + 20 * dp);
            batch.end();
        }
    }

    public boolean handleTouch(float tx, float ty) {
        if (!open) return false;
        if (tx < x || tx > x + w || ty < y || ty > y + h) {
            open = false;
            return true;
        }
        // Pixel Perfect toggle row
        if (isPencil && ty < y + 34 * dp) {
            ((PencilTool) app.tools[0]).pixelPerfect = !((PencilTool) app.tools[0]).pixelPerfect;
            return true;
        }
        float bx = x + 8 * dp;
        float by2 = y + (isPencil ? 34 * dp : 0) + (h - (isPencil ? 34 * dp : 0) - btnSize) / 2f;
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
