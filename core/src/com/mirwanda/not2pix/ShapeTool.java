package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Shape tool: Line, Rectangle, Diamond, Ellipse. Preview drawn with ShapeRenderer.
 */
public class ShapeTool implements Tool {

    public enum Shape { LINE, RECT, DIAMOND, ELLIPSE }
    public Shape currentShape = Shape.LINE;

    private int startX, startY;
    private int endX, endY;
    public boolean dragging = false;

    @Override public String getName() { return "Shape"; }

    @Override
    public void onDown(Pixmap target, int x, int y, Color color) {
        startX = x; startY = y;
        endX = x; endY = y;
        dragging = false;
    }

    @Override
    public void onDrag(Pixmap target, int x, int y, Color color) {
        endX = x; endY = y;
        dragging = true;
    }

    @Override
    public void onUp(Pixmap target, int x, int y, Color color) {
        target.setColor(color);
        drawShape(target, startX, startY, x, y);
        dragging = false;
    }

    /** Draw preview using ShapeRenderer in world coords (y-up, pixel units) */
    public void drawPreviewSR(ShapeRenderer sr, int canvasW, int canvasH) {
        // Convert pixmap Y (top-down) to world Y (bottom-up)
        float wx0 = startX + 0.5f, wy0 = canvasH - startY - 0.5f;
        float wx1 = endX + 0.5f, wy1 = canvasH - endY - 0.5f;
        switch (currentShape) {
            case LINE:
                sr.rectLine(wx0, wy0, wx1, wy1, 0.5f);
                break;
            case RECT:
                float minX = Math.min(wx0, wx1), minY = Math.min(wy0, wy1);
                float maxX = Math.max(wx0, wx1), maxY = Math.max(wy0, wy1);
                sr.rectLine(minX, minY, maxX, minY, 0.5f);
                sr.rectLine(maxX, minY, maxX, maxY, 0.5f);
                sr.rectLine(maxX, maxY, minX, maxY, 0.5f);
                sr.rectLine(minX, maxY, minX, minY, 0.5f);
                break;
            case DIAMOND:
                float cx = (wx0 + wx1) / 2f, cy = (wy0 + wy1) / 2f;
                float hw = Math.abs(wx1 - wx0) / 2f, hh = Math.abs(wy1 - wy0) / 2f;
                sr.rectLine(cx, cy + hh, cx + hw, cy, 0.5f);
                sr.rectLine(cx + hw, cy, cx, cy - hh, 0.5f);
                sr.rectLine(cx, cy - hh, cx - hw, cy, 0.5f);
                sr.rectLine(cx - hw, cy, cx, cy + hh, 0.5f);
                break;
            case ELLIPSE:
                float ecx = (wx0 + wx1) / 2f, ecy = (wy0 + wy1) / 2f;
                float erx = Math.abs(wx1 - wx0) / 2f, ery = Math.abs(wy1 - wy0) / 2f;
                if (erx < 0.5f) erx = 0.5f;
                if (ery < 0.5f) ery = 0.5f;
                int segs = 32;
                for (int i = 0; i < segs; i++) {
                    float a0 = (float) (i * 2 * Math.PI / segs);
                    float a1 = (float) ((i + 1) * 2 * Math.PI / segs);
                    sr.rectLine(ecx + erx * (float)Math.cos(a0), ecy + ery * (float)Math.sin(a0),
                                ecx + erx * (float)Math.cos(a1), ecy + ery * (float)Math.sin(a1), 0.5f);
                }
                break;
        }
    }

    private void drawShape(Pixmap pm, int x0, int y0, int x1, int y1) {
        switch (currentShape) {
            case LINE: PencilTool.drawLine(pm, x0, y0, x1, y1, 1); break;
            case RECT: drawRect(pm, x0, y0, x1, y1); break;
            case DIAMOND: drawDiamond(pm, x0, y0, x1, y1); break;
            case ELLIPSE: drawEllipse(pm, x0, y0, x1, y1); break;
        }
    }

    private void drawRect(Pixmap pm, int x0, int y0, int x1, int y1) {
        int minX = Math.min(x0, x1), maxX = Math.max(x0, x1);
        int minY = Math.min(y0, y1), maxY = Math.max(y0, y1);
        for (int x = minX; x <= maxX; x++) { pm.drawPixel(x, minY); pm.drawPixel(x, maxY); }
        for (int y = minY; y <= maxY; y++) { pm.drawPixel(minX, y); pm.drawPixel(maxX, y); }
    }

    private void drawDiamond(Pixmap pm, int x0, int y0, int x1, int y1) {
        int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
        int hw = Math.abs(x1 - x0) / 2, hh = Math.abs(y1 - y0) / 2;
        PencilTool.drawLine(pm, cx, cy - hh, cx + hw, cy, 1);
        PencilTool.drawLine(pm, cx + hw, cy, cx, cy + hh, 1);
        PencilTool.drawLine(pm, cx, cy + hh, cx - hw, cy, 1);
        PencilTool.drawLine(pm, cx - hw, cy, cx, cy - hh, 1);
    }

    private void drawEllipse(Pixmap pm, int x0, int y0, int x1, int y1) {
        int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
        int rx = Math.abs(x1 - x0) / 2, ry = Math.abs(y1 - y0) / 2;
        if (rx == 0 && ry == 0) { pm.drawPixel(cx, cy); return; }
        if (rx == 0) { PencilTool.drawLine(pm, cx, cy - ry, cx, cy + ry, 1); return; }
        if (ry == 0) { PencilTool.drawLine(pm, cx - rx, cy, cx + rx, cy, 1); return; }
        int x = 0, y = ry;
        long rx2 = (long)rx * rx, ry2 = (long)ry * ry;
        long err = ry2 - rx2 * ry + rx2 / 4;
        while (ry2 * x <= rx2 * y) {
            pm.drawPixel(cx + x, cy + y); pm.drawPixel(cx - x, cy + y);
            pm.drawPixel(cx + x, cy - y); pm.drawPixel(cx - x, cy - y);
            if (err < 0) { err += ry2 * (2 * x + 3); } else { err += ry2 * (2 * x + 3) + rx2 * (-2 * y + 2); y--; }
            x++;
        }
        err = ry2 * (x * x + x) + rx2 * (y * y - y) - rx2 * ry2;
        while (y >= 0) {
            pm.drawPixel(cx + x, cy + y); pm.drawPixel(cx - x, cy + y);
            pm.drawPixel(cx + x, cy - y); pm.drawPixel(cx - x, cy - y);
            if (err > 0) { err += rx2 * (-2 * y + 3); } else { err += ry2 * (2 * x + 2) + rx2 * (-2 * y + 3); x++; }
            y--;
        }
    }
}
