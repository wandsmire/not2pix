package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.mirwanda.not2pix.AnimFrame;
import com.mirwanda.not2pix.Not2Pix;

/**
 * Top frame strip for animation. Thumbnails scroll horizontally, clipped to not overlap buttons.
 */
public class FrameStrip extends UIPanel {

    private Not2Pix app;
    private float dp;
    private float thumbSize;
    private float btnSize;
    private float thumbStartX;
    public Runnable onDeleteFrame;
    private float scrollOffsetX = 0;
    private float lastDragX = -1;
    public boolean dragging = false;

    public FrameStrip(Not2Pix app, float screenWidth, float screenHeight, float dp) {
        super(0, screenHeight - 28 * dp - 28 * dp - 52 * dp, screenWidth, 52 * dp);
        this.app = app;
        this.dp = dp;
        this.thumbSize = 40 * dp;
        this.btnSize = 28 * dp;
        this.thumbStartX = 130 * dp; // after buttons
    }

    @Override
    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!visible) return;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(bgColor);
        sr.rect(x, y, width, height);
        sr.end();

        float by = y + 10 * dp;
        float bx = x + 4 * dp;

        // Play/Stop
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(app.playing ? new Color(0.7f, 0.3f, 0.3f, 1) : new Color(0.3f, 0.6f, 0.3f, 1));
        sr.rect(bx, by, btnSize, btnSize);
        sr.setColor(Color.WHITE);
        if (app.playing) {
            sr.rect(bx + 7 * dp, by + 7 * dp, 5 * dp, 14 * dp);
            sr.rect(bx + 16 * dp, by + 7 * dp, 5 * dp, 14 * dp);
        } else {
            sr.triangle(bx + 9 * dp, by + 6 * dp, bx + 9 * dp, by + btnSize - 6 * dp, bx + btnSize - 6 * dp, by + btnSize / 2f);
        }
        sr.end();
        bx += btnSize + 3 * dp;

        // + frame
        drawCtrlBtn(sr, batch, font, "+", bx, by);
        bx += btnSize + 2 * dp;
        // Dup frame
        drawCtrlBtn(sr, batch, font, "D", bx, by);
        bx += btnSize + 2 * dp;
        // Del frame
        drawCtrlBtn(sr, batch, font, "X", bx, by);

        // Frame thumbnails - only draw if x >= thumbStartX (clips behind buttons)
        float ty = y + 4 * dp;
        for (int i = 0; i < app.frames.size(); i++) {
            float frameX = thumbStartX - scrollOffsetX + i * (thumbSize + 6 * dp);
            // Clip: only draw if visible and not overlapping buttons
            if (frameX + thumbSize + 4 * dp < thumbStartX || frameX > x + width) continue;
            if (frameX < thumbStartX) continue; // don't draw partially behind buttons

            boolean active = (i == app.currentFrameIndex);
            AnimFrame frame = app.frames.get(i);

            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(active ? new Color(0.4f, 0.65f, 1f, 1) : new Color(0.28f, 0.28f, 0.28f, 1));
            sr.rect(frameX, ty, thumbSize + 4 * dp, thumbSize + 4 * dp);
            sr.end();

            if (frame.texture != null) {
                batch.begin();
                batch.draw(frame.texture, frameX + 2 * dp, ty + 2 * dp, thumbSize, thumbSize);
                batch.end();
            }

            if (font != null) {
                batch.begin();
                font.setColor(Color.WHITE);
                font.draw(batch, String.valueOf(i + 1), frameX + 4 * dp, ty + thumbSize);
                batch.end();
            }
        }
    }

    private void drawCtrlBtn(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                              String label, float bx, float by) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.28f, 0.28f, 0.28f, 1);
        sr.rect(bx, by, btnSize, btnSize);
        sr.end();
        if (font != null) {
            batch.begin();
            font.setColor(Color.WHITE);
            GlyphLayout gl = new GlyphLayout(font, label);
            font.draw(batch, label, bx + (btnSize - gl.width) / 2f, by + (btnSize + gl.height) / 2f);
            batch.end();
        }
    }

    public boolean handleTouch(float touchX, float touchY) {
        if (!hit(touchX, touchY)) return false;

        float by = y + 10 * dp;
        float bx = x + 4 * dp;

        if (touchX >= bx && touchX <= bx + btnSize && touchY >= by && touchY <= by + btnSize) {
            app.togglePlayback(); return true;
        }
        bx += btnSize + 3 * dp;

        if (touchX >= bx && touchX <= bx + btnSize && touchY >= by && touchY <= by + btnSize) { app.addFrame(); return true; }
        bx += btnSize + 2 * dp;
        if (touchX >= bx && touchX <= bx + btnSize && touchY >= by && touchY <= by + btnSize) { app.duplicateFrame(); return true; }
        bx += btnSize + 2 * dp;
        if (touchX >= bx && touchX <= bx + btnSize && touchY >= by && touchY <= by + btnSize) {
            if (onDeleteFrame != null) onDeleteFrame.run(); else app.deleteFrame();
            return true;
        }

        // Thumbnail area
        if (touchX >= thumbStartX) {
            lastDragX = touchX;
            dragging = true;
            int idx = (int) ((touchX - thumbStartX + scrollOffsetX) / (thumbSize + 6 * dp));
            if (idx >= 0 && idx < app.frames.size()) {
                app.saveFrameState();
                app.currentFrameIndex = idx;
                app.loadFramePublic(idx);
            }
            return true;
        }
        return true;
    }

    public void handleDrag(float touchX, float touchY) {
        if (dragging && lastDragX >= 0) {
            float dx = lastDragX - touchX;
            float maxScroll = Math.max(0, app.frames.size() * (thumbSize + 6 * dp) - (width - thumbStartX));
            scrollOffsetX = MathUtils.clamp(scrollOffsetX + dx, 0, maxScroll);
            lastDragX = touchX;
        }
    }

    public void handleUp() {
        dragging = false;
        lastDragX = -1;
    }
}
