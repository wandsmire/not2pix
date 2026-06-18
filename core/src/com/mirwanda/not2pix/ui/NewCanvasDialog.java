package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Modal dialog for picking new canvas size.
 * - / + steppers for common sizes, tap value to type custom number.
 */
public class NewCanvasDialog {

    public boolean open = false;
    private float dp;

    private static final int[] SIZES = {
        8, 16, 24, 32, 48, 64, 80, 96, 128, 160, 192, 256, 320, 384, 512, 1024
    };

    private int widthVal = 32;
    private int heightVal = 32;

    private Runnable onCancel;
    private java.util.function.BiConsumer<Integer, Integer> onConfirm;

    public NewCanvasDialog(float dp) { this.dp = dp; }

    public void show(java.util.function.BiConsumer<Integer, Integer> onConfirm, Runnable onCancel) {
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        open = true;
    }

    private float dw() { return 240 * dp; }
    private float dh() { return 190 * dp; }
    private float dx() { return (Gdx.graphics.getWidth() - dw()) / 2f; }
    private float dy() { return (Gdx.graphics.getHeight() - dh()) / 2f; }

    private float rowY(int row) {
        // row 0 = Width, row 1 = Height, from top down
        return dy() + dh() - 70 * dp - row * 50 * dp;
    }

    private float btnSize() { return 36 * dp; }
    private float rowLabelX() { return dx() + 8 * dp; }
    private float minusBtnX() { return dx() + 70 * dp; }
    private float plusBtnX() { return dx() + dw() - 8 * dp - btnSize(); }
    private float valMidX() { return minusBtnX() + btnSize() + (plusBtnX() - minusBtnX() - btnSize()) / 2f; }

    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!open) return;
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0, 0, 0, 0.65f);
        sr.rect(0, 0, sw, sh);
        sr.setColor(0.2f, 0.2f, 0.22f, 1);
        sr.rect(dx(), dy(), dw(), dh());

        float bs = btnSize();
        sr.setColor(0.3f, 0.3f, 0.3f, 1);
        for (int row = 0; row < 2; row++) {
            float ry = rowY(row);
            sr.rect(minusBtnX(), ry, bs, bs);
            sr.rect(plusBtnX(), ry, bs, bs);
            // Value tap area background
            sr.setColor(0.15f, 0.15f, 0.15f, 1);
            float valW = plusBtnX() - minusBtnX() - bs - 8 * dp;
            sr.rect(minusBtnX() + bs + 4 * dp, ry + 4 * dp, valW, bs - 8 * dp);
            sr.setColor(0.3f, 0.3f, 0.3f, 1);
        }

        // OK / Cancel
        float btnW = 80 * dp, btnH = 34 * dp;
        float btnY = dy() + 10 * dp;
        float okX = dx() + dw() / 2f - btnW - 6 * dp;
        float cnX = dx() + dw() / 2f + 6 * dp;
        sr.setColor(0.3f, 0.6f, 0.3f, 1);
        sr.rect(okX, btnY, btnW, btnH);
        sr.setColor(0.5f, 0.25f, 0.25f, 1);
        sr.rect(cnX, btnY, btnW, btnH);
        sr.end();

        batch.begin();
        GlyphLayout gl = new GlyphLayout();

        // Title
        font.setColor(Color.WHITE);
        gl.setText(font, "New Canvas");
        font.draw(batch, "New Canvas", dx() + (dw() - gl.width) / 2f, dy() + dh() - 14 * dp);

        // Rows
        String[] labels = {"W:", "H:"};
        int[] vals = {widthVal, heightVal};
        for (int row = 0; row < 2; row++) {
            float ry = rowY(row);
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, labels[row], rowLabelX(), ry + (bs + font.getCapHeight()) / 2f);

            String valStr = String.valueOf(vals[row]);
            gl.setText(font, valStr);
            font.setColor(Color.WHITE);
            font.draw(batch, valStr, valMidX() - gl.width / 2f, ry + (bs + gl.height) / 2f);

            font.setColor(Color.WHITE);
            gl.setText(font, "-");
            font.draw(batch, "-", minusBtnX() + (bs - gl.width) / 2f, ry + (bs + gl.height) / 2f);
            gl.setText(font, "+");
            font.draw(batch, "+", plusBtnX() + (bs - gl.width) / 2f, ry + (bs + gl.height) / 2f);
        }

        // Button labels
        float btnY2 = dy() + 10 * dp;
        gl.setText(font, "Create");
        font.setColor(Color.WHITE);
        font.draw(batch, "Create", okX + (btnW - gl.width) / 2f, btnY2 + (btnH + gl.height) / 2f);
        gl.setText(font, "Cancel");
        font.draw(batch, "Cancel", cnX + (btnW - gl.width) / 2f, btnY2 + (btnH + gl.height) / 2f);

        batch.end();
    }

    public boolean handleTouch(float tx, float ty) {
        if (!open) return false;

        float bs = btnSize();
        float btnW = 80 * dp, btnH = 34 * dp;
        float btnY = dy() + 10 * dp;
        float okX = dx() + dw() / 2f - btnW - 6 * dp;
        float cnX = dx() + dw() / 2f + 6 * dp;

        if (tx >= okX && tx <= okX + btnW && ty >= btnY && ty <= btnY + btnH) {
            open = false;
            if (onConfirm != null) onConfirm.accept(widthVal, heightVal);
            return true;
        }
        if (tx >= cnX && tx <= cnX + btnW && ty >= btnY && ty <= btnY + btnH) {
            open = false;
            if (onCancel != null) onCancel.run();
            return true;
        }

        for (int row = 0; row < 2; row++) {
            float ry = rowY(row);
            if (ty < ry || ty > ry + bs) continue;

            // Minus
            if (tx >= minusBtnX() && tx <= minusBtnX() + bs) {
                stepValue(row, -1);
                return true;
            }
            // Plus
            if (tx >= plusBtnX() && tx <= plusBtnX() + bs) {
                stepValue(row, 1);
                return true;
            }
            // Tap on value: open text input
            if (tx > minusBtnX() + bs && tx < plusBtnX()) {
                final int r = row;
                String current = String.valueOf(r == 0 ? widthVal : heightVal);
                Gdx.input.getTextInput(new Input.TextInputListener() {
                    @Override
                    public void input(String text) {
                        try {
                            int v = Integer.parseInt(text.trim());
                            if (v >= 1 && v <= 4096) {
                                if (r == 0) widthVal = v;
                                else heightVal = v;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                    @Override
                    public void canceled() {}
                }, r == 0 ? "Width" : "Height", current, "");
                return true;
            }
        }

        return true;
    }

    private void stepValue(int row, int dir) {
        int current = row == 0 ? widthVal : heightVal;
        // Find nearest SIZES index and step
        int bestIdx = 0;
        int bestDist = Math.abs(SIZES[0] - current);
        for (int i = 1; i < SIZES.length; i++) {
            int d = Math.abs(SIZES[i] - current);
            if (d < bestDist) { bestDist = d; bestIdx = i; }
        }
        int newIdx = Math.max(0, Math.min(SIZES.length - 1, bestIdx + dir));
        if (row == 0) widthVal = SIZES[newIdx];
        else heightVal = SIZES[newIdx];
    }
}
