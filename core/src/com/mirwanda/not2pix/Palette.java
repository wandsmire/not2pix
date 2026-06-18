package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Color;
import java.util.ArrayList;

/**
 * Color palette manager with preset colors and HSV color picking.
 */
public class Palette {

    public ArrayList<Color> colors = new ArrayList<>();
    public int selectedIndex = 0;
    public float hue = 0, saturation = 1, value = 1, alpha = 1;

    public Palette() {
        // Default palette - similar to Aseprite's default
        addHex(0x000000FF); addHex(0x1D2B53FF); addHex(0x7E2553FF); addHex(0x008751FF);
        addHex(0xAB5236FF); addHex(0x5F574FFF); addHex(0xC2C3C7FF); addHex(0xFFF1E8FF);
        addHex(0xFF004DFF); addHex(0xFFA300FF); addHex(0xFFEC27FF); addHex(0x00E436FF);
        addHex(0x29ADFFFF); addHex(0x83769CFF); addHex(0xFF77A8FF); addHex(0xFFCCAAFF);
        // Extended
        addHex(0xFFFFFFFF); addHex(0x888888FF); addHex(0x444444FF); addHex(0x222222FF);
        addHex(0xFF0000FF); addHex(0x00FF00FF); addHex(0x0000FFFF); addHex(0xFFFF00FF);
        addHex(0xFF00FFFF); addHex(0x00FFFFFF); addHex(0xFF8800FF); addHex(0x8800FFFF);
        addHex(0x0088FFFF); addHex(0x00FF88FF); addHex(0xFF0088FF); addHex(0x00000000);
    }

    private void addHex(int rgba) {
        Color c = new Color();
        Color.rgba8888ToColor(c, rgba);
        colors.add(c);
    }

    public Color getSelected() {
        return colors.get(selectedIndex);
    }

    /** Set selected color from HSV picker */
    public void setFromHSV(float h, float s, float v, float a) {
        this.hue = h; this.saturation = s; this.value = v; this.alpha = a;
        Color c = hsvToColor(h, s, v, a);
        colors.set(selectedIndex, c);
    }

    /** Convert HSV (h=0..360, s=0..1, v=0..1) to Color */
    public static Color hsvToColor(float h, float s, float v, float a) {
        float c = v * s;
        float x = c * (1 - Math.abs((h / 60f) % 2 - 1));
        float m = v - c;
        float r, g, b;
        if (h < 60)       { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else              { r = c; g = 0; b = x; }
        return new Color(r + m, g + m, b + m, a);
    }

    /** Convert Color to HSV */
    public static float[] colorToHSV(Color c) {
        float r = c.r, g = c.g, b = c.b;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0, s = max == 0 ? 0 : d / max, v = max;
        if (d != 0) {
            if (max == r) h = 60 * (((g - b) / d) % 6);
            else if (max == g) h = 60 * ((b - r) / d + 2);
            else h = 60 * ((r - g) / d + 4);
        }
        if (h < 0) h += 360;
        return new float[]{h, s, v, c.a};
    }
}
