package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

public class BackgroundTool implements Tool {

    private Not2Pix app;
    private int dragMode = 0; // 0 = none, 1 = move, 2 = resize top-left, 3 = resize top-right, 4 = resize bottom-right, 5 = resize bottom-left
    private float startTouchX, startTouchY;
    private float startBgX, startBgY, startBgW, startBgH;

    public BackgroundTool(Not2Pix app) {
        this.app = app;
    }

    @Override
    public String getName() {
        return "Background";
    }

    @Override
    public void onDown(Pixmap target, int px, int py, Color color) {
        if (app.bgTraceTexture == null) return;
        
        float wx = px;
        float wy = app.canvasHeight - 1 - py;

        startTouchX = wx;
        startTouchY = wy;
        startBgX = app.bgTraceX;
        startBgY = app.bgTraceY;
        startBgW = app.bgTraceWidth;
        startBgH = app.bgTraceHeight;

        // Check if we hit corner handles
        float threshold = Math.max(1f, 16f / app.getZoom()); // screen pixels threshold mapped to canvas space

        if (dist(wx, wy, app.bgTraceX, app.bgTraceY) < threshold) {
            dragMode = 5; // bottom-left
        } else if (dist(wx, wy, app.bgTraceX + app.bgTraceWidth, app.bgTraceY) < threshold) {
            dragMode = 4; // bottom-right
        } else if (dist(wx, wy, app.bgTraceX + app.bgTraceWidth, app.bgTraceY + app.bgTraceHeight) < threshold) {
            dragMode = 3; // top-right
        } else if (dist(wx, wy, app.bgTraceX, app.bgTraceY + app.bgTraceHeight) < threshold) {
            dragMode = 2; // top-left
        } else if (wx >= app.bgTraceX && wx <= app.bgTraceX + app.bgTraceWidth &&
                   wy >= app.bgTraceY && wy <= app.bgTraceY + app.bgTraceHeight) {
            dragMode = 1; // move
        } else {
            dragMode = 0;
        }
    }

    @Override
    public void onDrag(Pixmap target, int px, int py, Color color) {
        if (app.bgTraceTexture == null || dragMode == 0) return;

        float wx = px;
        float wy = app.canvasHeight - 1 - py;

        float dx = wx - startTouchX;
        float dy = wy - startTouchY;

        if (dragMode == 1) { // Move
            app.bgTraceX = startBgX + dx;
            app.bgTraceY = startBgY + dy;
        } else if (dragMode == 3) { // Resize top-right
            app.bgTraceWidth = Math.max(4f, startBgW + dx);
            app.bgTraceHeight = Math.max(4f, startBgH + dy);
        } else if (dragMode == 4) { // Resize bottom-right
            app.bgTraceWidth = Math.max(4f, startBgW + dx);
            app.bgTraceY = startBgY + dy;
            app.bgTraceHeight = Math.max(4f, startBgH - dy);
        } else if (dragMode == 2) { // Resize top-left
            app.bgTraceX = startBgX + dx;
            app.bgTraceWidth = Math.max(4f, startBgW - dx);
            app.bgTraceHeight = Math.max(4f, startBgH + dy);
        } else if (dragMode == 5) { // Resize bottom-left
            app.bgTraceX = startBgX + dx;
            app.bgTraceWidth = Math.max(4f, startBgW - dx);
            app.bgTraceY = startBgY + dy;
            app.bgTraceHeight = Math.max(4f, startBgH - dy);
        }
    }

    @Override
    public void onUp(Pixmap target, int px, int py, Color color) {
        dragMode = 0;
    }

    private float dist(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }
}
