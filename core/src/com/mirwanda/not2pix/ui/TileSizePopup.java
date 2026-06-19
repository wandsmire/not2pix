package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.mirwanda.not2pix.Not2Pix;

public class TileSizePopup {

    private Not2Pix app;
    public boolean open = false;

    public TileSizePopup(Not2Pix app, float dp) {
        this.app = app;
    }

    public void show(float bx, float by) {
        open = true;
        Gdx.input.getTextInput(new Input.TextInputListener() {
            @Override
            public void input(String text) {
                open = false;
                try {
                    int val = Integer.parseInt(text.trim());
                    app.tileSize = Math.max(0, Math.min(128, val));
                    app.savePrefs();
                } catch (NumberFormatException e) {
                    // ignore invalid input
                }
            }
            @Override
            public void canceled() { open = false; }
        }, "Tile Grid Size", String.valueOf(app.tileSize), "0 = off, max 128");
    }

    public void draw(com.badlogic.gdx.graphics.glutils.ShapeRenderer sr, com.badlogic.gdx.graphics.g2d.SpriteBatch batch, com.badlogic.gdx.graphics.g2d.BitmapFont font) {
        // Native input, nothing to draw
    }

    public boolean handleTouch(float tx, float ty) {
        return false;
    }
}
