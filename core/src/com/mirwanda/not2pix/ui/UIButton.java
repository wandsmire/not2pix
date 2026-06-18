package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class UIButton extends UIElement {

    public String label;
    public boolean selected = false;
    public boolean toggle = false;
    public Color bgColor = new Color(0.22f, 0.22f, 0.22f, 1);
    public Color selectedColor = new Color(0.45f, 0.55f, 0.75f, 1);
    public Color textColor = Color.WHITE;
    public Runnable action;
    /** If set, draw this icon via a callback instead of text */
    public IconDrawer iconDrawer;

    public UIButton(String label, float x, float y, float w, float h) {
        this.label = label;
        this.x = x; this.y = y; this.width = w; this.height = h;
    }

    @Override
    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!visible) return;
        // Background
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(selected ? selectedColor : bgColor);
        sr.rect(x, y, width, height);
        sr.end();

        // Border
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.35f, 0.35f, 0.35f, 1);
        sr.rect(x, y, width, height);
        sr.end();

        // Icon or text
        if (iconDrawer != null) {
            iconDrawer.draw(sr, x, y, width, height, selected);
        } else if (font != null && label != null) {
            batch.begin();
            font.setColor(textColor);
            GlyphLayout gl = new GlyphLayout(font, label);
            font.draw(batch, label, x + (width - gl.width) / 2f, y + (height + gl.height) / 2f);
            batch.end();
        }
    }

    public interface IconDrawer {
        void draw(ShapeRenderer sr, float x, float y, float w, float h, boolean selected);
    }
}
