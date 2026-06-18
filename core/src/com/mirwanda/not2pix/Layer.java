package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

public class Layer {

    public String name;
    public Pixmap pixmap;
    public Texture texture;
    public boolean visible = true;
    public float opacity = 1f;
    public boolean dirty = true;

    public Layer(String name, int width, int height) {
        this.name = name;
        this.pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        this.pixmap.setColor(Color.CLEAR);
        this.pixmap.fill();
        refreshTexture();
    }

    public void refreshTexture() {
        if (!dirty && texture != null) return;
        if (texture != null) texture.dispose();
        texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        dirty = false;
    }

    public void markDirty() { dirty = true; }

    public void dispose() {
        if (texture != null) texture.dispose();
        if (pixmap != null) pixmap.dispose();
    }
}
