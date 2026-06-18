package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import java.util.ArrayList;

public class PencilTool implements Tool {

    private int lastX = -1, lastY = -1;
    public int brushSize = 1;
    public boolean pixelPerfect = true;

    // Stroke buffer for pixel-perfect mode
    public ArrayList<int[]> strokePoints = new ArrayList<>();
    public ArrayList<int[]> removedPoints = new ArrayList<>();
    public float removeAnimTimer = -1; // -1 = not animating
    private static final float REMOVE_ANIM_DURATION = 0.15f;
    private boolean strokeActive = false;

    @Override public String getName() { return "Pencil"; }

    @Override
    public void onDown(Pixmap target, int x, int y, Color color) {
        if (brushSize <= 1 && pixelPerfect) {
            strokePoints.clear();
            removedPoints.clear();
            removeAnimTimer = -1;
            strokeActive = true;
            addStrokePoint(x, y);
        } else {
            target.setColor(color);
            drawDot(target, x, y);
        }
        lastX = x; lastY = y;
    }

    @Override
    public void onDrag(Pixmap target, int x, int y, Color color) {
        if (x == lastX && y == lastY) return;
        if (brushSize <= 1 && pixelPerfect) {
            // Add all points along Bresenham line to stroke buffer
            addLineToStroke(lastX, lastY, x, y);
        } else {
            target.setColor(color);
            drawLine(target, lastX, lastY, x, y, brushSize);
        }
        lastX = x; lastY = y;
    }

    @Override
    public void onUp(Pixmap target, int x, int y, Color color) {
        if (brushSize <= 1 && pixelPerfect && strokeActive) {
            // Find and mark corner pixels for removal
            removedPoints.clear();
            ArrayList<int[]> cleaned = removeCornersFromStroke(strokePoints, removedPoints);
            // Commit cleaned stroke to pixmap
            target.setColor(color);
            for (int[] p : cleaned) {
                target.drawPixel(p[0], p[1]);
            }
            // Start removal animation if there are corners
            if (!removedPoints.isEmpty()) {
                removeAnimTimer = REMOVE_ANIM_DURATION;
            }
            strokePoints.clear();
            strokeActive = false;
        }
        lastX = -1; lastY = -1;
    }

    /** Returns true if currently drawing (stroke preview should be rendered) */
    public boolean isStrokeActive() { return strokeActive; }

    /** Returns true if removal animation is playing */
    public boolean isAnimating() { return removeAnimTimer > 0; }

    public void updateAnim(float delta) {
        if (removeAnimTimer > 0) {
            removeAnimTimer -= delta;
            if (removeAnimTimer <= 0) {
                removeAnimTimer = -1;
                removedPoints.clear();
            }
        }
    }

    private void addStrokePoint(int x, int y) {
        // Avoid duplicates
        if (!strokePoints.isEmpty()) {
            int[] last = strokePoints.get(strokePoints.size() - 1);
            if (last[0] == x && last[1] == y) return;
        }
        strokePoints.add(new int[]{x, y});
    }

    private void addLineToStroke(int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        boolean first = true;
        while (true) {
            if (!first) addStrokePoint(x0, y0);
            first = false;
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }

    /** Remove corner pixels that make the stroke thicker than 1px.
     *  B is removable if prev and next are 8-connected and direction changes at B. */
    private ArrayList<int[]> removeCornersFromStroke(ArrayList<int[]> points, ArrayList<int[]> removed) {
        if (points.size() <= 2) return new ArrayList<>(points);
        boolean[] skip = new boolean[points.size()];
        for (int i = 1; i < points.size() - 1; i++) {
            if (skip[i - 1]) continue; // don't remove two in a row
            int[] prev = points.get(i - 1);
            int[] cur = points.get(i);
            int[] next = points.get(i + 1);
            int cheb = Math.max(Math.abs(next[0] - prev[0]), Math.abs(next[1] - prev[1]));
            int dx1 = cur[0] - prev[0], dy1 = cur[1] - prev[1];
            int dx2 = next[0] - cur[0], dy2 = next[1] - cur[1];
            if (cheb == 1 && (dx1 != dx2 || dy1 != dy2)) {
                skip[i] = true;
                removed.add(cur);
            }
        }
        ArrayList<int[]> result = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            if (!skip[i]) result.add(points.get(i));
        }
        return result;
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

    /** Standard Bresenham line with size (used by EraserTool etc.) */
    public static void drawLine(Pixmap pm, int x0, int y0, int x1, int y1, int size) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            if (size <= 1) {
                pm.drawPixel(x0, y0);
            } else {
                int r = size / 2;
                for (int iy = -r; iy <= r; iy++)
                    for (int ix = -r; ix <= r; ix++)
                        if (ix * ix + iy * iy <= r * r)
                            pm.drawPixel(x0 + ix, y0 + iy);
            }
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }
}
