package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.mirwanda.not2pix.Not2Pix;

import java.util.ArrayList;

public class TopToolbar extends UIPanel {

    private Not2Pix app;
    private float dp;
    private float scrollX = 0f;
    private float lastDragX = -1f;
    private boolean dragging = false;
    private float buttonW;
    private float buttonH;
    private float gap;

    public TopToolbar(Not2Pix app, float screenWidth, float screenHeight, float dp) {
        super(0, screenHeight - 28 * dp - 28 * dp - 40 * dp, screenWidth, 40 * dp);
        this.app = app;
        this.dp = dp;
        this.buttonW = 76 * dp;
        this.buttonH = 32 * dp;
        this.gap = 4 * dp;
        this.bgColor = new Color(0.12f, 0.12f, 0.12f, 0.95f);
        
        rebuildButtons();
    }

    public void rebuildButtons() {
        children.clear();

        // 1. Fit to Width
        UIButton btnFit = new UIButton("Fit W", 0, 0, buttonW, buttonH);
        btnFit.action = () -> app.fitToWidth();
        children.add(btnFit);

        // 2. Background Tool Select
        UIButton btnBg = new UIButton("Bg Tool", 0, 0, buttonW, buttonH);
        btnBg.action = () -> {
            app.setTool(5);
            if (app.bgTraceTexture == null) {
                app.platform.selectBackgroundImage();
            }
        };
        children.add(btnBg);

        // 3. Select Bg (only active/visible when Bg Tool is selected)
        UIButton btnSelectBg = new UIButton("Set Bg", 0, 0, buttonW, buttonH);
        btnSelectBg.action = () -> app.platform.selectBackgroundImage();
        children.add(btnSelectBg);

        // 4. Remove Bg (only active/visible when Bg Tool is selected)
        UIButton btnRemoveBg = new UIButton("Clear Bg", 0, 0, buttonW, buttonH);
        btnRemoveBg.action = () -> app.removeBackgroundImage();
        children.add(btnRemoveBg);

        // 5. Mirror X (Toggle)
        UIButton btnMirrorX = new UIButton("Mirror X", 0, 0, buttonW, buttonH);
        btnMirrorX.toggle = true;
        btnMirrorX.action = () -> {
            app.mirrorX = !app.mirrorX;
            app.savePrefs();
        };
        children.add(btnMirrorX);

        // 6. Mirror Y (Toggle)
        UIButton btnMirrorY = new UIButton("Mirror Y", 0, 0, buttonW, buttonH);
        btnMirrorY.toggle = true;
        btnMirrorY.action = () -> {
            app.mirrorY = !app.mirrorY;
            app.savePrefs();
        };
        children.add(btnMirrorY);

        // 7. Onion Skin (Toggle)
        UIButton btnOnion = new UIButton("Onion", 0, 0, buttonW, buttonH);
        btnOnion.toggle = true;
        btnOnion.action = () -> {
            app.onionSkin = !app.onionSkin;
            app.savePrefs();
        };
        children.add(btnOnion);

        // 8. Minimap (Toggle)
        UIButton btnMinimap = new UIButton("Minimap", 0, 0, buttonW, buttonH);
        btnMinimap.toggle = true;
        btnMinimap.action = () -> {
            app.showMinimap = !app.showMinimap;
            app.savePrefs();
        };
        children.add(btnMinimap);

        // 9. Grid (Toggle)
        UIButton btnGrid = new UIButton("Grid", 0, 0, buttonW, buttonH);
        btnGrid.toggle = true;
        btnGrid.action = () -> {
            app.showGrid = !app.showGrid;
            app.savePrefs();
        };
        children.add(btnGrid);

        // 10. Tile Size Button (Tile Grid Size Selection)
        UIButton btnTile = new UIButton("Tile Size", 0, 0, buttonW, buttonH);
        btnTile.action = () -> {
            if (app.ui.tileSizePopup != null) {
                app.ui.tileSizePopup.show(x + width / 2f - 80 * dp, y - 60 * dp);
            }
        };
        children.add(btnTile);
    }

    private void updateButtonStates() {
        for (UIElement child : children) {
            if (!(child instanceof UIButton)) continue;
            UIButton btn = (UIButton) child;
            if (btn.label.equals("Bg Tool")) {
                btn.selected = (app.activeToolIndex == 5);
            } else if (btn.label.equals("Mirror X")) {
                btn.selected = app.mirrorX;
            } else if (btn.label.equals("Mirror Y")) {
                btn.selected = app.mirrorY;
            } else if (btn.label.equals("Onion")) {
                btn.selected = app.onionSkin;
            } else if (btn.label.equals("Minimap")) {
                btn.selected = app.showMinimap;
            } else if (btn.label.equals("Grid")) {
                btn.selected = app.showGrid;
            }
        }
    }

    private ArrayList<UIElement> getActiveChildren() {
        ArrayList<UIElement> active = new ArrayList<>();
        for (UIElement c : children) {
            if (!(c instanceof UIButton)) continue;
            UIButton btn = (UIButton) c;
            
            if (btn.label.equals("Set Bg") || btn.label.equals("Clear Bg")) {
                if (app.activeToolIndex != 5) continue;
            }
            active.add(c);
        }
        return active;
    }

    @Override
    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!visible) return;

        updateButtonStates();

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(bgColor);
        sr.rect(x, y, width, height);
        sr.end();

        ArrayList<UIElement> activeList = getActiveChildren();
        float by = y + (height - buttonH) / 2f;
        float startX = 6 * dp;
        
        for (int i = 0; i < activeList.size(); i++) {
            UIElement btn = activeList.get(i);
            btn.x = x + startX + i * (buttonW + gap) - scrollX;
            btn.y = by;
            btn.width = buttonW;
            btn.height = buttonH;
            
            if (btn.x + btn.width >= x && btn.x <= x + width) {
                btn.draw(sr, batch, font);
            }
        }

        float contentW = activeList.size() * (buttonW + gap) + 12 * dp;
        if (contentW > width) {
            float thumbW = width / contentW * width;
            float thumbX = x + (scrollX / contentW) * width;
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0.4f, 0.4f, 0.4f, 0.6f);
            sr.rect(thumbX, y, thumbW, 2 * dp);
            sr.end();
        }
    }

    public boolean handleTouch(float tx, float ty) {
        if (!hit(tx, ty)) return false;
        lastDragX = tx;
        dragging = false;
        return true;
    }

    public void handleDrag(float tx, float ty) {
        if (lastDragX < 0) return;
        float dx = tx - lastDragX;
        if (Math.abs(dx) > 4 * dp) dragging = true;
        if (dragging) {
            ArrayList<UIElement> activeList = getActiveChildren();
            float contentW = activeList.size() * (buttonW + gap) + 12 * dp;
            float maxScroll = Math.max(0, contentW - width);
            scrollX = MathUtils.clamp(scrollX - dx, 0, maxScroll);
        }
        lastDragX = tx;
    }

    public void handleUp(float tx, float ty) {
        if (!dragging) {
            ArrayList<UIElement> activeList = getActiveChildren();
            for (UIElement c : activeList) {
                if (c.hit(tx, ty) && c instanceof UIButton) {
                    UIButton btn = (UIButton) c;
                    if (btn.action != null) {
                        btn.action.run();
                    }
                    break;
                }
            }
        }
        lastDragX = -1f;
        dragging = false;
    }
}
