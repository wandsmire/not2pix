package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mirwanda.not2pix.Not2Pix;

/**
 * Vertical toolbar on the left side, bottom-aligned.
 * Layout (top to bottom): [Pencil][Eraser][Fill][Shape][Select] ... [Undo][Redo][Menu]
 */
public class Toolbar extends UIPanel {

    private Not2Pix app;
    private float dp;

    // Indices match children list order (top to bottom)
    public static final int TOOL_PENCIL = 0;
    public static final int TOOL_ERASER = 1;
    public static final int TOOL_FILL = 2;
    public static final int TOOL_SHAPE = 3;
    public static final int TOOL_SELECT = 4;
    public static final int BTN_UNDO = 5;
    public static final int BTN_REDO = 6;
    public static final int BTN_MENU = 7;
    public static final int TOOL_OFFSET = 0;
    private static final int FIRST_TOOL = 0;
    private static final int NUM_TOOLS = 5;

    public Toolbar(Not2Pix app, float screenWidth, float screenHeight, float dp) {
        super(0, 0, 48 * dp, 0);
        this.app = app;
        this.dp = dp;
        float btnSize = 42 * dp;
        float pad = 2 * dp;
        float lw = Math.max(2f, 1.5f * dp);

        int numBtns = 8; // 5 tools + undo + redo + menu
        this.height = numBtns * (btnSize + pad) + pad;
        this.y = 0;
        this.x = 0;

        float bx = (width - btnSize) / 2f;
        // Build top-to-bottom: top-most button gets highest y
        // Pencil at top, menu at bottom
        float topY = height - btnSize - pad;

        // Pencil (top-most)
        UIButton pencil = new UIButton("Pen", bx, topY, btnSize, btnSize);
        pencil.iconDrawer = iconPencil(lw);
        pencil.selected = true;
        pencil.action = () -> { app.setTool(0); selectToolButton(TOOL_PENCIL); };
        children.add(pencil);

        // Eraser
        UIButton eraser = new UIButton("Era", bx, topY - (btnSize + pad), btnSize, btnSize);
        eraser.iconDrawer = iconEraser(lw);
        eraser.action = () -> { app.setTool(1); selectToolButton(TOOL_ERASER); };
        children.add(eraser);

        // Fill
        UIButton fill = new UIButton("Fil", bx, topY - 2 * (btnSize + pad), btnSize, btnSize);
        fill.iconDrawer = iconFill(lw);
        fill.action = () -> { app.setTool(2); selectToolButton(TOOL_FILL); };
        children.add(fill);

        // Shape
        UIButton shape = new UIButton("Shp", bx, topY - 3 * (btnSize + pad), btnSize, btnSize);
        shape.iconDrawer = iconShape(lw);
        shape.action = () -> { app.setTool(3); selectToolButton(TOOL_SHAPE); };
        children.add(shape);

        // Selection
        UIButton select = new UIButton("Sel", bx, topY - 4 * (btnSize + pad), btnSize, btnSize);
        select.iconDrawer = iconSelect(lw);
        select.action = () -> { app.setTool(4); selectToolButton(TOOL_SELECT); };
        children.add(select);

        // Undo
        UIButton undo = new UIButton("U", bx, topY - 5 * (btnSize + pad), btnSize, btnSize);
        undo.iconDrawer = iconUndo(lw);
        undo.action = () -> app.undo();
        children.add(undo);

        // Redo
        UIButton redo = new UIButton("R", bx, topY - 6 * (btnSize + pad), btnSize, btnSize);
        redo.iconDrawer = iconRedo(lw);
        redo.action = () -> app.redo();
        children.add(redo);

        // Menu (bottom-most)
        UIButton menu = new UIButton("M", bx, topY - 7 * (btnSize + pad), btnSize, btnSize);
        menu.iconDrawer = iconHamburger(lw);
        children.add(menu);
    }

    public void selectToolButton(int idx) {
        for (int i = FIRST_TOOL; i < FIRST_TOOL + NUM_TOOLS && i < children.size(); i++) {
            ((UIButton) children.get(i)).selected = (i == idx);
        }
    }

    private UIButton.IconDrawer iconHamburger(float lw) {
        return (sr, x, y, w, h, sel) -> {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(Color.LIGHT_GRAY);
            float lh = lw * 1.5f;
            sr.rect(x + w * 0.2f, y + h * 0.3f, w * 0.6f, lh);
            sr.rect(x + w * 0.2f, y + h * 0.5f - lh / 2f, w * 0.6f, lh);
            sr.rect(x + w * 0.2f, y + h * 0.7f - lh, w * 0.6f, lh);
            sr.end();
        };
    }

