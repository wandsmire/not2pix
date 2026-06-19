package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.mirwanda.not2pix.Palette;
import com.mirwanda.not2pix.SelectionTool;

/**
 * Color adjustment dialog for selection buffer.
 * Sliders: Hue shift (-180..+180), Saturation (-100..+100), Brightness (-100..+100)
 * Applied in real-time to the selection buffer preview.
 */
public class ColorAdjustDialog {

    public boolean open = false;
    private float dp;
    private float x, y, w, h;
    private float sliderW, sliderH, sliderX;
    private float rowH;

    private SelectionTool sel;
    private Pixmap originalBuffer; // saved copy to recompute adjustments from

    public float hueShift = 0;   // -180 to 180
    public float satShift = 0;   // -100 to 100
    public float brightShift = 0; // -100 to 100
    private int draggingSlider = -1; // 0=hue, 1=sat, 2=bri

    public ColorAdjustDialog(float dp) {
        this.dp = dp;
        this.w = 240 * dp;
        this.rowH = 44 * dp;
        this.h = 4 * rowH + 20 * dp; // 3 sliders + buttons
        this.sliderH = 20 * dp;
    }

    public void show(SelectionTool sel) {
        if (sel == null || sel.buffer == null) return;
        this.sel = sel;
        hueShift = 0; satShift = 0; brightShift = 0;
        // Save a copy of the original buffer
        if (originalBuffer != null) originalBuffer.dispose();
        originalBuffer = new Pixmap(sel.buffer.getWidth(), sel.buffer.getHeight(), Pixmap.Format.RGBA8888);
        originalBuffer.setBlending(Pixmap.Blending.None);
        originalBuffer.drawPixmap(sel.buffer, 0, 0);
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        this.x = (sw - w) / 2f;
        this.y = sh * 0.05f;
        this.sliderX = x + 50 * dp;
        this.sliderW = w - 100 * dp;
        open = true;
    }

    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!open) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.15f, 0.15f, 0.15f, 0.95f);
        sr.rect(x, y, w, h);
        sr.end();

        float ry = y + h - 10 * dp;

        // Hue slider
        ry -= rowH;
        drawSlider(sr, batch, font, ry, "Hue", hueShift, -180, 180);

        // Saturation slider
        ry -= rowH;
        drawSlider(sr, batch, font, ry, "Sat", satShift, -100, 100);

        // Brightness slider
        ry -= rowH;
        drawSlider(sr, batch, font, ry, "Bri", brightShift, -100, 100);

        // OK / Cancel buttons
        ry -= rowH;
        float btnW = 60 * dp, btnH = 30 * dp;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.2f, 0.5f, 0.3f, 1);
        sr.rect(x + 20 * dp, ry + 7 * dp, btnW, btnH);
        sr.setColor(0.5f, 0.2f, 0.2f, 1);
        sr.rect(x + w - btnW - 20 * dp, ry + 7 * dp, btnW, btnH);
        sr.end();
        batch.begin();
        font.setColor(Color.WHITE);
        GlyphLayout gl = new GlyphLayout(font, "OK");
        font.draw(batch, "OK", x + 20 * dp + (btnW - gl.width) / 2f, ry + 7 * dp + (btnH + gl.height) / 2f);
        gl.setText(font, "Cancel");
        font.draw(batch, "Cancel", x + w - btnW - 20 * dp + (btnW - gl.width) / 2f, ry + 7 * dp + (btnH + gl.height) / 2f);
        batch.end();
    }

    private void drawSlider(ShapeRenderer sr, SpriteBatch batch, BitmapFont font, float ry, String label, float value, float min, float max) {
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, label, x + 8 * dp, ry + rowH / 2f + font.getCapHeight() / 2f);
        batch.end();
        // Track
        float trackY = ry + (rowH - sliderH) / 2f;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.3f, 0.3f, 0.3f, 1);
        sr.rect(sliderX, trackY, sliderW, sliderH);
        // Thumb
        float ratio = (value - min) / (max - min);
        float thumbX = sliderX + ratio * sliderW;
        sr.setColor(Color.WHITE);
        sr.rect(thumbX - 4 * dp, trackY - 2 * dp, 8 * dp, sliderH + 4 * dp);
        sr.end();
        // Value label
        batch.begin();
        font.setColor(Color.LIGHT_GRAY);
        String valStr = String.valueOf((int) value);
        font.draw(batch, valStr, sliderX + sliderW + 4 * dp, ry + rowH / 2f + font.getCapHeight() / 2f);
        batch.end();
    }

    public boolean handleTouch(float tx, float ty) {
        if (!open) return false;

        float ry = y + h - 10 * dp;

        // Hue slider
        ry -= rowH;
        if (hitSlider(tx, ty, ry)) { draggingSlider = 0; hueShift = sliderValue(tx, -180, 180); applyAdjustment(); return true; }

        // Sat slider
        ry -= rowH;
        if (hitSlider(tx, ty, ry)) { draggingSlider = 1; satShift = sliderValue(tx, -100, 100); applyAdjustment(); return true; }

        // Brightness slider
        ry -= rowH;
        if (hitSlider(tx, ty, ry)) { draggingSlider = 2; brightShift = sliderValue(tx, -100, 100); applyAdjustment(); return true; }

        draggingSlider = -1;

        // Buttons
        ry -= rowH;
        float btnW = 60 * dp, btnH = 30 * dp;
        // OK
        if (tx >= x + 20 * dp && tx <= x + 20 * dp + btnW && ty >= ry + 7 * dp && ty <= ry + 7 * dp + btnH) {
            close();
            return true;
        }
        // Cancel
        if (tx >= x + w - btnW - 20 * dp && tx <= x + w - 20 * dp && ty >= ry + 7 * dp && ty <= ry + 7 * dp + btnH) {
            if (sel != null && sel.buffer != null && originalBuffer != null) {
                sel.buffer.setBlending(Pixmap.Blending.None);
                sel.buffer.setColor(Color.CLEAR);
                sel.buffer.fill();
                sel.buffer.drawPixmap(originalBuffer, 0, 0);
                sel.buffer.setBlending(Pixmap.Blending.SourceOver);
                sel.disposeBufferTexture();
            }
            close();
            return true;
        }

        return true;
    }

    /** Call this on drag events when dialog is open */
    public void handleDrag(float tx, float ty) {
        if (!open || draggingSlider < 0) return;
        switch (draggingSlider) {
            case 0: hueShift = sliderValue(tx, -180, 180); break;
            case 1: satShift = sliderValue(tx, -100, 100); break;
            case 2: brightShift = sliderValue(tx, -100, 100); break;
        }
        applyAdjustment();
    }

    public void handleUp() { draggingSlider = -1; }

    private boolean hitSlider(float tx, float ty, float ry) {
        float trackY = ry + (rowH - sliderH) / 2f;
        return tx >= sliderX && tx <= sliderX + sliderW && ty >= trackY - 4 * dp && ty <= trackY + sliderH + 4 * dp;
    }

    private float sliderValue(float tx, float min, float max) {
        float ratio = MathUtils.clamp((tx - sliderX) / sliderW, 0, 1);
        return min + ratio * (max - min);
    }

    private void applyAdjustment() {
        if (sel == null || sel.buffer == null || originalBuffer == null) return;
        int w2 = originalBuffer.getWidth(), h2 = originalBuffer.getHeight();
        sel.buffer.setBlending(Pixmap.Blending.None);
        for (int py = 0; py < h2; py++) {
            for (int px = 0; px < w2; px++) {
                int rgba = originalBuffer.getPixel(px, py);
                int a = rgba & 0xFF;
                if (a == 0) { sel.buffer.drawPixel(px, py, 0); continue; }
                Color c = new Color();
                Color.rgba8888ToColor(c, rgba);
                float[] hsv = Palette.colorToHSV(c);
                hsv[0] = (hsv[0] + hueShift + 360) % 360;
                hsv[1] = MathUtils.clamp(hsv[1] + satShift / 100f, 0, 1);
                hsv[2] = MathUtils.clamp(hsv[2] + brightShift / 100f, 0, 1);
                Color nc = Palette.hsvToColor(hsv[0], hsv[1], hsv[2], c.a);
                sel.buffer.drawPixel(px, py, Color.rgba8888(nc));
            }
        }
        sel.buffer.setBlending(Pixmap.Blending.SourceOver);
        sel.disposeBufferTexture();
    }

    private void close() {
        open = false;
        if (originalBuffer != null) { originalBuffer.dispose(); originalBuffer = null; }
    }
}
