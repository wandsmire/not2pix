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

    // Which slider is being dragged: 0=none, 6=toolsPos, 7=colorPos
    private int dragSlider = 0;

    public Runnable onGridColor;
    public Runnable onTileGridColor;
    public Runnable onBgColor;
    public Runnable onTileSize;
    public Runnable onCheckerLight;
    public Runnable onCheckerDark;
    public Runnable onMinimapSize;

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
        dragSlider = 0;
        open = true;
    }

    /** Returns slider track rect for a given row's left edge, width, etc. */
    private float[] sliderTrack(float ry) {
        float trackX = x + 10 * dp;
        float trackW = w - 20 * dp;
        float trackY = ry + rowH / 2f - 3 * dp;
        float trackH = 6 * dp;
        return new float[]{trackX, trackY, trackW, trackH};
    }

    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!open) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Dim background
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

        // Row 0: Grid Color
        drawRow(sr, batch, font, ry, "Grid Color", app.gridColor);
        ry -= rowH;

        // Row 1: Tile Grid Color
        drawRow(sr, batch, font, ry, "Tile Grid Color", app.tileGridColor);
        ry -= rowH;

        // Row 2: Background Color
        drawRow(sr, batch, font, ry, "Background Color", app.bgColor);
        ry -= rowH;

        // Row 3: Checker Light
        drawRow(sr, batch, font, ry, "Checker Light", app.checkerLight);
        ry -= rowH;

        // Row 4: Checker Dark
        drawRow(sr, batch, font, ry, "Checker Dark", app.checkerDark);
        ry -= rowH;

        // Row 5: Minimap Size
        drawRow(sr, batch, font, ry, "Minimap Size: " + app.minimapSize, null);
        ry -= rowH;

        // Row 6: Tools position slider
        drawSliderRow(sr, batch, font, ry, "Tools Y Pos", app.toolsPosition);
        ry -= rowH;

        // Row 7: Color bar position slider
        drawSliderRow(sr, batch, font, ry, "Colors Y Pos", app.colorPosition);
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
            float sx = x + w - swatchSize - 10 * dp;
            float sy = ry + (rowH - swatchSize) / 2f;
            // Checkerboard
            sr.setColor(Color.LIGHT_GRAY); sr.rect(sx, sy, swatchSize / 2f, swatchSize / 2f);
            sr.setColor(Color.DARK_GRAY);  sr.rect(sx + swatchSize / 2f, sy, swatchSize / 2f, swatchSize / 2f);
            sr.setColor(Color.DARK_GRAY);  sr.rect(sx, sy + swatchSize / 2f, swatchSize / 2f, swatchSize / 2f);
            sr.setColor(Color.LIGHT_GRAY); sr.rect(sx + swatchSize / 2f, sy + swatchSize / 2f, swatchSize / 2f, swatchSize / 2f);
            sr.setColor(swatch);
            sr.rect(sx, sy, swatchSize, swatchSize);
            sr.end();
        }
    }

    private void drawSliderRow(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, float ry, String label, float value) {
        // Label
        batch.begin();
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, label, x + 10 * dp, ry + rowH - 6 * dp);
        batch.end();

        // Track
        float[] t = sliderTrack(ry);
        float trackX = t[0], trackY = t[1], trackW = t[2], trackH = t[3];

        sr.begin(ShapeRenderer.ShapeType.Filled);
        // Track background
        sr.setColor(0.3f, 0.3f, 0.3f, 1f);
        sr.rect(trackX, trackY, trackW, trackH);
        // Track fill (left portion)
        sr.setColor(0.4f, 0.6f, 1f, 1f);
        sr.rect(trackX, trackY, trackW * value, trackH);
        // Thumb circle (drawn as a square since ShapeRenderer circle needs segments)
        float thumbR = 8 * dp;
        float thumbX = trackX + trackW * value - thumbR;
        float thumbY = trackY + trackH / 2f - thumbR;
        sr.setColor(1f, 1f, 1f, 1f);
        sr.rect(thumbX, thumbY, thumbR * 2, thumbR * 2);
        sr.end();
    }

    public boolean handleTouch(float tx, float ty) {
        if (!open) return false;
        // Outside dialog = close
        if (tx < x || tx > x + w || ty < y || ty > y + h) { open = false; return true; }

        int row = (int) ((y + h - 10 * dp - ty) / rowH);
        if (row < 0 || row > 8) return true;

        switch (row) {
            case 0: if (onGridColor != null) onGridColor.run(); open = false; break;
            case 1: if (onTileGridColor != null) onTileGridColor.run(); open = false; break;
            case 2: if (onBgColor != null) onBgColor.run(); open = false; break;
            case 3: if (onCheckerLight != null) onCheckerLight.run(); open = false; break;
            case 4: if (onCheckerDark != null) onCheckerDark.run(); open = false; break;
            case 5: if (onMinimapSize != null) onMinimapSize.run(); open = false; break;
            case 6: dragSlider = 6; applySliderValue(6, tx); break;
            case 7: dragSlider = 7; applySliderValue(7, tx); break;
            case 8: open = false; break;
        }
        return true;
    }

    public void handleDrag(float tx, float ty) {
        if (!open || dragSlider == 0) return;
        applySliderValue(dragSlider, tx);
    }

    public void handleUp() {
        if (dragSlider != 0) {
            app.savePrefs();
            dragSlider = 0;
        }
    }

    /** Maps touch x to a 0..1 value and stores it in the appropriate preference. */
    private void applySliderValue(int slider, float tx) {
        float[] t = sliderTrackForRow(slider);
        if (t == null) return;
        float trackX = t[0], trackW = t[2];
        float value = (tx - trackX) / trackW;
        value = Math.max(0f, Math.min(1f, value));
        if (slider == 6) app.toolsPosition = value;
        else if (slider == 7) app.colorPosition = value;
    }

    /** Returns sliderTrack for a given row index (0-based). */
    private float[] sliderTrackForRow(int rowIndex) {
        // Row y = y + h - (rowIndex+1)*rowH - 10*dp
        float ry = y + h - (rowIndex + 1) * rowH - 10 * dp;
        return sliderTrack(ry);
    }
}
