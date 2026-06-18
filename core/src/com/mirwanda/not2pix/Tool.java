package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

public interface Tool {
    String getName();
    void onDown(Pixmap target, int x, int y, Color color);
    void onDrag(Pixmap target, int x, int y, Color color);
    void onUp(Pixmap target, int x, int y, Color color);
}
