package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
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
        this.maxSize = 96 * dp;
    }

    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
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
        float topOffset = 52 * dp + 28 * dp + 28 * dp; // frame strip + status bar + doc strip
        my = sh - topOffset - drawH - 6 * dp;

        frameCounter++;
        if (frameCounter >= 15) {
            frameCounter = 0;
            updateThumbnail();
        }

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(1, 1, 1, 1);
        sr.rect(mx, my, drawW, drawH);
        sr.end();

        if (thumbnail != null) {
            batch.begin();
            batch.draw(thumbnail, mx, my, drawW, drawH);
            batch.end();
        }
    }

    public boolean hit(float tx, float ty) {
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
