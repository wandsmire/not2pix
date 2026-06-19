package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mirwanda.not2pix.Not2Pix;

/**
 * Preferences/Settings dialog. Shows toggles and color swatches for:
 * - Grid on/off
 * - Grid color
 * - Tile grid size
 * - Tile grid color
 * - Background color
 * - Mirror X/Y
 * - Onion skin
 */
public class PreferencesDialog {

    private Not2Pix app;
    public boolean open = false;
    private float dp;
    private float x, y, w, h;
    private float rowH;
    private static final int[] TILE_SIZES = {0, 4, 8, 16, 32};

    public Runnable onGridColor;
    public Runnable onTileGridColor;
    public Runnable onBgColor;
    public Runnable onTileSize;

    public PreferencesDialog(Not2Pix app, float dp) {
        this.app = app;
        this.dp = dp;
        this.rowH = 38 * dp;
        this.w = 220 * dp;
    }

    public void show() {
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();
        this.h = 9 * rowH + 10 * dp; // 9 rows + padding
        this.x = (sw - w) / 2f;
        this.y = (sh - h) / 2f;
        open = true;
    }

    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!open) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Dim
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0, 0, 0, 0.6f);
        sr.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        // Panel
        sr.setColor(0.15f, 0.15f, 0.15f, 0.95f);
        sr.rect(x, y, w, h);
        sr.end();

        // Title
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Preferences", x + 10 * dp, y + h - 8 * dp);
        batch.end();

        float ry = y + h - rowH - 10 * dp; // first row below title
        float swatchSize = 24 * dp;
        float labelX = x + 10 * dp;
        float swatchX = x + w - swatchSize - 10 * dp;

        // Row 0: Grid toggle
        drawRow(sr, batch, font, ry, (app.showGrid ? "[x] " : "[ ] ") + "Grid", null);
        ry -= rowH;

        // Row 1: Grid Color (swatch)
        drawRow(sr, batch, font, ry, "Grid Color", app.gridColor);
        ry -= rowH;

        // Row 2: Tile Grid size
        String tileLabel = app.tileSize == 0 ? "Tile Grid: Off" : "Tile Grid: " + app.tileSize;
        drawRow(sr, batch, font, ry, tileLabel, null);
        ry -= rowH;

        // Row 3: Tile Grid Color (swatch)
        drawRow(sr, batch, font, ry, "Tile Grid Color", app.tileGridColor);
        ry -= rowH;

        // Row 4: Background Color (swatch)
        drawRow(sr, batch, font, ry, "Background Color", app.bgColor);
        ry -= rowH;

        // Row 5: Mirror X
        drawRow(sr, batch, font, ry, (app.mirrorX ? "[x] " : "[ ] ") + "Mirror X", null);
        ry -= rowH;

        // Row 6: Mirror Y
        drawRow(sr, batch, font, ry, (app.mirrorY ? "[x] " : "[ ] ") + "Mirror Y", null);
        ry -= rowH;

        // Row 7: Onion Skin
        drawRow(sr, batch, font, ry, (app.onionSkin ? "[x] " : "[ ] ") + "Onion Skin", null);
        ry -= rowH;

        // Row 8: Close button
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.3f, 0.3f, 0.5f, 1);
        sr.rect(x + w / 2f - 40 * dp, ry + 4 * dp, 80 * dp, rowH - 8 * dp);
        sr.end();
        batch.begin();
        font.setColor(Color.WHITE);
        GlyphLayout gl = new GlyphLayout(font, "Close");
        font.draw(batch, "Close", x + w / 2f - gl.width / 2f, ry + rowH / 2f + gl.height / 2f);
        batch.end();
    }

    private void drawRow(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, float ry, String label, Color swatch) {
        float swatchSize = 24 * dp;
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, label, x + 10 * dp, ry + rowH / 2f + font.getCapHeight() / 2f);
        batch.end();
        if (swatch != null) {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            // Checkerboard behind swatch
            float sx = x + w - swatchSize - 10 * dp;
            float sy = ry + (rowH - swatchSize) / 2f;
            sr.setColor(Color.LIGHT_GRAY); sr.rect(sx, sy, swatchSize / 2f, swatchSize / 2f);
            sr.setColor(Color.DARK_GRAY); sr.rect(sx + swatchSize / 2f, sy, swatchSize / 2f, swatchSize / 2f);
            sr.setColor(Color.DARK_GRAY); sr.rect(sx, sy + swatchSize / 2f, swatchSize / 2f, swatchSize / 2f);
            sr.setColor(Color.LIGHT_GRAY); sr.rect(sx + swatchSize / 2f, sy + swatchSize / 2f, swatchSize / 2f, swatchSize / 2f);
            sr.setColor(swatch);
            sr.rect(sx, sy, swatchSize, swatchSize);
            sr.end();
        }
    }

    public boolean handleTouch(float tx, float ty) {
        if (!open) return false;
        // Outside dialog = close
        if (tx < x || tx > x + w || ty < y || ty > y + h) { open = false; return true; }

        float ry = y + h - rowH - 10 * dp;
        int row = (int) ((y + h - 10 * dp - ty) / rowH);
        if (row < 0 || row > 8) return true;

        switch (row) {
            case 0: app.showGrid = !app.showGrid; break;
            case 1: if (onGridColor != null) onGridColor.run(); open = false; break;
            case 2: // Cycle tile size
                int cur = 0;
                for (int i = 0; i < TILE_SIZES.length; i++) if (TILE_SIZES[i] == app.tileSize) { cur = i; break; }
                app.tileSize = TILE_SIZES[(cur + 1) % TILE_SIZES.length];
                break;
            case 3: if (onTileGridColor != null) onTileGridColor.run(); open = false; break;
            case 4: if (onBgColor != null) onBgColor.run(); open = false; break;
            case 5: app.mirrorX = !app.mirrorX; break;
            case 6: app.mirrorY = !app.mirrorY; break;
            case 7: app.onionSkin = !app.onionSkin; break;
            case 8: open = false; break;
        }
        return true;
    }
}
