package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mirwanda.not2pix.Layer;
import com.mirwanda.not2pix.Not2Pix;

public class Minimap {

    private Not2Pix app;
    private float dp;
    private float maxSize;
    private float drawW, drawH;
    private Texture thumbnail;
    private int frameCounter = 0;
    private float mx, my;

    public Minimap(Not2Pix app, float dp) {
        this.app = app;
        this.dp = dp;
    }

    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!app.showMinimap) return;
        this.maxSize = app.minimapSize * dp;
        float sh = Gdx.graphics.getHeight();
        // Proportional to canvas
        float aspect = (float) app.canvasWidth / app.canvasHeight;
        if (aspect >= 1f) {
            drawW = maxSize;
            drawH = maxSize / aspect;
        } else {
            drawH = maxSize;
            drawW = maxSize * aspect;
        }
        mx = 6 * dp;
        float topOffset = (app.frameStripOpen ? 82 * dp : 0) + 28 * dp + 28 * dp; // frame strip (if open) + status bar + doc strip
        my = sh - topOffset - drawH - 6 * dp;

        frameCounter++;
        if (frameCounter >= 15) {
            frameCounter = 0;
            updateThumbnail();
        }

        sr.begin(ShapeRenderer.ShapeType.Filled);
        float cellW = drawW / 8f;
        float cellH = drawH / 8f;
        for (int cy = 0; cy < 8; cy++) {
            for (int cx = 0; cx < 8; cx++) {
                sr.setColor((cx + cy) % 2 == 0 ? app.checkerLight : app.checkerDark);
                sr.rect(mx + cx * cellW, my + cy * cellH, cellW, cellH);
            }
        }
        sr.end();

        if (thumbnail != null) {
            batch.begin();
            batch.draw(thumbnail, mx, my, drawW, drawH);
            batch.end();
        }

        // Draw viewport rectangle when zoomed in
        float zoom = app.getZoom();
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        float visW = screenW / zoom;
        float visH = screenH / zoom;
        if (visW < app.canvasWidth || visH < app.canvasHeight) {
            float camCx = app.canvasWidth / 2f - app.getPanX();
            float camCy = app.canvasHeight / 2f - app.getPanY();
            float left = camCx - visW / 2f;
            float bottom = camCy - visH / 2f;
            // Map canvas coords to minimap coords
            float rx = mx + (left / app.canvasWidth) * drawW;
            float ry = my + (bottom / app.canvasHeight) * drawH;
            float rw = (visW / app.canvasWidth) * drawW;
            float rh = (visH / app.canvasHeight) * drawH;
            // Clamp to minimap bounds
            float rx2 = Math.min(rx + rw, mx + drawW);
            float ry2 = Math.min(ry + rh, my + drawH);
            rx = Math.max(rx, mx);
            ry = Math.max(ry, my);
            rw = rx2 - rx;
            rh = ry2 - ry;
            if (rw > 0 && rh > 0) {
                float lw = 2 * dp;
                sr.begin(ShapeRenderer.ShapeType.Filled);
                sr.setColor(Color.DARK_GRAY);
                sr.rect(rx, ry, rw, lw);
                sr.rect(rx, ry + rh - lw, rw, lw);
                sr.rect(rx, ry, lw, rh);
                sr.rect(rx + rw - lw, ry, lw, rh);
                sr.end();
            }
        }

        // Border around minimap
        float bw = 2 * dp;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(Color.DARK_GRAY);
        sr.rect(mx - bw, my - bw, drawW + bw * 2, bw);
        sr.rect(mx - bw, my + drawH, drawW + bw * 2, bw);
        sr.rect(mx - bw, my, bw, drawH);
        sr.rect(mx + drawW, my, bw, drawH);
        sr.end();
    }

    public boolean hit(float tx, float ty) {
        if (!app.showMinimap) return false;
        return tx >= mx && tx <= mx + drawW && ty >= my && ty <= my + drawH;
    }

    public void handleTouch(float tx, float ty) {
        float ratioX = (tx - mx) / drawW;
        float ratioY = (ty - my) / drawH;
        float canvasX = ratioX * app.canvasWidth;
        float canvasY = ratioY * app.canvasHeight;
        float cx = app.canvasWidth / 2f;
        float cy = app.canvasHeight / 2f;
        app.setPan(-(canvasX - cx), -(canvasY - cy));
    }

    private void updateThumbnail() {
        int tw = app.canvasWidth, th = app.canvasHeight;
        int maxDim = 128;
        if (tw > maxDim || th > maxDim) {
            float scale = (float) maxDim / Math.max(tw, th);
            tw = Math.max(1, (int)(tw * scale));
            th = Math.max(1, (int)(th * scale));
        }
        Pixmap flat = new Pixmap(tw, th, Pixmap.Format.RGBA8888);
        flat.setBlending(Pixmap.Blending.SourceOver);
        for (Layer l : app.layers) {
            if (l.visible) flat.drawPixmap(l.pixmap, 0, 0, app.canvasWidth, app.canvasHeight, 0, 0, tw, th);
        }
        if (thumbnail != null) thumbnail.dispose();
        thumbnail = new Texture(flat);
        thumbnail.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        flat.dispose();
    }

    public void dispose() {
        if (thumbnail != null) thumbnail.dispose();
    }
}
