package com.mirwanda.not2pix.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mirwanda.not2pix.Not2Pix;
import com.mirwanda.not2pix.PencilTool;
import com.mirwanda.not2pix.EraserTool;
import com.mirwanda.not2pix.SelectionTool;

public class EditorUI {

    private Not2Pix app;
    private ShapeRenderer sr;
    private SpriteBatch batch;
    private BitmapFont font;
    private float dp;

    public Toolbar toolbar;
    public PaletteBar paletteBar;
    public MovementToggle movementToggle;
    public StatusBar statusBar;
    public HSVPicker hsvPicker;
    public LayerPanel layerPanel;
    public FrameStrip frameStrip;
    public ConfirmDialog confirmDialog;
    public NewCanvasDialog newCanvasDialog;
    public Minimap minimap;
    public FileMenu fileMenu;
    public BrushSizePopup brushSizePopup;
    public ShapeSelector shapeSelector;
    public TileSizePopup tileSizePopup;
    public ZoomPopup zoomPopup;
    public DocStrip docStrip;

    private boolean touchHandled = false;
    private enum DragTarget { NONE, FRAME_STRIP, LAYER_PANEL, HSV_PICKER, MINIMAP, DOC_STRIP }
    private DragTarget activeDrag = DragTarget.NONE;

    private float touchDownTime = 0;
    private float touchDownX = 0, touchDownY = 0;
    private float lastTouchX = 0, lastTouchY = 0;
    private boolean longPressTriggered = false;
    private int longPressBtnIdx = -1;

    // Toast notification
    private String toastMessage = null;
    private float toastTimer = 0f;
    private static final float TOAST_DURATION = 3f;

    public EditorUI(Not2Pix app) {
        this.app = app;
        sr = new ShapeRenderer();
        batch = new SpriteBatch();
        dp = Math.max(1, Gdx.graphics.getDensity());
        font = new BitmapFont();
        font.getData().setScale(Math.max(1.0f, 1.1f * dp));
        font.setColor(Color.WHITE);
        buildUI();
    }

    private void buildUI() {
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        toolbar = new Toolbar(app, sw, sh, dp);
        toolbar.selectToolButton(app.activeToolIndex);
        paletteBar = new PaletteBar(app, sw, sh, dp);
        movementToggle = new MovementToggle(app, sw, sh, dp);
        statusBar = new StatusBar(app, sw, sh, dp);
        frameStrip = new FrameStrip(app, sw, sh, dp);
        hsvPicker = new HSVPicker(app, sw, sh, dp);
        layerPanel = new LayerPanel(app, sw, sh, dp);
        confirmDialog = new ConfirmDialog(dp);
        minimap = new Minimap(app, dp);
        fileMenu = new FileMenu(app, dp);
        brushSizePopup = new BrushSizePopup(app, dp);
        shapeSelector = new ShapeSelector(app, dp);
        tileSizePopup = new TileSizePopup(app, dp);
        zoomPopup = new ZoomPopup(app, dp);
        newCanvasDialog = new NewCanvasDialog(dp);
        docStrip = new DocStrip(app, sw, sh, dp);

        paletteBar.onPickerOpen = () -> hsvPicker.show();
        paletteBar.onColorPickerTool = () -> {
            app.colorPickerActive = true;
            showToast("Tap canvas to pick color");
        };
        layerPanel.onDeleteLayer = () -> confirmDialog.show("Delete layer?", () -> app.removeLayer());
        frameStrip.onDeleteFrame = () -> confirmDialog.show("Delete frame?", () -> app.deleteFrame());

        // Wire toolbar menu button
        UIButton menuBtn = (UIButton) toolbar.children.get(Toolbar.BTN_MENU);
        menuBtn.action = () -> fileMenu.show(toolbar.width, menuBtn.y);
        fileMenu.onTileGrid = () -> tileSizePopup.show(Gdx.graphics.getWidth() / 2f - 80 * dp, Gdx.graphics.getHeight() / 2f);
        fileMenu.onNew = () -> newCanvasDialog.show(
            (w, h) -> app.newCanvas(w, h),
            null);
        fileMenu.onClose = () -> confirmDialog.show("Close document?", () -> app.closeDocument());
        fileMenu.onExit = () -> confirmDialog.show("Exit app?", () -> app.closeApp());
        fileMenu.onSave = () -> confirmDialog.show("Save and back to NotTiled?", () -> { app.saveFile(); app.closeApp(); });
        fileMenu.onExportPng = () -> app.saveFileAs();
        fileMenu.onExportGif = () -> app.exportGif();
        fileMenu.onSaveAse = () -> app.saveAse();
        fileMenu.onLoadAse = () -> app.loadAse();
    }

