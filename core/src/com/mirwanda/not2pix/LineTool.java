package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

public class LineTool implements Tool {

    private int startX, startY;
    public Pixmap preview;
    public boolean dragging = false;

    @Override public String getName() { return "Line"; }

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
        PencilTool.drawLine(preview, startX, startY, x, y, 1);
    }

    @Override
    public void onUp(Pixmap target, int x, int y, Color color) {
        target.setColor(color);
        PencilTool.drawLine(target, startX, startY, x, y, 1);
        disposePreview();
        dragging = false;
    }

    public void disposePreview() {
        if (preview != null) { preview.dispose(); preview = null; }
    }
}
