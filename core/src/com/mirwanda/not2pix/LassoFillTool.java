package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import java.util.ArrayList;

/**
 * Lasso Fill tool: draw a freehand outline stroke, and on release
 * the enclosed area is filled with the selected color.
 * Uses scanline polygon fill on the collected path points.
 */
public class LassoFillTool implements Tool {

    public ArrayList<int[]> points = new ArrayList<>();
    private boolean active = false;

    @Override public String getName() { return "Lasso Fill"; }

    @Override
    public void onDown(Pixmap target, int x, int y, Color color) {
        points.clear();
        points.add(new int[]{x, y});
        active = true;
    }

    @Override
    public void onDrag(Pixmap target, int x, int y, Color color) {
        if (!active) return;
        // Avoid duplicates
        if (!points.isEmpty()) {
            int[] last = points.get(points.size() - 1);
            if (last[0] == x && last[1] == y) return;
        }
        points.add(new int[]{x, y});
    }

    @Override
    public void onUp(Pixmap target, int x, int y, Color color) {
        if (!active || points.size() < 3) { active = false; points.clear(); return; }
        // Draw the outline
        target.setBlending(Pixmap.Blending.None);
        target.setColor(color);
        for (int i = 0; i < points.size() - 1; i++) {
            int[] a = points.get(i), b = points.get(i + 1);
            drawLine(target, a[0], a[1], b[0], b[1]);
        }
        // Close the path
        int[] first = points.get(0), last2 = points.get(points.size() - 1);
        drawLine(target, last2[0], last2[1], first[0], first[1]);

        // Scanline polygon fill
        fillPolygon(target, color);

        target.setBlending(Pixmap.Blending.SourceOver);
        active = false;
        points.clear();
    }

    public boolean isActive() { return active; }

    private void fillPolygon(Pixmap pm, Color color) {
        if (points.size() < 3) return;
        int w = pm.getWidth(), h = pm.getHeight();
        // Find bounding box
        int minY = h, maxY = 0;
        for (int[] p : points) { if (p[1] < minY) minY = p[1]; if (p[1] > maxY) maxY = p[1]; }
        minY = Math.max(0, minY); maxY = Math.min(h - 1, maxY);

        pm.setColor(color);
        int n = points.size();
        for (int sy = minY; sy <= maxY; sy++) {
            // Find intersections with polygon edges
            ArrayList<Integer> intersections = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int[] a = points.get(i), b = points.get((i + 1) % n);
                int y0 = a[1], y1 = b[1];
                if ((y0 <= sy && y1 > sy) || (y1 <= sy && y0 > sy)) {
                    int ix = a[0] + (sy - y0) * (b[0] - a[0]) / (y1 - y0);
                    intersections.add(ix);
                }
            }
            // Sort
            java.util.Collections.sort(intersections);
            // Fill between pairs
            for (int i = 0; i + 1 < intersections.size(); i += 2) {
                int x0 = Math.max(0, intersections.get(i));
                int x1 = Math.min(w - 1, intersections.get(i + 1));
                for (int sx = x0; sx <= x1; sx++) {
                    pm.drawPixel(sx, sy);
                }
            }
        }
    }

    private void drawLine(Pixmap pm, int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            pm.drawPixel(x0, y0);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }
}
