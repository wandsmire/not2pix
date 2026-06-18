package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Lightweight UI element base. Screen-space coordinates (origin top-left).
 */
public abstract class UIElement {

    public float x, y, width, height;
    public boolean visible = true;

    public boolean hit(float touchX, float touchY) {
        return visible && touchX >= x && touchX <= x + width && touchY >= y && touchY <= y + height;
    }

    public abstract void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font);
}
