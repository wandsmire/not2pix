package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

public class SelectionTool implements Tool {

    public boolean hasSelection = false;
    public boolean drawing = false; // true while dragging out a new selection rect
    public int selX, selY, selW, selH;
    private int startX, startY;
    private boolean moving = false;
    private int moveStartX, moveStartY;

    public Pixmap buffer;
    private Texture bufferTexture;
    private int origX, origY;

    public float rotationDeg = 0;
    public boolean freeRotateMode = false;
    public boolean isImported = false;

    @Override public String getName() { return "Select"; }

    @Override
    public void onDown(Pixmap target, int x, int y, Color color) {
        if (buffer != null) {
            if (freeRotateMode) {
                // Only track start for rotation drag, don't reposition
                moving = true;
                moveStartX = x;
                moveStartY = y;
            } else {
                // Already cut — tap to place at pointer (centered)
                selX = x - selW / 2;
                selY = y - selH / 2;
                moving = true;
                moveStartX = x;
                moveStartY = y;
            }
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
            drawing = true;
        }
    }

    @Override
    public void onDrag(Pixmap target, int x, int y, Color color) {
        if (moving) {
            int dx = x - moveStartX;
            int dy = y - moveStartY;
            if (freeRotateMode && buffer != null) {
                rotationDeg += dx * 5; // horizontal drag rotates
            } else {
                selX += dx;
                selY += dy;
            }
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
        drawing = false;
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
            if (rotationDeg != 0) applyRotation();
            target.setBlending(Pixmap.Blending.SourceOver);
            target.drawPixmap(buffer, selX, selY);
            disposeBuffer();
        }
        hasSelection = false;
        moving = false;
        rotationDeg = 0;
        freeRotateMode = false;
        isImported = false;
    }

    public void cancelSelection(Pixmap target) {
        if (buffer != null) {
            if (!isImported) {
                target.setBlending(Pixmap.Blending.None);
                target.drawPixmap(buffer, origX, origY);
                target.setBlending(Pixmap.Blending.SourceOver);
            }
            disposeBuffer();
        }
        hasSelection = false;
        moving = false;
        rotationDeg = 0;
        freeRotateMode = false;
        isImported = false;
    }

    /** Copy buffer back at current position without erasing it */
    public void copySelection(Pixmap target) {
        if (buffer == null) return;
        target.setBlending(Pixmap.Blending.SourceOver);
        target.drawPixmap(buffer, selX, selY);
    }

    /** Rotate buffer 90° clockwise */
    public void rotate90() {
        if (buffer == null) return;
        int w = buffer.getWidth(), h = buffer.getHeight();
        Pixmap rotated = new Pixmap(h, w, Pixmap.Format.RGBA8888);
        rotated.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                rotated.drawPixel(h - 1 - y, x, buffer.getPixel(x, y));
        buffer.dispose();
        buffer = rotated;
        int tmp = selW; selW = selH; selH = tmp;
        disposeBufferTexture();
    }

    /** Mirror buffer horizontally */
    public void mirrorH() {
        if (buffer == null) return;
        int w = buffer.getWidth(), h = buffer.getHeight();
        Pixmap mirrored = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        mirrored.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                mirrored.drawPixel(w - 1 - x, y, buffer.getPixel(x, y));
        buffer.dispose();
        buffer = mirrored;
        disposeBufferTexture();
    }

    /** Mirror buffer vertically */
    public void mirrorV() {
        if (buffer == null) return;
        int w = buffer.getWidth(), h = buffer.getHeight();
        Pixmap mirrored = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        mirrored.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                mirrored.drawPixel(x, h - 1 - y, buffer.getPixel(x, y));
        buffer.dispose();
        buffer = mirrored;
        disposeBufferTexture();
    }

