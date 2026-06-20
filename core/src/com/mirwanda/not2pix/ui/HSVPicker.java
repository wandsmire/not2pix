package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.mirwanda.not2pix.Not2Pix;
import com.mirwanda.not2pix.Palette;

/**
 * HSV color picker popup. Shows:
 * - Large SV gradient square (x=saturation, y=value at current hue)
 * - Hue slider bar below
 * - Alpha slider
 * - OK button to dismiss
 */
public class HSVPicker extends UIPanel {

    private Not2Pix app;
    private float dp;
    private Texture svTexture;
    private Texture hueTexture;
    private float svX, svY, svSize;
    private float hueBarX, hueBarY, hueBarW, hueBarH;
    private float alphaBarX, alphaBarY, alphaBarW, alphaBarH;
    private float previewX, previewY, previewW, previewH;
    private UIButton okBtn;
    private UIButton cancelBtn;
    private Color oldColor = new Color();
    public boolean open = false;

    // When non-null, we're picking for an external color (not palette)
    private Color colorTarget = null;
    private Runnable onTargetDone = null;
    private float targetHue, targetSat, targetVal, targetAlpha;

    public HSVPicker(Not2Pix app, float screenWidth, float screenHeight, float dp) {
        super(0, 0, 0, 0);
        this.app = app;
        this.dp = dp;

        float pickerSize = Math.min(screenWidth * 0.8f, screenHeight * 0.6f);
        float btnH = 30 * dp;
        float alphaH = 20 * dp;
        float pad = 10 * dp;
        float hueW = 20 * dp;
        float prevW = 30 * dp;

        svSize = pickerSize - hueW - prevW - 4 * pad;
        this.width = hueW + svSize + prevW + 4 * pad;
        this.height = svSize + btnH + alphaH + 4 * pad;
        this.x = (screenWidth - width) / 2f;
        this.y = 0;

        // Vertical hue bar on the left
        hueBarX = x + pad;
        hueBarY = y + btnH + alphaH + 3 * pad;
        hueBarW = hueW;
        hueBarH = svSize;

        // SV square to the right of hue bar
        svX = hueBarX + hueW + pad;
        svY = hueBarY;

        // Color preview to the right of SV square
        previewX = svX + svSize + pad;
        previewY = hueBarY;
        previewW = prevW;
        previewH = svSize;

        // Alpha bar below SV area
        alphaBarX = hueBarX;
        alphaBarY = y + btnH + 2 * pad;
        alphaBarW = width - 2 * pad;
        alphaBarH = alphaH;

        float halfBtnW = (width - 3 * pad) / 2f;
        cancelBtn = new UIButton("Cancel", x + pad, y + pad, halfBtnW, btnH);
        cancelBtn.action = () -> {
            app.palette.setFromColor(oldColor);
            open = false;
            visible = false;
        };
        children.add(cancelBtn);

        okBtn = new UIButton("OK", x + pad + halfBtnW + pad, y + pad, halfBtnW, btnH);
        okBtn.action = () -> { open = false; visible = false; };
        children.add(okBtn);

        buildHueTexture();
        buildSVTexture(app.palette.hue);
        visible = false;
    }

    private void buildHueTexture() {
        int w = 1, h = 256;
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        for (int i = 0; i < h; i++) {
            Color c = Palette.hsvToColor((1f - i / (float) h) * 360f, 1, 1, 1);
            pm.setColor(c);
            pm.drawPixel(0, i);
        }
        hueTexture = new Texture(pm);
        pm.dispose();
    }

    private void buildSVTexture(float hue) {
        if (svTexture != null) svTexture.dispose();
        int size = 128;
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        for (int sy = 0; sy < size; sy++) {
            for (int sx = 0; sx < size; sx++) {
                float s = sx / (float) size;
                float v = 1f - sy / (float) size;
                pm.setColor(Palette.hsvToColor(hue, s, v, 1));
                pm.drawPixel(sx, sy);
            }
        }
        svTexture = new Texture(pm);
        pm.dispose();
    }

    public void show() {
        open = true;
        visible = true;
        colorTarget = null;
        onTargetDone = null;
        oldColor.set(app.palette.getSelected());
        float[] hsv = Palette.colorToHSV(app.palette.getSelected());
        app.palette.hue = hsv[0];
        app.palette.saturation = hsv[1];
        app.palette.value = hsv[2];
        app.palette.alpha = hsv[3];
        buildSVTexture(app.palette.hue);
    }

    /** Open picker for an arbitrary color target (not the palette) */
    public void showForColor(Color target, Runnable onDone) {
        open = true;
        visible = true;
        colorTarget = target;
        onTargetDone = onDone;
        oldColor.set(target);
        float[] hsv = Palette.colorToHSV(target);
        targetHue = hsv[0]; targetSat = hsv[1]; targetVal = hsv[2]; targetAlpha = hsv[3];
        buildSVTexture(targetHue);
    }

