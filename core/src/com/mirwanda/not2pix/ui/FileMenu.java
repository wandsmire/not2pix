package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mirwanda.not2pix.Not2Pix;

import java.util.ArrayList;

public class FileMenu {

    private Not2Pix app;
    public boolean open = false;
    private float x, y, w, h;
    private float itemH;
    private float dp;
    public Runnable onTileGrid;
    public Runnable onNew;
    public Runnable onClose;
    public Runnable onSave;
    public Runnable onExit;
    public Runnable onExportPng;
    public Runnable onExportGif;
    public Runnable onSaveAse;
    public Runnable onLoadAse;
    public Runnable onPreferences;
    public Runnable onResizeCanvas;

    private static class MenuItem {
        String label;
        Runnable action;
        boolean hasSubmenu;
        ArrayList<MenuItem> submenu = new ArrayList<>();
        boolean expanded = false;

        MenuItem(String label, Runnable action) {
            this.label = label;
            this.action = action;
            this.hasSubmenu = false;
        }

        MenuItem(String label) {
            this.label = label;
            this.hasSubmenu = true;
        }
    }

    private ArrayList<MenuItem> menuItems = new ArrayList<>();

    public FileMenu(Not2Pix app, float dp) {
        this.app = app;
        this.dp = dp;
        this.w = 160 * dp;
        this.itemH = 38 * dp;
    }

    private void buildItems() {
        menuItems.clear();
        boolean fromNotTiled = app.isFromNotTiled();

        if (!fromNotTiled) {
            menuItems.add(new MenuItem("New", () -> { if (onNew != null) onNew.run(); }));
            
            MenuItem openMenu = new MenuItem("Open");
            openMenu.submenu.add(new MenuItem("  Open PNG", () -> app.openFile()));
            openMenu.submenu.add(new MenuItem("  Open ASE", () -> { if (onLoadAse != null) onLoadAse.run(); }));
            menuItems.add(openMenu);
        }

        if (fromNotTiled) {
            menuItems.add(new MenuItem("Save & Back", () -> { if (onSave != null) onSave.run(); }));
        } else {
            MenuItem saveMenu = new MenuItem("Save");
            saveMenu.submenu.add(new MenuItem("  Save", () -> app.saveFile()));
            saveMenu.submenu.add(new MenuItem("  Save As PNG", () -> app.saveFileAs()));
            saveMenu.submenu.add(new MenuItem("  Save ASE", () -> { if (onSaveAse != null) onSaveAse.run(); }));
            menuItems.add(saveMenu);
        }

        MenuItem exportMenu = new MenuItem("Export");
        exportMenu.submenu.add(new MenuItem("  Export PNG", () -> { if (onExportPng != null) onExportPng.run(); }));
        exportMenu.submenu.add(new MenuItem("  Export GIF", () -> { if (onExportGif != null) onExportGif.run(); }));
        menuItems.add(exportMenu);

        menuItems.add(new MenuItem("Close", () -> { if (onClose != null) onClose.run(); }));
        menuItems.add(new MenuItem("Resize Canvas", () -> { if (onResizeCanvas != null) onResizeCanvas.run(); }));
        menuItems.add(new MenuItem("Preference", () -> { if (onPreferences != null) onPreferences.run(); }));
        menuItems.add(new MenuItem("Exit", () -> { if (onExit != null) onExit.run(); }));

        recalculateHeight();
    }

    private ArrayList<MenuItem> getVisibleItems() {
        ArrayList<MenuItem> visible = new ArrayList<>();
        for (MenuItem item : menuItems) {
            visible.add(item);
            if (item.hasSubmenu && item.expanded) {
                for (MenuItem sub : item.submenu) {
                    visible.add(sub);
                }
            }
        }
        return visible;
    }

    private void recalculateHeight() {
        h = getVisibleItems().size() * itemH;
    }

    public void show(float bx, float by) {
        buildItems();
        this.x = bx;
        this.y = by;
        open = true;
    }

    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (!open) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();

        recalculateHeight();

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0, 0, 0, 0.5f);
        sr.rect(0, 0, sw, sh);
        sr.setColor(0.18f, 0.18f, 0.18f, 1);
        sr.rect(x, y, w, h);
        sr.end();

        batch.begin();
        ArrayList<MenuItem> visible = getVisibleItems();
        for (int i = 0; i < visible.size(); i++) {
            float iy = y + h - (i + 1) * itemH;
            MenuItem item = visible.get(i);
            
            if (item.label.startsWith("  ")) {
                font.setColor(Color.LIGHT_GRAY);
            } else {
                font.setColor(Color.WHITE);
            }
            
            String displayLabel = item.label;
            if (item.hasSubmenu) {
                displayLabel += item.expanded ? " v" : " >";
            }
            
            GlyphLayout gl = new GlyphLayout(font, displayLabel);
            font.draw(batch, displayLabel, x + 10 * dp, iy + (itemH + gl.height) / 2f);
        }
        batch.end();
    }

    public boolean handleTouch(float tx, float ty) {
        if (!open) return false;
        ArrayList<MenuItem> visible = getVisibleItems();
        if (tx >= x && tx <= x + w && ty >= y && ty <= y + h) {
            int idx = (int) ((y + h - ty) / itemH);
            if (idx >= 0 && idx < visible.size()) {
                MenuItem item = visible.get(idx);
                if (item.hasSubmenu) {
                    item.expanded = !item.expanded;
                    recalculateHeight();
                    return true;
                } else {
                    if (item.action != null) {
                        item.action.run();
                    }
                }
            }
        }
        open = false;
        return true;
    }
}