    public void draw() {
        sr.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        toolbar.draw(sr, batch, font);
        paletteBar.draw(sr, batch, font);
        movementToggle.draw(sr, batch, font);
        frameStrip.draw(sr, batch, font);
        docStrip.draw(sr, batch, font);
        statusBar.draw(sr, batch, font);
        layerPanel.draw(sr, batch, font);
        minimap.draw(sr, batch, font);
        hsvPicker.draw(sr, batch, font);
        fileMenu.draw(sr, batch, font);
        brushSizePopup.draw(sr, batch, font);
        shapeSelector.draw(sr, batch, font);
        tileSizePopup.draw(sr, batch, font);
        zoomPopup.draw(sr, batch, font);
        newCanvasDialog.draw(sr, batch, font);
        confirmDialog.draw(sr, batch, font);

        drawBrushSizeIndicator();
        drawSelectionButtons();
        checkLongPress();
        drawToast();
    }

    public void showToast(String message) {
        toastMessage = message;
        toastTimer = TOAST_DURATION;
    }

    private void drawToast() {
        if (toastMessage == null) return;
        toastTimer -= Gdx.graphics.getDeltaTime();
        if (toastTimer <= 0) { toastMessage = null; return; }
        float alpha = Math.min(1f, toastTimer);
        float sw = Gdx.graphics.getWidth();
        float padH = 12 * dp;
        float padW = 20 * dp;
        float fh = font.getCapHeight() + padH * 2;
        float fy = Gdx.graphics.getHeight() - statusBar.height - fh - 4 * dp;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.15f, 0.05f, 0.05f, 0.92f * alpha);
        sr.rect(0, fy, sw, fh);
        sr.end();
        batch.begin();
        font.setColor(1f, 0.4f, 0.4f, alpha);
        font.draw(batch, toastMessage, padW, fy + fh - padH);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void drawBrushSizeIndicator() {
        int toolIdx = app.activeToolIndex;
        if (toolIdx > 1) return;
        int size = 1;
        if (app.tools[toolIdx] instanceof PencilTool) size = ((PencilTool) app.tools[toolIdx]).brushSize;
        else if (app.tools[toolIdx] instanceof EraserTool) size = ((EraserTool) app.tools[toolIdx]).brushSize;
        if (size <= 1) return;
        UIElement btn = toolbar.children.get(Toolbar.TOOL_OFFSET + toolIdx);
        batch.begin();
        font.setColor(Color.YELLOW);
        font.draw(batch, String.valueOf(size), btn.x + btn.width - 14 * dp, btn.y + 16 * dp);
        batch.end();
    }

    private void checkLongPress() {
        if (longPressTriggered || longPressBtnIdx < 0) return;
        if (Gdx.input.isTouched(0)) {
            float elapsed = (System.nanoTime() / 1e9f) - touchDownTime;
            float dx = Math.abs(Gdx.input.getX(0) - touchDownX);
            float dy = Math.abs(Gdx.input.getY(0) - touchDownY);
            if (elapsed >= 0.5f && dx < 12 * dp && dy < 12 * dp) {
                longPressTriggered = true;
                UIElement btn = toolbar.children.get(longPressBtnIdx);
                if (longPressBtnIdx == Toolbar.TOOL_PENCIL || longPressBtnIdx == Toolbar.TOOL_ERASER) {
                    brushSizePopup.show(toolbar.width + 4 * dp, btn.y);
                } else if (longPressBtnIdx == Toolbar.TOOL_SHAPE) {
                    shapeSelector.show(toolbar.width + 4 * dp, btn.y);
                }
            }
        }
    }

