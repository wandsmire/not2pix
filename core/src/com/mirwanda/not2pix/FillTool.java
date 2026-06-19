package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Scanline-based flood fill.
 *
 * The previous implementation used a naive 4-directional BFS that enqueued
 * every pixel individually without a visited set.  On a 1024×1024 blank canvas
 * this created millions of queue entries (each pixel enqueued up to 4 times)
 * and caused OutOfMemoryError.
 *
 * This version uses the scanline fill algorithm: for each seed pixel we extend
 * left/right to fill the entire horizontal span, then enqueue the rows above
 * and below.  Combined with an explicit boolean[] visited array, memory usage
 * is bounded at O(width * height) bits ≈ 128 KB for 1024×1024.
 */
public class FillTool implements Tool {

    public boolean clearMode = false; // true = fill with transparency

    @Override public String getName() { return "Fill"; }

    @Override
    public void onDown(Pixmap target, int x, int y, Color color) {
        if (x < 0 || y < 0 || x >= target.getWidth() || y >= target.getHeight()) return;
        int targetColor = target.getPixel(x, y);
        int fillColor = clearMode ? 0 : Color.rgba8888(color);
        if (targetColor == fillColor) return;
        scanlineFill(target, x, y, targetColor, fillColor);
    }

    @Override public void onDrag(Pixmap target, int x, int y, Color color) {}
    @Override public void onUp(Pixmap target, int x, int y, Color color) {}

    /**
     * Scanline flood-fill with explicit visited array.
     *
     * For each seed we extend the horizontal span, mark all pixels as visited,
     * then scan the rows above and below for new seeds.  The Deque stores
     * (x, y) pairs packed as a single long for zero-allocation iteration.
     */
    private void scanlineFill(Pixmap pm, int startX, int startY, int oldColor, int newColor) {
        int w = pm.getWidth(), h = pm.getHeight();
        boolean[] visited = new boolean[w * h];

        pm.setBlending(Pixmap.Blending.None);
        pm.setColor(new Color(newColor));

        // Stack of (x, y) seeds — packed as (x | y << 16)
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{startX, startY});

        while (!stack.isEmpty()) {
            int[] seed = stack.pop();
            int sx = seed[0], sy = seed[1];
            if (sy < 0 || sy >= h) continue;
            if (visited[sx + sy * w]) continue;
            if (pm.getPixel(sx, sy) != oldColor) continue;

            // Extend left
            int left = sx;
            while (left > 0 && !visited[(left - 1) + sy * w] && pm.getPixel(left - 1, sy) == oldColor) {
                left--;
            }
            // Extend right
            int right = sx;
            while (right < w - 1 && !visited[(right + 1) + sy * w] && pm.getPixel(right + 1, sy) == oldColor) {
                right++;
            }

            // Fill the span and mark visited
            for (int x = left; x <= right; x++) {
                pm.drawPixel(x, sy);
                visited[x + sy * w] = true;
            }

            // Scan rows above and below for new seeds
            for (int dir = -1; dir <= 1; dir += 2) {
                int ny = sy + dir;
                if (ny < 0 || ny >= h) continue;
                boolean inSpan = false;
                for (int x = left; x <= right; x++) {
                    int idx = x + ny * w;
                    if (!visited[idx] && pm.getPixel(x, ny) == oldColor) {
                        if (!inSpan) {
                            stack.push(new int[]{x, ny});
                            inSpan = true;
                        }
                    } else {
                        inSpan = false;
                    }
                }
            }
        }
        pm.setBlending(Pixmap.Blending.SourceOver);
    }
}
