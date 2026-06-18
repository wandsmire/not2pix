package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

public class SelectionTool implements Tool {

    public boolean hasSelection = false;
    public int selX, selY, selW, selH;
    private int startX, startY;
    private boolean moving = false;
    private int moveStartX, moveStartY;

    public Pixmap buffer;
    private Texture bufferTexture;
    private int origX, origY;

    @Override public String getName() { return "Select"; }

    @Override
    public void onDown(Pixmap target, int x, int y, Color color) {
        if (buffer != null) {
            // Already cut — tap to place at pointer (centered)
            selX = x - selW / 2;
            selY = y - selH / 2;
            moving = true;
            moveStartX = x;
            moveStartY = y;
        } else if (hasSelection && insideSelection(x, y)) {
            // Cut region into buffer and start moving
            moving = true;
            moveStartX = x;
            moveStartY = y;
            origX = selX;
            origY = selY;
            buffer = new Pixmap(selW, selH, Pixmap.Format.RGBA8888);
            buffer.setBlending(Pixmap.Blending.None);
            buffer.drawPixmap(target, 0, 0, selX, selY, selW, selH);
            target.setBlending(Pixmap.Blending.None);
            target.setColor(Color.CLEAR);
            target.fillRectangle(selX, selY, selW, selH);
            target.setBlending(Pixmap.Blending.SourceOver);
            disposeBufferTexture();
        } else {
            startX = x;
            startY = y;
            hasSelection = false;
            moving = false;
        }
    }

    @Override
    public void onDrag(Pixmap target, int x, int y, Color color) {
        if (moving) {
            int dx = x - moveStartX;
            int dy = y - moveStartY;
            selX += dx;
            selY += dy;
            moveStartX = x;
            moveStartY = y;
        } else {
            selX = Math.min(startX, x);
            selY = Math.min(startY, y);
            selW = Math.abs(x - startX) + 1;
            selH = Math.abs(y - startY) + 1;
            hasSelection = true;
        }
    }

    @Override
    public void onUp(Pixmap target, int x, int y, Color color) {
        moving = false;
    }

    private boolean insideSelection(int x, int y) {
        return x >= selX && x < selX + selW && y >= selY && y < selY + selH;
    }

    public Texture getBufferTexture() {
        if (buffer == null) return null;
        if (bufferTexture == null) {
            bufferTexture = new Texture(buffer);
            bufferTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        return bufferTexture;
    }

    public void commitSelection(Pixmap target) {
        if (buffer != null) {
            target.setBlending(Pixmap.Blending.SourceOver);
            target.drawPixmap(buffer, selX, selY);
            disposeBuffer();
        }
        hasSelection = false;
        moving = false;
    }

    public void cancelSelection(Pixmap target) {
        if (buffer != null) {
            target.setBlending(Pixmap.Blending.None);
            target.drawPixmap(buffer, origX, origY);
            target.setBlending(Pixmap.Blending.SourceOver);
            disposeBuffer();
        }
        hasSelection = false;
        moving = false;
    }

    private void disposeBuffer() {
        disposeBufferTexture();
        if (buffer != null) { buffer.dispose(); buffer = null; }
    }

    private void disposeBufferTexture() {
        if (bufferTexture != null) { bufferTexture.dispose(); bufferTexture = null; }
    }

    public void dispose() { disposeBuffer(); }
}