    public boolean touchDown(float screenX, float screenY) {
        float tx = screenX;
        float ty = Gdx.graphics.getHeight() - screenY;

        touchDownTime = System.nanoTime() / 1e9f;
        touchDownX = screenX;
        touchDownY = screenY;
        lastTouchX = tx;
        lastTouchY = ty;
        longPressTriggered = false;
        longPressBtnIdx = -1;

        // Modals
        if (confirmDialog.open) { confirmDialog.handleTouch(tx, ty); touchHandled = true; return true; }
        if (newCanvasDialog.open) { newCanvasDialog.handleTouch(tx, ty); touchHandled = true; return true; }
        if (brushSizePopup.open) { brushSizePopup.handleTouch(tx, ty); touchHandled = true; return true; }
        if (tileSizePopup.open) { tileSizePopup.handleTouch(tx, ty); touchHandled = true; return true; }
        if (zoomPopup.open) { zoomPopup.handleTouch(tx, ty); touchHandled = true; return true; }
        if (shapeSelector.open) { shapeSelector.handleTouch(tx, ty); touchHandled = true; return true; }
        if (fileMenu.open) { fileMenu.handleTouch(tx, ty); touchHandled = true; return true; }
        if (hsvPicker.open) { hsvPicker.handleTouch(tx, ty); touchHandled = true; activeDrag = DragTarget.HSV_PICKER; return true; }

        if (layerPanel.handleTouch(tx, ty)) { touchHandled = true; activeDrag = DragTarget.LAYER_PANEL; return true; }
        if (movementToggle.handleTouch(tx, ty)) { touchHandled = true; return true; }
        if (paletteBar.handleTouch(tx, ty)) { touchHandled = true; return true; }
        if (docStrip.handleTouch(tx, ty)) { touchHandled = true; activeDrag = DragTarget.DOC_STRIP; return true; }
        if (frameStrip.handleTouch(tx, ty)) { touchHandled = true; activeDrag = DragTarget.FRAME_STRIP; return true; }

        // Toolbar
        UIElement hit = toolbar.hitChild(tx, ty);
        if (hit instanceof UIButton) {
            UIButton btn = (UIButton) hit;
            int btnIdx = toolbar.children.indexOf(btn);
            if (btnIdx == Toolbar.TOOL_PENCIL || btnIdx == Toolbar.TOOL_ERASER || btnIdx == Toolbar.TOOL_SHAPE) {
                longPressBtnIdx = btnIdx;
            }
            if (btn.action != null) btn.action.run();
            touchHandled = true;
            return true;
        }
        if (toolbar.hit(tx, ty)) { touchHandled = true; return true; }

        // Status bar absorbs touch (notch area)
        if (statusBar.hit(tx, ty)) { touchHandled = true; return true; }

        // Minimap drag
        if (minimap.hit(tx, ty)) {
            minimap.handleTouch(tx, ty);
            activeDrag = DragTarget.MINIMAP;
            touchHandled = true;
            return true;
        }

        // Selection confirm/cancel buttons
        if (app.tools[app.activeToolIndex] instanceof SelectionTool) {
            SelectionTool sel = (SelectionTool) app.tools[app.activeToolIndex];
            if (sel.buffer != null && handleSelectionButtons(tx, ty, sel)) {
                touchHandled = true;
                return true;
            }
        }

        // Tapped canvas - close layer panel if open
        if (layerPanel.open) {
            layerPanel.open = false;
            touchHandled = true;
            return true;
        }

        touchHandled = false;
        activeDrag = DragTarget.NONE;
        return false;
    }

    public boolean touchDragged(float screenX, float screenY) {
        if (!touchHandled) return false;
        float tx = screenX;
        float ty = Gdx.graphics.getHeight() - screenY;
        lastTouchX = tx;
        lastTouchY = ty;
        switch (activeDrag) {
            case HSV_PICKER: hsvPicker.handleTouch(tx, ty); break;
            case FRAME_STRIP: frameStrip.handleDrag(tx, ty); break;
            case LAYER_PANEL: layerPanel.handleDrag(tx, ty); break;
            case MINIMAP: minimap.handleTouch(tx, ty); break;
            case DOC_STRIP: docStrip.handleDrag(tx, ty); break;
            default: break;
        }
        return true;
    }

