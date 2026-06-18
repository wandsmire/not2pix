package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mirwanda.not2pix.Not2Pix;

public class TileSizePopup {

    private Not2Pix app;
    public boolean open = false;
    private float x, y, w, h;
    private float dp;
    private float btnSize;

    public TileSizePopup(Not2Pix app, float dp) {
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

    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!open) return;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.18f, 0.18f, 0.18f, 1);
        sr.rect(x, y, w, h);
        sr.end();

        float bx = x + 8 * dp;
        float by2 = y + (h - btnSize) / 2f;
        // - button
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.3f, 0.3f, 0.3f, 1);
        sr.rect(bx, by2, btnSize, btnSize);
        sr.end();

        batch.begin();
        font.setColor(Color.WHITE);
        GlyphLayout gl = new GlyphLayout(font, "-");
        font.draw(batch, "-", bx + (btnSize - gl.width) / 2f, by2 + (btnSize + gl.height) / 2f);
        String label = app.tileSize == 0 ? "Off" : String.valueOf(app.tileSize);
        gl.setText(font, label);
        font.draw(batch, label, x + w / 2f - gl.width / 2f, y + h / 2f + gl.height / 2f);
        batch.end();

        // + button
        bx = x + w - btnSize - 8 * dp;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.3f, 0.3f, 0.3f, 1);
        sr.rect(bx, by2, btnSize, btnSize);
        sr.end();
        batch.begin();
        font.setColor(Color.WHITE);
        gl.setText(font, "+");
        font.draw(batch, "+", bx + (btnSize - gl.width) / 2f, by2 + (btnSize + gl.height) / 2f);
        batch.end();
    }

    public boolean handleTouch(float tx, float ty) {
        if (!open) return false;
        if (tx < x || tx > x + w || ty < y || ty > y + h) {
            open = false;
            return true;
        }
        float bx = x + 8 * dp;
        float by2 = y + (h - btnSize) / 2f;
        if (tx >= bx && tx <= bx + btnSize && ty >= by2 && ty <= by2 + btnSize) {
            app.tileSize = Math.max(0, app.tileSize - 1);
            return true;
        }
        bx = x + w - btnSize - 8 * dp;
        if (tx >= bx && tx <= bx + btnSize && ty >= by2 && ty <= by2 + btnSize) {
            app.tileSize = Math.min(128, app.tileSize + 1);
            return true;
        }
        return true;
    }
}