    private UIButton.IconDrawer iconPencil(float lw) {
        return (sr, x, y, w, h, sel) -> {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(sel ? Color.WHITE : Color.LIGHT_GRAY);
            sr.rectLine(x + w * 0.7f, y + h * 0.8f, x + w * 0.25f, y + h * 0.3f, lw * 3f);
            sr.setColor(sel ? Color.YELLOW : new Color(0.8f, 0.7f, 0.3f, 1));
            sr.triangle(x + w * 0.25f, y + h * 0.3f, x + w * 0.18f, y + h * 0.23f, x + w * 0.3f, y + h * 0.22f);
            sr.setColor(sel ? new Color(1f, 0.7f, 0.2f, 1) : new Color(0.7f, 0.5f, 0.2f, 1));
            sr.rectLine(x + w * 0.55f, y + h * 0.65f, x + w * 0.35f, y + h * 0.42f, lw * 2.5f);
            sr.end();
        };
    }

    private UIButton.IconDrawer iconEraser(float lw) {
        return (sr, x, y, w, h, sel) -> {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(sel ? new Color(1f, 0.7f, 0.8f, 1) : new Color(0.8f, 0.5f, 0.6f, 1));
            sr.rect(x + w * 0.2f, y + h * 0.3f, w * 0.6f, h * 0.4f);
            sr.setColor(sel ? new Color(0.9f, 0.4f, 0.5f, 1) : new Color(0.6f, 0.3f, 0.4f, 1));
            sr.rect(x + w * 0.2f, y + h * 0.3f, w * 0.6f, h * 0.15f);
            sr.end();
        };
    }

    private UIButton.IconDrawer iconFill(float lw) {
        return (sr, x, y, w, h, sel) -> {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(sel ? Color.WHITE : Color.LIGHT_GRAY);
            sr.rect(x + w * 0.2f, y + h * 0.35f, w * 0.4f, h * 0.35f);
            sr.rect(x + w * 0.15f, y + h * 0.65f, w * 0.5f, h * 0.08f);
            sr.setColor(sel ? Color.CYAN : new Color(0.3f, 0.6f, 1f, 1));
            sr.circle(x + w * 0.72f, y + h * 0.3f, lw * 2.5f);
            sr.end();
        };
    }

    private UIButton.IconDrawer iconShape(float lw) {
        return (sr, x, y, w, h, sel) -> {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(sel ? Color.WHITE : Color.LIGHT_GRAY);
            float m = 0.25f;
            sr.rectLine(x + w * m, y + h * m, x + w * (1 - m), y + h * m, lw * 1.5f);
            sr.rectLine(x + w * (1 - m), y + h * m, x + w * (1 - m), y + h * (1 - m), lw * 1.5f);
            sr.rectLine(x + w * (1 - m), y + h * (1 - m), x + w * m, y + h * (1 - m), lw * 1.5f);
            sr.rectLine(x + w * m, y + h * (1 - m), x + w * m, y + h * m, lw * 1.5f);
            sr.setColor(sel ? Color.YELLOW : new Color(0.7f, 0.7f, 0.3f, 1));
            sr.rectLine(x + w * 0.3f, y + h * 0.7f, x + w * 0.7f, y + h * 0.3f, lw);
            sr.end();
        };
    }

    private UIButton.IconDrawer iconSelect(float lw) {
        return (sr, x, y, w, h, sel) -> {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(sel ? Color.WHITE : Color.LIGHT_GRAY);
            float m = 0.25f, dash = w * 0.1f;
            float rx = x + w * m, ry = y + h * m, rw = w * 0.5f, rh = h * 0.5f;
            for (float dx = 0; dx < rw; dx += dash * 2) {
                float dw = Math.min(dash, rw - dx);
                sr.rect(rx + dx, ry + rh - lw, dw, lw * 1.5f);
                sr.rect(rx + dx, ry, dw, lw * 1.5f);
            }
            for (float dy = 0; dy < rh; dy += dash * 2) {
                float dh = Math.min(dash, rh - dy);
                sr.rect(rx, ry + dy, lw * 1.5f, dh);
                sr.rect(rx + rw - lw, ry + dy, lw * 1.5f, dh);
            }
            sr.setColor(sel ? Color.YELLOW : new Color(0.7f, 0.7f, 0.3f, 1));
            sr.rectLine(x + w * 0.55f, y + h * 0.45f, x + w * 0.75f, y + h * 0.25f, lw);
            sr.end();
        };
    }

    private UIButton.IconDrawer iconUndo(float lw) {
        return (sr, x, y, w, h, sel) -> {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(Color.LIGHT_GRAY);
            sr.rectLine(x + w * 0.65f, y + h * 0.5f, x + w * 0.3f, y + h * 0.5f, lw * 1.5f);
            sr.triangle(x + w * 0.3f, y + h * 0.32f, x + w * 0.3f, y + h * 0.68f, x + w * 0.17f, y + h * 0.5f);
            sr.end();
        };
    }

    private UIButton.IconDrawer iconRedo(float lw) {
        return (sr, x, y, w, h, sel) -> {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(Color.LIGHT_GRAY);
            sr.rectLine(x + w * 0.35f, y + h * 0.5f, x + w * 0.7f, y + h * 0.5f, lw * 1.5f);
            sr.triangle(x + w * 0.7f, y + h * 0.32f, x + w * 0.7f, y + h * 0.68f, x + w * 0.83f, y + h * 0.5f);
            sr.end();
        };
    }
}
