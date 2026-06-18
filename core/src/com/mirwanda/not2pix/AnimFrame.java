package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/**
 * A single animation frame, storing its own pixmap snapshot.
 */
public class AnimFrame {

    public Pixmap pixmap;
    public Texture texture;

    public AnimFrame(int width, int height) {
        pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.CLEAR);
        pixmap.fill();
        refreshTexture();
    }

    public void refreshTexture() {
        if (texture != null) texture.dispose();
        texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    public void dispose() {
        if (texture != null) texture.dispose();
        if (pixmap != null) pixmap.dispose();
    }
}