    @Override
    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!visible || !open) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Dim background (semi-transparent to see canvas)
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0, 0, 0, 0.3f);
        sr.rect(0, 0, 9999, 9999);
        sr.end();

        // Panel background (semi-transparent)
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.18f, 0.18f, 0.18f, 0.75f);
        sr.rect(x, y, width, height);
        sr.end();

        Palette pal = app.palette;
        float drawHue = colorTarget != null ? targetHue : pal.hue;
        float drawSat = colorTarget != null ? targetSat : pal.saturation;
        float drawVal = colorTarget != null ? targetVal : pal.value;
        float drawAlpha = colorTarget != null ? targetAlpha : pal.alpha;
        Color drawColor = colorTarget != null ? colorTarget : pal.getSelected();

        // Vertical hue bar on left
        batch.begin();
        batch.draw(hueTexture, hueBarX, hueBarY, hueBarW, hueBarH);
        batch.end();
        // Hue cursor (horizontal marker)
        float hueCurY = hueBarY + (drawHue / 360f) * hueBarH;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(Color.WHITE);
        sr.rect(hueBarX - 2, hueCurY - 2, hueBarW + 4, 4);
        sr.end();

        // SV square
        batch.begin();
        batch.draw(svTexture, svX, svY, svSize, svSize);
        batch.end();

        // SV cursor
        float curX = svX + drawSat * svSize;
        float curY = svY + drawVal * svSize;
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(Color.WHITE);
        sr.circle(curX, curY, 5 * dp);
        sr.end();

        // Color preview: checkerboard + new (top) and old (bottom)
        float halfH = previewH / 2f;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        float checkSize = 6 * dp;
        for (float cy = previewY; cy < previewY + previewH; cy += checkSize) {
            for (float cx = previewX; cx < previewX + previewW; cx += checkSize) {
                int ix = (int) ((cx - previewX) / checkSize);
                int iy = (int) ((cy - previewY) / checkSize);
                sr.setColor((ix + iy) % 2 == 0 ? Color.LIGHT_GRAY : Color.DARK_GRAY);
                sr.rect(cx, cy, Math.min(checkSize, previewX + previewW - cx), Math.min(checkSize, previewY + previewH - cy));
            }
        }
        // New color (top half)
        sr.setColor(drawColor);
        sr.rect(previewX, previewY + halfH, previewW, halfH);
        // Old color (bottom half)
        sr.setColor(oldColor);
        sr.rect(previewX, previewY, previewW, halfH);
        sr.end();

        // Alpha bar
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.5f, 0.5f, 0.5f, 1);
        sr.rect(alphaBarX, alphaBarY, alphaBarW, alphaBarH);
        sr.setColor(drawColor);
        sr.rect(alphaBarX, alphaBarY, alphaBarW * drawAlpha, alphaBarH);
        sr.end();

        // Buttons
        okBtn.draw(sr, batch, font);
        cancelBtn.draw(sr, batch, font);
    }

    /** Handle touch. Returns true if picker consumed it. */
    public boolean handleTouch(float touchX, float touchY) {
        if (!open || !visible) return false;

        // OK button
        if (okBtn.hit(touchX, touchY)) {
            if (colorTarget != null && onTargetDone != null) onTargetDone.run();
            colorTarget = null; onTargetDone = null;
            open = false; visible = false;
            return true;
        }

        // Cancel button
        if (cancelBtn.hit(touchX, touchY)) {
            if (colorTarget != null) {
                colorTarget.set(oldColor);
            } else {
                app.palette.setFromColor(oldColor);
            }
            colorTarget = null; onTargetDone = null;
            open = false; visible = false;
            return true;
        }

        // SV area
        if (touchX >= svX && touchX <= svX + svSize && touchY >= svY && touchY <= svY + svSize) {
            float s = MathUtils.clamp((touchX - svX) / svSize, 0, 1);
            float v = MathUtils.clamp((touchY - svY) / svSize, 0, 1);
            if (colorTarget != null) {
                targetSat = s; targetVal = v;
                colorTarget.set(Palette.hsvToColor(targetHue, targetSat, targetVal, targetAlpha));
            } else {
                app.palette.saturation = s; app.palette.value = v;
                app.palette.setFromHSV(app.palette.hue, s, v, app.palette.alpha);
            }
            return true;
        }

        // Vertical hue bar
        if (touchX >= hueBarX - 5 * dp && touchX <= hueBarX + hueBarW + 5 * dp &&
            touchY >= hueBarY && touchY <= hueBarY + hueBarH) {
            float h = MathUtils.clamp((touchY - hueBarY) / hueBarH * 360f, 0, 359);
            if (colorTarget != null) {
                targetHue = h;
                buildSVTexture(targetHue);
                colorTarget.set(Palette.hsvToColor(targetHue, targetSat, targetVal, targetAlpha));
            } else {
                app.palette.hue = h;
                buildSVTexture(h);
                app.palette.setFromHSV(h, app.palette.saturation, app.palette.value, app.palette.alpha);
            }
            return true;
        }

        // Alpha bar
        if (touchX >= alphaBarX && touchX <= alphaBarX + alphaBarW &&
            touchY >= alphaBarY - 3 * dp && touchY <= alphaBarY + alphaBarH + 3 * dp) {
            float a = MathUtils.clamp((touchX - alphaBarX) / alphaBarW, 0, 1);
            if (colorTarget != null) {
                targetAlpha = a;
                colorTarget.set(Palette.hsvToColor(targetHue, targetSat, targetVal, targetAlpha));
            } else {
                app.palette.alpha = a;
                app.palette.setFromHSV(app.palette.hue, app.palette.saturation, app.palette.value, a);
            }
            return true;
        }

        return true;
    }

    public void dispose() {
        if (svTexture != null) svTexture.dispose();
        if (hueTexture != null) hueTexture.dispose();
    }
}
