package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

public class EraserTool implements Tool {

    private int lastX = -1, lastY = -1;
    public int brushSize = 1;

    @Override public String getName() { return "Eraser"; }

    @Override
    public void onDown(Pixmap target, int x, int y, Color color) {
        target.setColor(Color.CLEAR);
        target.setBlending(Pixmap.Blending.None);
        drawDot(target, x, y);
        target.setBlending(Pixmap.Blending.SourceOver);
        lastX = x; lastY = y;
    }

    @Override
    public void onDrag(Pixmap target, int x, int y, Color color) {
        target.setColor(Color.CLEAR);
        target.setBlending(Pixmap.Blending.None);
        if (lastX >= 0) {
            PencilTool.drawLine(target, lastX, lastY, x, y, brushSize);
        } else {
            drawDot(target, x, y);
        }
        target.setBlending(Pixmap.Blending.SourceOver);
        lastX = x; lastY = y;
    }

    @Override
    public void onUp(Pixmap target, int x, int y, Color color) {
        lastX = -1; lastY = -1;
    }

    private void drawDot(Pixmap pm, int cx, int cy) {
        if (brushSize <= 1) {
            pm.drawPixel(cx, cy);
        } else {
            int r = brushSize / 2;
            for (int dy = -r; dy <= r; dy++)
                for (int dx = -r; dx <= r; dx++)
                    if (dx * dx + dy * dy <= r * r)
                        pm.drawPixel(cx + dx, cy + dy);
        }
    }
}
