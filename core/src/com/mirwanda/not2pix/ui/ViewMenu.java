package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mirwanda.not2pix.Not2Pix;

public class ViewMenu {

    private Not2Pix app;
    public boolean open = false;
    private float x, y, w, h;
    private float itemH;
    private float dp;
    private static final int[] TILE_SIZES = {0, 4, 8, 16, 32};

    public ViewMenu(Not2Pix app, float dp) {
        this.app = app;
        this.dp = dp;
        this.w = 130 * dp;
        this.itemH = 40 * dp;
        this.h = 3 * itemH;
    }

    public void show(float bx, float by) {
        this.x = bx;
        this.y = by;
        open = true;
    }

    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!open) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0, 0, 0, 0.5f);
        sr.rect(0, 0, sw, sh);
        sr.setColor(0.18f, 0.18f, 0.18f, 1);
        sr.rect(x, y, w, h);
        sr.end();

        String tileLabel = app.tileSize == 0 ? "Tile Grid: Off" : "Tile Grid: " + app.tileSize;
        String[] labels = {"Fit to Width", (app.showGrid ? "\u2713 " : "  ") + "Grid", tileLabel};
        batch.begin();
        for (int i = 0; i < labels.length; i++) {
            float iy = y + h - (i + 1) * itemH;
            boolean active = (i == 1 && app.showGrid) || (i == 2 && app.tileSize > 0);
            font.setColor(active ? Color.CYAN : Color.WHITE);
            GlyphLayout gl = new GlyphLayout(font, labels[i]);
            font.draw(batch, labels[i], x + 10 * dp, iy + (itemH + gl.height) / 2f);
        }
        batch.end();
    }

    public boolean handleTouch(float tx, float ty) {
        if (!open) return false;
        if (tx >= x && tx <= x + w && ty >= y && ty <= y + h) {
            int idx = (int) ((y + h - ty) / itemH);
            if (idx == 0) app.fitToWidth();
            else if (idx == 1) app.showGrid = !app.showGrid;
            else if (idx == 2) {
                // Cycle tile size
                int cur = 0;
                for (int i = 0; i < TILE_SIZES.length; i++) {
                    if (TILE_SIZES[i] == app.tileSize) { cur = i; break; }
                }
                app.tileSize = TILE_SIZES[(cur + 1) % TILE_SIZES.length];
            }
        }
        open = false;
        return true;
    }
}
