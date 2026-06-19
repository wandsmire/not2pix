package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mirwanda.not2pix.Not2Pix;

/**
 * Dialog for resizing the canvas with width/height input and stretch toggle.
 */
public class CanvasResizeDialog {

    private Not2Pix app;
    public boolean open = false;
    private float dp;
    private int newWidth, newHeight;
    private boolean stretch = false;

    public CanvasResizeDialog(Not2Pix app, float dp) {
        this.app = app;
        this.dp = dp;
    }

    public void show() {
        newWidth = app.canvasWidth;
        newHeight = app.canvasHeight;
        stretch = false;
        open = true;
    }

    private float dw() { return 240 * dp; }
    private float dh() { return 220 * dp; }
    private float dx() { return (Gdx.graphics.getWidth() - dw()) / 2f; }
    private float dy() { return (Gdx.graphics.getHeight() - dh()) / 2f; }

    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!open) return;
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0, 0, 0, 0.65f);
        sr.rect(0, 0, sw, sh);
        sr.setColor(0.2f, 0.2f, 0.22f, 1);
        sr.rect(dx(), dy(), dw(), dh());
        sr.end();

        batch.begin();
        font.setColor(Color.WHITE);
        GlyphLayout gl = new GlyphLayout(font, "Resize Canvas");
        font.draw(batch, "Resize Canvas", dx() + (dw() - gl.width) / 2f, dy() + dh() - 14 * dp);
        batch.end();

        float rowH = 42 * dp;
        float rowX = dx() + 10 * dp;
        float valX = dx() + 90 * dp;
        float valW = dw() - 100 * dp;
        float ry = dy() + dh() - 55 * dp;

        // Width row
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.15f, 0.15f, 0.15f, 1);
        sr.rect(valX, ry, valW, 30 * dp);
        sr.end();
        batch.begin();
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Width:", rowX, ry + 22 * dp);
        font.setColor(Color.WHITE);
        font.draw(batch, String.valueOf(newWidth), valX + 8 * dp, ry + 22 * dp);
        batch.end();
        ry -= rowH;

        // Height row
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.15f, 0.15f, 0.15f, 1);
        sr.rect(valX, ry, valW, 30 * dp);
        sr.end();
        batch.begin();
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Height:", rowX, ry + 22 * dp);
        font.setColor(Color.WHITE);
        font.draw(batch, String.valueOf(newHeight), valX + 8 * dp, ry + 22 * dp);
        batch.end();
        ry -= rowH;

        // Stretch toggle
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, (stretch ? "[x] " : "[ ] ") + "Stretch image", rowX, ry + 22 * dp);
        batch.end();
        ry -= rowH;

        // OK / Cancel buttons
        float btnW = 80 * dp, btnH = 34 * dp;
        float btnY = dy() + 12 * dp;
        float okX = dx() + dw() / 2f - btnW - 6 * dp;
        float cnX = dx() + dw() / 2f + 6 * dp;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.3f, 0.6f, 0.3f, 1);
        sr.rect(okX, btnY, btnW, btnH);
        sr.setColor(0.5f, 0.25f, 0.25f, 1);
        sr.rect(cnX, btnY, btnW, btnH);
        sr.end();
        batch.begin();
        font.setColor(Color.WHITE);
        gl.setText(font, "Resize");
        font.draw(batch, "Resize", okX + (btnW - gl.width) / 2f, btnY + (btnH + gl.height) / 2f);
        gl.setText(font, "Cancel");
        font.draw(batch, "Cancel", cnX + (btnW - gl.width) / 2f, btnY + (btnH + gl.height) / 2f);
        batch.end();
    }

    public boolean handleTouch(float tx, float ty) {
        if (!open) return false;

        float rowH = 42 * dp;
        float valX = dx() + 90 * dp;
        float valW = dw() - 100 * dp;
        float ry = dy() + dh() - 55 * dp;

        // Width tap
        if (tx >= valX && tx <= valX + valW && ty >= ry && ty <= ry + 30 * dp) {
            Gdx.input.getTextInput(new Input.TextInputListener() {
                @Override public void input(String text) {
                    try { int v = Integer.parseInt(text.trim()); if (v >= 1 && v <= 4096) newWidth = v; } catch (NumberFormatException e) {}
                }
                @Override public void canceled() {}
            }, "Width", String.valueOf(newWidth), "1-4096");
            return true;
        }
        ry -= rowH;

        // Height tap
        if (tx >= valX && tx <= valX + valW && ty >= ry && ty <= ry + 30 * dp) {
            Gdx.input.getTextInput(new Input.TextInputListener() {
                @Override public void input(String text) {
                    try { int v = Integer.parseInt(text.trim()); if (v >= 1 && v <= 4096) newHeight = v; } catch (NumberFormatException e) {}
                }
                @Override public void canceled() {}
            }, "Height", String.valueOf(newHeight), "1-4096");
            return true;
        }
        ry -= rowH;

        // Stretch toggle
        float rowX = dx() + 10 * dp;
        if (tx >= rowX && tx <= dx() + dw() - 10 * dp && ty >= ry && ty <= ry + 30 * dp) {
            stretch = !stretch;
            return true;
        }

        // OK / Cancel
        float btnW = 80 * dp, btnH = 34 * dp;
        float btnY = dy() + 12 * dp;
        float okX = dx() + dw() / 2f - btnW - 6 * dp;
        float cnX = dx() + dw() / 2f + 6 * dp;

        if (tx >= okX && tx <= okX + btnW && ty >= btnY && ty <= btnY + btnH) {
            open = false;
            if (newWidth != app.canvasWidth || newHeight != app.canvasHeight) {
                app.resizeCanvas(newWidth, newHeight, stretch);
            }
            return true;
        }
        if (tx >= cnX && tx <= cnX + btnW && ty >= btnY && ty <= btnY + btnH) {
            open = false;
            return true;
        }

        return true;
    }
}
