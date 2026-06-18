package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.graphics.Color;
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
    private UIButton okBtn;
    public boolean open = false;

    public HSVPicker(Not2Pix app, float screenWidth, float screenHeight, float dp) {
        super(0, 0, 0, 0);
        this.app = app;
        this.dp = dp;

        float pickerSize = Math.min(screenWidth * 0.8f, screenHeight * 0.6f);
        float totalH = pickerSize + 60 * dp;
        this.width = pickerSize + 20 * dp;
        this.height = totalH;
        this.x = (screenWidth - width) / 2f;
        this.y = (screenHeight - height) / 2f;

        svSize = pickerSize - 20 * dp;
        svX = x + 10 * dp;
        svY = y + 60 * dp;

        hueBarX = svX;
        hueBarY = y + 35 * dp;
        hueBarW = svSize;
        hueBarH = 20 * dp;

        alphaBarX = svX;
        alphaBarY = y + 10 * dp;
        alphaBarW = svSize;
        alphaBarH = 16 * dp;

        okBtn = new UIButton("OK", x + width - 50 * dp, y + height - 30 * dp, 44 * dp, 24 * dp);
        okBtn.action = () -> open = false;
        children.add(okBtn);

        buildHueTexture();
        buildSVTexture(app.palette.hue);
        visible = false;
    }

    private void buildHueTexture() {
        int w = 256, h = 1;
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        for (int i = 0; i < w; i++) {
            Color c = Palette.hsvToColor(i * 360f / w, 1, 1, 1);
            pm.setColor(c);
            pm.drawPixel(i, 0);
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
        // Sync from palette
        float[] hsv = Palette.colorToHSV(app.palette.getSelected());
        app.palette.hue = hsv[0];
        app.palette.saturation = hsv[1];
        app.palette.value = hsv[2];
        app.palette.alpha = hsv[3];
        buildSVTexture(app.palette.hue);
    }

    @Override
    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!visible || !open) return;

        // Dim background
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0, 0, 0, 0.6f);
        sr.rect(0, 0, 9999, 9999);
        sr.end();

        // Panel background
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.18f, 0.18f, 0.18f, 1);
        sr.rect(x, y, width, height);
        sr.end();

        // SV square
        batch.begin();
        batch.draw(svTexture, svX, svY, svSize, svSize);
        batch.end();

        // SV cursor
        Palette pal = app.palette;
        float curX = svX + pal.saturation * svSize;
        float curY = svY + (1f - pal.value) * svSize;
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(Color.WHITE);
        sr.circle(curX, curY, 5 * dp);
        sr.end();

        // Hue bar
        batch.begin();
        batch.draw(hueTexture, hueBarX, hueBarY, hueBarW, hueBarH);
        batch.end();
        // Hue cursor
        float hueX = hueBarX + (pal.hue / 360f) * hueBarW;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(Color.WHITE);
        sr.rect(hueX - 2, hueBarY - 2, 4, hueBarH + 4);
        sr.end();

        // Alpha bar
        sr.begin(ShapeRenderer.ShapeType.Filled);
        // Checkerboard background for alpha
        sr.setColor(0.5f, 0.5f, 0.5f, 1);
        sr.rect(alphaBarX, alphaBarY, alphaBarW, alphaBarH);
        sr.setColor(pal.getSelected());
        sr.rect(alphaBarX, alphaBarY, alphaBarW * pal.alpha, alphaBarH);
        sr.end();

        // OK button
        okBtn.draw(sr, batch, font);
    }

    /** Handle touch. Returns true if picker consumed it. */
    public boolean handleTouch(float touchX, float touchY) {
        if (!open || !visible) return false;

        // OK button
        if (okBtn.hit(touchX, touchY)) {
            open = false;
            visible = false;
            return true;
        }

        Palette pal = app.palette;

        // SV area
        if (touchX >= svX && touchX <= svX + svSize && touchY >= svY && touchY <= svY + svSize) {
            pal.saturation = MathUtils.clamp((touchX - svX) / svSize, 0, 1);
            pal.value = MathUtils.clamp(1f - (touchY - svY) / svSize, 0, 1);
            pal.setFromHSV(pal.hue, pal.saturation, pal.value, pal.alpha);
            return true;
        }

        // Hue bar
        if (touchX >= hueBarX && touchX <= hueBarX + hueBarW &&
            touchY >= hueBarY - 5 * dp && touchY <= hueBarY + hueBarH + 5 * dp) {
            pal.hue = MathUtils.clamp((touchX - hueBarX) / hueBarW * 360f, 0, 359);
            buildSVTexture(pal.hue);
            pal.setFromHSV(pal.hue, pal.saturation, pal.value, pal.alpha);
            return true;
        }

        // Alpha bar
        if (touchX >= alphaBarX && touchX <= alphaBarX + alphaBarW &&
            touchY >= alphaBarY - 3 * dp && touchY <= alphaBarY + alphaBarH + 3 * dp) {
            pal.alpha = MathUtils.clamp((touchX - alphaBarX) / alphaBarW, 0, 1);
            pal.setFromHSV(pal.hue, pal.saturation, pal.value, pal.alpha);
            return true;
        }

        // Consume all touches when open (modal)
        return true;
    }

    public void dispose() {
        if (svTexture != null) svTexture.dispose();
        if (hueTexture != null) hueTexture.dispose();
    }
}
