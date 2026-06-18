package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.ArrayList;

/**
 * A panel that contains child UI elements and draws a background.
 */
public class UIPanel extends UIElement {

    public ArrayList<UIElement> children = new ArrayList<>();
    public Color bgColor = new Color(0.16f, 0.16f, 0.16f, 0.95f);

    public UIPanel(float x, float y, float w, float h) {
        this.x = x; this.y = y; this.width = w; this.height = h;
    }

    @Override
    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!visible) return;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(bgColor);
        sr.rect(x, y, width, height);
        sr.end();
        for (UIElement child : children) {
            child.draw(sr, batch, font);
        }
    }

    /** Check if touch hits any child, return it */
    public UIElement hitChild(float touchX, float touchY) {
        if (!visible || !hit(touchX, touchY)) return null;
        for (int i = children.size() - 1; i >= 0; i--) {
            UIElement c = children.get(i);
            if (c.hit(touchX, touchY)) return c;
        }
        return this; // hit panel background
    }
}