    /** Apply free rotation (rotationDeg) to buffer pixels */
    private void applyRotation() {
        if (buffer == null || rotationDeg == 0) return;
        int w = buffer.getWidth(), h = buffer.getHeight();
        // Compute bounding box of rotated image
        double rad = Math.toRadians(rotationDeg);
        double cos = Math.cos(rad), sin = Math.sin(rad);
        double absCos = Math.abs(cos), absSin = Math.abs(sin);
        int nw = (int) Math.ceil(w * absCos + h * absSin);
        int nh = (int) Math.ceil(w * absSin + h * absCos);
        Pixmap rotated = new Pixmap(nw, nh, Pixmap.Format.RGBA8888);
        rotated.setBlending(Pixmap.Blending.None);
        float cx = w / 2f, cy = h / 2f;
        float ncx = nw / 2f, ncy = nh / 2f;
        for (int dy = 0; dy < nh; dy++) {
            for (int dx = 0; dx < nw; dx++) {
                float rx = dx - ncx, ry = dy - ncy;
                int sx = (int) (rx * cos + ry * sin + cx);
                int sy = (int) (-rx * sin + ry * cos + cy);
                if (sx >= 0 && sx < w && sy >= 0 && sy < h) {
                    rotated.drawPixel(dx, dy, buffer.getPixel(sx, sy));
                }
            }
        }
        buffer.dispose();
        buffer = rotated;
        // Keep the center of the selection at the same position
        float oldCenterX = selX + (w - 1) * 0.5f;
        float oldCenterY = selY + (h - 1) * 0.5f;
        selX = Math.round(oldCenterX - (nw - 1) * 0.5f) - 1;
        selY = Math.round(oldCenterY - (nh - 1) * 0.5f);
        selW = nw;
        selH = nh;
        disposeBufferTexture();
    }

    /** Apply outline around non-transparent pixels in buffer.
     *  Outline is drawn outside the opaque area, never on top of existing pixels. */
    public void applyOutline(Color outlineColor, int thickness) {
        if (buffer == null) return;
        int w = buffer.getWidth(), h = buffer.getHeight();
        // Expand buffer to accommodate outline
        int nw = w + thickness * 2, nh = h + thickness * 2;
        Pixmap result = new Pixmap(nw, nh, Pixmap.Format.RGBA8888);
        result.setBlending(Pixmap.Blending.None);
        int outRGBA = Color.rgba8888(outlineColor);
        // For each pixel in new buffer, check if it's within 'thickness' of any opaque pixel
        for (int ny = 0; ny < nh; ny++) {
            for (int nx = 0; nx < nw; nx++) {
                int ox = nx - thickness, oy = ny - thickness;
                // If original pixel is opaque, skip (don't draw outline on existing pixels)
                if (ox >= 0 && ox < w && oy >= 0 && oy < h) {
                    int px = buffer.getPixel(ox, oy);
                    if ((px & 0xFF) != 0) continue;
                }
                // Check if any opaque pixel within radius
                boolean near = false;
                for (int dy = -thickness; dy <= thickness && !near; dy++) {
                    for (int dx = -thickness; dx <= thickness && !near; dx++) {
                        if (dx * dx + dy * dy > thickness * thickness) continue;
                        int sx = ox + dx, sy = oy + dy;
                        if (sx >= 0 && sx < w && sy >= 0 && sy < h) {
                            if ((buffer.getPixel(sx, sy) & 0xFF) != 0) near = true;
                        }
                    }
                }
                if (near) result.drawPixel(nx, ny, outRGBA);
            }
        }
        // Draw original on top of outline
        result.setBlending(Pixmap.Blending.SourceOver);
        result.drawPixmap(buffer, thickness, thickness);
        buffer.dispose();
        buffer = result;
        selX -= thickness;
        selY -= thickness;
        selW = nw;
        selH = nh;
        disposeBufferTexture();
    }

    private void disposeBuffer() {
        disposeBufferTexture();
        if (buffer != null) { buffer.dispose(); buffer = null; }
    }

    public void disposeBufferTexture() {
        if (bufferTexture != null) { bufferTexture.dispose(); bufferTexture = null; }
    }

    public void dispose() { disposeBuffer(); }
}