    public boolean touchUp() {
        if (activeDrag == DragTarget.FRAME_STRIP) frameStrip.handleUp();
        if (activeDrag == DragTarget.LAYER_PANEL) layerPanel.handleUp();
        if (activeDrag == DragTarget.DOC_STRIP) docStrip.handleUp(lastTouchX, lastTouchY);
        paletteBar.touchReleased();
        boolean was = touchHandled;
        touchHandled = false;
        activeDrag = DragTarget.NONE;
        longPressBtnIdx = -1;
        return was;
    }

    public boolean isOverUI(float screenX, float screenY) {
        float tx = screenX;
        float ty = Gdx.graphics.getHeight() - screenY;
        if (confirmDialog.open || newCanvasDialog.open || fileMenu.open || hsvPicker.open || brushSizePopup.open || shapeSelector.open || tileSizePopup.open || zoomPopup.open) return true;
        if (statusBar.hit(tx, ty)) return true;
        if (toolbar.hit(tx, ty)) return true;
        if (paletteBar.hit(tx, ty)) return true;
        if (movementToggle.hit(tx, ty)) return true;
        if (frameStrip.hit(tx, ty)) return true;
        if (docStrip.hit(tx, ty)) return true;
        if (layerPanel.open && tx >= layerPanel.x) return true;
        if (minimap.hit(tx, ty)) return true;
        return false;
    }

    /** Returns true if movement mode is active */
    public boolean isMovementMode() {
        return movementToggle.movementMode;
    }

    public void resize(int width, int height) {
        dp = Math.max(1, Gdx.graphics.getDensity());
        font.getData().setScale(Math.max(1.0f, 1.1f * dp));
        buildUI();
    }

    private void drawSelectionButtons() {
        if (!(app.tools[app.activeToolIndex] instanceof SelectionTool)) return;
        SelectionTool sel = (SelectionTool) app.tools[app.activeToolIndex];
        if (sel.buffer == null) return;
        float btnSize = 32 * dp;
        float bx = Gdx.graphics.getWidth() / 2f - btnSize - 4 * dp;
        float by = movementToggle.y + movementToggle.height + 8 * dp;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.1f, 0.6f, 0.1f, 1);
        sr.rect(bx, by, btnSize, btnSize);
        sr.setColor(Color.WHITE);
        sr.rectLine(bx + 8 * dp, by + btnSize / 2f, bx + btnSize / 2f - 2 * dp, by + 8 * dp, 2 * dp);
        sr.rectLine(bx + btnSize / 2f - 2 * dp, by + 8 * dp, bx + btnSize - 8 * dp, by + btnSize - 8 * dp, 2 * dp);
        float cx = bx + btnSize + 8 * dp;
        sr.setColor(0.6f, 0.1f, 0.1f, 1);
        sr.rect(cx, by, btnSize, btnSize);
        sr.setColor(Color.WHITE);
        sr.rectLine(cx + 8 * dp, by + 8 * dp, cx + btnSize - 8 * dp, by + btnSize - 8 * dp, 2 * dp);
        sr.rectLine(cx + 8 * dp, by + btnSize - 8 * dp, cx + btnSize - 8 * dp, by + 8 * dp, 2 * dp);
        sr.end();
    }

    private boolean handleSelectionButtons(float tx, float ty, SelectionTool sel) {
        float btnSize = 32 * dp;
        float bx = Gdx.graphics.getWidth() / 2f - btnSize - 4 * dp;
        float by = movementToggle.y + movementToggle.height + 8 * dp;
        if (tx >= bx && tx <= bx + btnSize && ty >= by && ty <= by + btnSize) {
            sel.commitSelection(app.layers.get(app.activeLayerIndex).pixmap);
            app.layers.get(app.activeLayerIndex).markDirty();
            return true;
        }
        float cx = bx + btnSize + 8 * dp;
        if (tx >= cx && tx <= cx + btnSize && ty >= by && ty <= by + btnSize) {
            sel.cancelSelection(app.layers.get(app.activeLayerIndex).pixmap);
            app.layers.get(app.activeLayerIndex).markDirty();
            return true;
        }
        return false;
    }

    public void dispose() {
        sr.dispose();
        batch.dispose();
        font.dispose();
        hsvPicker.dispose();
        minimap.dispose();
    }
}
