package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

public class RectTool implements Tool {

    private int startX, startY;
    public Pixmap preview;
    public boolean dragging = false;

    @Override public String getName() { return "Rect"; }

    @Override
    public void onDown(Pixmap target, int x, int y, Color color) {
        startX = x; startY = y;
        dragging = false;
        disposePreview();
        preview = new Pixmap(target.getWidth(), target.getHeight(), Pixmap.Format.RGBA8888);
        preview.setBlending(Pixmap.Blending.None);
        preview.setColor(Color.CLEAR);
        preview.fill();
        preview.setBlending(Pixmap.Blending.SourceOver);
    }

    @Override
    public void onDrag(Pixmap target, int x, int y, Color color) {
        if (preview == null) return;
        dragging = true;
        preview.setBlending(Pixmap.Blending.None);
        preview.setColor(Color.CLEAR);
        preview.fill();
        preview.setBlending(Pixmap.Blending.SourceOver);
        preview.setColor(color);
        drawRect(preview, startX, startY, x, y);
    }

    @Override
    public void onUp(Pixmap target, int x, int y, Color color) {
        target.setColor(color);
        drawRect(target, startX, startY, x, y);
        disposePreview();
        dragging = false;
    }

    private void drawRect(Pixmap pm, int x0, int y0, int x1, int y1) {
        int minX = Math.min(x0, x1), maxX = Math.max(x0, x1);
        int minY = Math.min(y0, y1), maxY = Math.max(y0, y1);
        for (int x = minX; x <= maxX; x++) { pm.drawPixel(x, minY); pm.drawPixel(x, maxY); }
        for (int y = minY; y <= maxY; y++) { pm.drawPixel(minX, y); pm.drawPixel(maxX, y); }
    }

    public void disposePreview() {
        if (preview != null) { preview.dispose(); preview = null; }
    }
}
