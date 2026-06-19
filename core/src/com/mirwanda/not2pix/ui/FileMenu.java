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
    private ArrayList<String> labels = new ArrayList<>();
    private ArrayList<Runnable> actions = new ArrayList<>();

    public FileMenu(Not2Pix app, float dp) {
        this.app = app;
        this.dp = dp;
        this.w = 145 * dp;
        this.itemH = 38 * dp;
    }

    private void buildItems() {
        labels.clear();
        actions.clear();
        boolean fromNotTiled = app.isFromNotTiled();

        if (!fromNotTiled) {
            labels.add("New"); actions.add(() -> { if (onNew != null) onNew.run(); });
            labels.add("Open"); actions.add(() -> app.openFile());
        }
        if (fromNotTiled) {
            labels.add("Save & Back"); actions.add(() -> { if (onSave != null) onSave.run(); });
        } else {
            labels.add("Save"); actions.add(() -> app.saveFile());
        }
        labels.add("Save As"); actions.add(() -> app.saveFileAs());
        labels.add("Close"); actions.add(() -> { if (onClose != null) onClose.run(); });
        labels.add("Export PNG"); actions.add(() -> { if (onExportPng != null) onExportPng.run(); });
        labels.add("Export GIF"); actions.add(() -> { if (onExportGif != null) onExportGif.run(); });
        labels.add("Save .ase"); actions.add(() -> { if (onSaveAse != null) onSaveAse.run(); });
        labels.add("Load .ase"); actions.add(() -> { if (onLoadAse != null) onLoadAse.run(); });
        labels.add("Preferences"); actions.add(() -> { if (onPreferences != null) onPreferences.run(); });
        labels.add("Exit"); actions.add(() -> { if (onExit != null) onExit.run(); });

        h = labels.size() * itemH;
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
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0, 0, 0, 0.5f);
        sr.rect(0, 0, sw, sh);
        sr.setColor(0.18f, 0.18f, 0.18f, 1);
        sr.rect(x, y, w, h);
        sr.end();

        batch.begin();
        for (int i = 0; i < labels.size(); i++) {
            float iy = y + h - (i + 1) * itemH;
            String lbl = labels.get(i);
            if (lbl.equals("---")) {
                font.setColor(Color.GRAY);
            } else {
                font.setColor(Color.WHITE);
            }
            GlyphLayout gl = new GlyphLayout(font, lbl);
            font.draw(batch, lbl, x + 10 * dp, iy + (itemH + gl.height) / 2f);
        }
        batch.end();
    }

    public boolean handleTouch(float tx, float ty) {
        if (!open) return false;
        if (tx >= x && tx <= x + w && ty >= y && ty <= y + h) {
            int idx = (int) ((y + h - ty) / itemH);
            if (idx >= 0 && idx < actions.size()) {
                Runnable action = actions.get(idx);
                if (action != null) action.run();
            }
        }
        open = false;
        return true;
    }
}
