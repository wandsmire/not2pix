package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class ConfirmDialog {

    public boolean open = false;
    private String message = "";
    private Runnable onConfirm;
    private float dp;

    public ConfirmDialog(float dp) { this.dp = dp; }

    public void show(String message, Runnable onConfirm) {
        this.message = message;
        this.onConfirm = onConfirm;
        this.open = true;
    }

    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!open) return;
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0, 0, 0, 0.6f);
        sr.rect(0, 0, sw, sh);
        float bw = 200 * dp, bh = 100 * dp;
        float bx = (sw - bw) / 2f, by = (sh - bh) / 2f;
        sr.setColor(0.2f, 0.2f, 0.2f, 1);
        sr.rect(bx, by, bw, bh);
        float btnW = 70 * dp, btnH = 30 * dp;
        float yesX = bx + bw * 0.25f - btnW / 2f, noX = bx + bw * 0.75f - btnW / 2f;
        float btnY = by + 12 * dp;
        sr.setColor(0.3f, 0.6f, 0.3f, 1);
        sr.rect(yesX, btnY, btnW, btnH);
        sr.setColor(0.6f, 0.3f, 0.3f, 1);
        sr.rect(noX, btnY, btnW, btnH);
        sr.end();

        batch.begin();
        font.setColor(Color.WHITE);
        GlyphLayout gl = new GlyphLayout(font, message);
        font.draw(batch, message, bx + (bw - gl.width) / 2f, by + bh - 20 * dp);
        gl.setText(font, "Yes");
        font.draw(batch, "Yes", yesX + (btnW - gl.width) / 2f, btnY + (btnH + gl.height) / 2f);
        gl.setText(font, "No");
        font.draw(batch, "No", noX + (btnW - gl.width) / 2f, btnY + (btnH + gl.height) / 2f);
        batch.end();
    }

    public boolean handleTouch(float tx, float ty) {
        if (!open) return false;
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        float bw = 200 * dp, bh = 100 * dp;
        float bx = (sw - bw) / 2f, by = (sh - bh) / 2f;
        float btnW = 70 * dp, btnH = 30 * dp;
        float yesX = bx + bw * 0.25f - btnW / 2f, noX = bx + bw * 0.75f - btnW / 2f;
        float btnY = by + 12 * dp;
        if (ty >= btnY && ty <= btnY + btnH) {
            if (tx >= yesX && tx <= yesX + btnW) { if (onConfirm != null) onConfirm.run(); open = false; return true; }
            if (tx >= noX && tx <= noX + btnW) { open = false; return true; }
        }
        return true;
    }
}
