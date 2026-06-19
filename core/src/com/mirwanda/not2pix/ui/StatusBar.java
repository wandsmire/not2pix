package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mirwanda.not2pix.Not2Pix;

/**
 * Top status bar (notch area). Shows app name on left, image size on right.
 */
public class StatusBar extends UIPanel {

    private Not2Pix app;
    private float dp;

    public StatusBar(Not2Pix app, float screenWidth, float screenHeight, float dp) {
        super(0, screenHeight - 28 * dp, screenWidth, 28 * dp);
        this.app = app;
        this.dp = dp;
        this.bgColor = new Color(0.1f, 0.1f, 0.1f, 0.95f);
    }

    @Override
    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!visible) return;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(bgColor);
        sr.rect(x, y, width, height);
        sr.end();

        batch.begin();
        // App name left (inset for curved edges)
        font.setColor(Color.WHITE);
        font.draw(batch, "Not2Pix", x + 20 * dp, y + height - 6 * dp);
        // Image size right (inset for curved edges)
        String size = app.canvasWidth + "x" + app.canvasHeight;
        GlyphLayout gl = new GlyphLayout(font, size);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, size, x + width - gl.width - 20 * dp, y + height - 6 * dp);
        batch.end();
    }
}
