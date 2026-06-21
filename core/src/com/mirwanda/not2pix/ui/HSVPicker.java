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
    private UIButton hexBtn;
    private Color oldColor = new Color();
    public boolean open = false;

    private int dragMode = 0; // 0 = none, 1 = SV, 2 = Hue, 3 = Alpha

    public void touchReleased() {
        dragMode = 0;
    }

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

        float btnW = (width - 4 * pad) / 3f;
        cancelBtn = new UIButton("Cancel", x + pad, y + pad, btnW, btnH);
        cancelBtn.action = () -> {
            app.palette.setFromColor(oldColor);
            open = false;
            visible = false;
        };
        children.add(cancelBtn);

        hexBtn = new UIButton("#FFFFFFFF", x + 2 * pad + btnW, y + pad, btnW, btnH);
        hexBtn.action = () -> {
            Color cur = colorTarget != null ? colorTarget : app.palette.getSelected();
            String currentHex = colorToHex(cur);
            Gdx.input.getTextInput(new com.badlogic.gdx.Input.TextInputListener() {
                @Override
                public void input(String text) {
                    parseColorHex(text);
                }
                @Override
                public void canceled() {}
            }, "Edit Color Hex", currentHex, "#RRGGBB or #RRGGBBAA");
        };
        children.add(hexBtn);

        okBtn = new UIButton("OK", x + 3 * pad + 2 * btnW, y + pad, btnW, btnH);
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
        dragMode = 0;
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
        dragMode = 0;
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
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
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

        // Alpha bar background (checkerboard)
        sr.begin(ShapeRenderer.ShapeType.Filled);
        float alphaCheckSize = 5 * dp;
        for (float cy = alphaBarY; cy < alphaBarY + alphaBarH; cy += alphaCheckSize) {
            for (float cx = alphaBarX; cx < alphaBarX + alphaBarW; cx += alphaCheckSize) {
                int ix = (int) ((cx - alphaBarX) / alphaCheckSize);
                int iy = (int) ((cy - alphaBarY) / alphaCheckSize);
                sr.setColor((ix + iy) % 2 == 0 ? new Color(0.9f, 0.9f, 0.9f, 1f) : new Color(0.75f, 0.75f, 0.75f, 1f));
                sr.rect(cx, cy, Math.min(alphaCheckSize, alphaBarX + alphaBarW - cx), Math.min(alphaCheckSize, alphaBarY + alphaBarH - cy));
            }
        }
        
        // Alpha bar color gradient (transparent -> opaque)
        Color cTrans = new Color(drawColor.r, drawColor.g, drawColor.b, 0f);
        Color cOpaque = new Color(drawColor.r, drawColor.g, drawColor.b, 1f);
        sr.rect(alphaBarX, alphaBarY, alphaBarW, alphaBarH, cTrans, cOpaque, cOpaque, cTrans);
        
        // Alpha bar cursor indicator (framed thumb)
        float indicatorX = alphaBarX + drawAlpha * alphaBarW;
        sr.setColor(0.1f, 0.1f, 0.1f, 1f); // Dark border
        sr.rect(indicatorX - 3 * dp, alphaBarY - 2 * dp, 6 * dp, alphaBarH + 4 * dp);
        sr.setColor(1f, 1f, 1f, 1f); // White center
        sr.rect(indicatorX - 2 * dp, alphaBarY - 1 * dp, 4 * dp, alphaBarH + 2 * dp);
        sr.end();

        // Alpha bar border
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.35f, 0.35f, 0.35f, 1);
        sr.rect(alphaBarX, alphaBarY, alphaBarW, alphaBarH);
        sr.end();

        // Buttons
        if (hexBtn != null) {
            hexBtn.label = colorToHex(drawColor);
            hexBtn.draw(sr, batch, font);
        }
        okBtn.draw(sr, batch, font);
        cancelBtn.draw(sr, batch, font);
    }

    private void updateSV(float touchX, float touchY) {
        float s = MathUtils.clamp((touchX - svX) / svSize, 0, 1);
        float v = MathUtils.clamp((touchY - svY) / svSize, 0, 1);
        if (colorTarget != null) {
            targetSat = s; targetVal = v;
            colorTarget.set(Palette.hsvToColor(targetHue, targetSat, targetVal, targetAlpha));
        } else {
            app.palette.saturation = s; app.palette.value = v;
            app.palette.setFromHSV(app.palette.hue, s, v, app.palette.alpha);
        }
    }

    private void updateHue(float touchY) {
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
    }

    private void updateAlpha(float touchX) {
        float a = MathUtils.clamp((touchX - alphaBarX) / alphaBarW, 0, 1);
        if (colorTarget != null) {
            targetAlpha = a;
            colorTarget.set(Palette.hsvToColor(targetHue, targetSat, targetVal, targetAlpha));
        } else {
            app.palette.alpha = a;
            app.palette.setFromHSV(app.palette.hue, app.palette.saturation, app.palette.value, a);
        }
    }

    /** Handle touch. Returns true if picker consumed it. */
    public boolean handleTouch(float touchX, float touchY) {
        if (!open || !visible) return false;

        // If locked to a slider zone during drag, directly delegate
        if (dragMode == 1) {
            updateSV(touchX, touchY);
            return true;
        } else if (dragMode == 2) {
            updateHue(touchY);
            return true;
        } else if (dragMode == 3) {
            updateAlpha(touchX);
            return true;
        } else if (dragMode == 4) {
            return true;
        }

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

        // Hex edit button
        if (hexBtn != null && hexBtn.hit(touchX, touchY)) {
            dragMode = 4;
            if (hexBtn.action != null) {
                hexBtn.action.run();
            }
            return true;
        }

        // SV area
        if (touchX >= svX && touchX <= svX + svSize && touchY >= svY && touchY <= svY + svSize) {
            dragMode = 1;
            updateSV(touchX, touchY);
            return true;
        }

        // Vertical hue bar
        if (touchX >= hueBarX - 5 * dp && touchX <= hueBarX + hueBarW + 5 * dp &&
            touchY >= hueBarY && touchY <= hueBarY + hueBarH) {
            dragMode = 2;
            updateHue(touchY);
            return true;
        }

        // Alpha bar
        if (touchX >= alphaBarX && touchX <= alphaBarX + alphaBarW &&
            touchY >= alphaBarY - 3 * dp && touchY <= alphaBarY + alphaBarH + 3 * dp) {
            dragMode = 3;
            updateAlpha(touchX);
            return true;
        }

        return true;
    }

    private String colorToHex(Color color) {
        int r = MathUtils.clamp((int) (color.r * 255f), 0, 255);
        int g = MathUtils.clamp((int) (color.g * 255f), 0, 255);
        int b = MathUtils.clamp((int) (color.b * 255f), 0, 255);
        int a = MathUtils.clamp((int) (color.a * 255f), 0, 255);
        return String.format("#%02X%02X%02X%02X", r, g, b, a);
    }

    private void parseColorHex(String text) {
        try {
            String hex = text.trim();
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            if (hex.length() == 6) {
                hex += "ff";
            }
            if (hex.length() == 8) {
                float r = Integer.parseInt(hex.substring(0, 2), 16) / 255f;
                float g = Integer.parseInt(hex.substring(2, 4), 16) / 255f;
                float b = Integer.parseInt(hex.substring(4, 6), 16) / 255f;
                float a = Integer.parseInt(hex.substring(6, 8), 16) / 255f;

                Color parsedColor = new Color(r, g, b, a);
                float[] hsv = Palette.colorToHSV(parsedColor);

                if (colorTarget != null) {
                    targetHue = hsv[0];
                    targetSat = hsv[1];
                    targetVal = hsv[2];
                    targetAlpha = hsv[3];
                    colorTarget.set(parsedColor);
                } else {
                    app.palette.hue = hsv[0];
                    app.palette.saturation = hsv[1];
                    app.palette.value = hsv[2];
                    app.palette.alpha = hsv[3];
                    app.palette.setFromHSV(hsv[0], hsv[1], hsv[2], hsv[3]);
                }
                buildSVTexture(hsv[0]);
            }
        } catch (Exception e) {
            // Ignore invalid input
        }
    }

    public void dispose() {
        if (svTexture != null) svTexture.dispose();
        if (hueTexture != null) hueTexture.dispose();
    }
}
