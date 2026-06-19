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
    public BottomStrip bottomStrip;
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
    public PreferencesDialog prefsDialog;
    public ColorAdjustDialog colorAdjustDialog;
    public CanvasResizeDialog canvasResizeDialog;

    private boolean touchHandled = false;
    private enum DragTarget { NONE, FRAME_STRIP, LAYER_PANEL, HSV_PICKER, MINIMAP, DOC_STRIP }
    private DragTarget activeDrag = DragTarget.NONE;

    private float touchDownTime = 0;
    private float touchDownX = 0, touchDownY = 0;
    private float lastTouchX = 0, lastTouchY = 0;
    private boolean longPressTriggered = false;
    private int longPressBtnIdx = -1;
    private float selectToolLastTap = 0;

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

        bottomStrip = new BottomStrip(app, sw, sh, dp);
        float stripH = bottomStrip.height; // offset for left/right panels

        toolbar = new Toolbar(app, sw, sh, dp, stripH);
        toolbar.selectToolButton(app.activeToolIndex);
        paletteBar = new PaletteBar(app, sw, sh, dp, stripH);
        statusBar = new StatusBar(app, sw, sh, dp);
        frameStrip = new FrameStrip(app, sw, sh, dp);
        hsvPicker = new HSVPicker(app, sw, sh, dp);
        layerPanel = new LayerPanel(app, sw, sh, dp);
        layerPanel.y = stripH; // move above bottom strip
        confirmDialog = new ConfirmDialog(dp);
        minimap = new Minimap(app, dp);
        fileMenu = new FileMenu(app, dp);
        brushSizePopup = new BrushSizePopup(app, dp);
        shapeSelector = new ShapeSelector(app, dp);
        tileSizePopup = new TileSizePopup(app, dp);
        zoomPopup = new ZoomPopup(app, dp);
        newCanvasDialog = new NewCanvasDialog(dp);
        docStrip = new DocStrip(app, sw, sh, dp);
        docStrip.onCloseDoc = () -> confirmDialog.show("Close document?", () -> app.closeDocument());
        prefsDialog = new PreferencesDialog(app, dp);
        colorAdjustDialog = new ColorAdjustDialog(dp);
        canvasResizeDialog = new CanvasResizeDialog(app, dp);

        paletteBar.onPickerOpen = () -> hsvPicker.show();
        paletteBar.onColorPickerTool = () -> {
            app.colorPickerActive = true;
            showToast("Tap canvas to pick color");
        };
        layerPanel.onDeleteLayer = () -> confirmDialog.show("Delete layer?", () -> app.removeLayer());
        layerPanel.onOpen = () -> cancelActiveSelection();
        frameStrip.onDeleteFrame = () -> confirmDialog.show("Delete frame?", () -> app.deleteFrame());

        // Wire toolbar menu button
        UIButton menuBtn = (UIButton) toolbar.children.get(Toolbar.BTN_MENU);
        menuBtn.action = () -> { cancelActiveSelection(); fileMenu.show(toolbar.width, menuBtn.y); };
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
        fileMenu.onPreferences = () -> prefsDialog.show();
        prefsDialog.onGridColor = () -> hsvPicker.showForColor(app.gridColor, () -> app.savePrefs());
        prefsDialog.onTileGridColor = () -> hsvPicker.showForColor(app.tileGridColor, () -> app.savePrefs());
        prefsDialog.onBgColor = () -> hsvPicker.showForColor(app.bgColor, () -> app.savePrefs());
        prefsDialog.onCheckerLight = () -> hsvPicker.showForColor(app.checkerLight, () -> app.savePrefs());
        prefsDialog.onCheckerDark = () -> hsvPicker.showForColor(app.checkerDark, () -> app.savePrefs());
        prefsDialog.onTileSize = () -> tileSizePopup.show(0, 0);
        prefsDialog.onMinimapSize = () -> {
            Gdx.input.getTextInput(new com.badlogic.gdx.Input.TextInputListener() {
                @Override public void input(String text) {
                    try { int v = Integer.parseInt(text.trim()); if (v >= 32 && v <= 256) { app.minimapSize = v; app.savePrefs(); } } catch (NumberFormatException e) {}
                }
                @Override public void canceled() {}
            }, "Minimap Size", String.valueOf(app.minimapSize), "32-256 dp");
        };
        fileMenu.onResizeCanvas = () -> canvasResizeDialog.show();
    }

    public void draw() {
        sr.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        toolbar.draw(sr, batch, font);
        paletteBar.draw(sr, batch, font);
        bottomStrip.draw(sr, batch, font);

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
        canvasResizeDialog.draw(sr, batch, font);
        prefsDialog.draw(sr, batch, font);
        colorAdjustDialog.draw(sr, batch, font);
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
                } else if (longPressBtnIdx == Toolbar.TOOL_FILL) {
                    com.mirwanda.not2pix.FillTool ft = (com.mirwanda.not2pix.FillTool) app.tools[2];
                    ft.clearMode = !ft.clearMode;
                    showToast(ft.clearMode ? "Fill: Clear to transparency" : "Fill: Normal");
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
        if (colorAdjustDialog.open) { colorAdjustDialog.handleTouch(tx, ty); touchHandled = true; activeDrag = DragTarget.HSV_PICKER; return true; }
        if (prefsDialog.open) { prefsDialog.handleTouch(tx, ty); touchHandled = true; return true; }
        if (newCanvasDialog.open) { newCanvasDialog.handleTouch(tx, ty); touchHandled = true; return true; }
        if (canvasResizeDialog.open) { canvasResizeDialog.handleTouch(tx, ty); touchHandled = true; return true; }
        if (brushSizePopup.open) { brushSizePopup.handleTouch(tx, ty); touchHandled = true; return true; }
        if (tileSizePopup.open) { tileSizePopup.handleTouch(tx, ty); touchHandled = true; return true; }
        if (zoomPopup.open) { zoomPopup.handleTouch(tx, ty); touchHandled = true; return true; }
        if (shapeSelector.open) { shapeSelector.handleTouch(tx, ty); touchHandled = true; return true; }
        if (fileMenu.open) { fileMenu.handleTouch(tx, ty); touchHandled = true; return true; }
        if (hsvPicker.open) { hsvPicker.handleTouch(tx, ty); touchHandled = true; activeDrag = DragTarget.HSV_PICKER; return true; }

        if (layerPanel.handleTouch(tx, ty)) { touchHandled = true; activeDrag = DragTarget.LAYER_PANEL; return true; }
        if (bottomStrip.handleTouch(tx, ty)) { touchHandled = true; return true; }
        if (paletteBar.handleTouch(tx, ty)) { touchHandled = true; return true; }
        if (docStrip.handleTouch(tx, ty)) { touchHandled = true; activeDrag = DragTarget.DOC_STRIP; return true; }
        if (frameStrip.handleTouch(tx, ty)) { touchHandled = true; activeDrag = DragTarget.FRAME_STRIP; return true; }

        // Toolbar
        UIElement hit = toolbar.hitChild(tx, ty);
        if (hit instanceof UIButton) {
            UIButton btn = (UIButton) hit;
            int btnIdx = toolbar.children.indexOf(btn);
            if (btnIdx == Toolbar.TOOL_PENCIL || btnIdx == Toolbar.TOOL_ERASER || btnIdx == Toolbar.TOOL_SHAPE || btnIdx == Toolbar.TOOL_FILL) {
                longPressBtnIdx = btnIdx;
            }
            // Double-tap select tool = select whole layer
            if (btnIdx == Toolbar.TOOL_SELECT && app.activeToolIndex == 4) {
                float now = System.nanoTime() / 1e9f;
                if (now - selectToolLastTap < 0.35f) {
                    SelectionTool sel = (SelectionTool) app.tools[4];
                    sel.hasSelection = true;
                    sel.selX = 0; sel.selY = 0;
                    sel.selW = app.canvasWidth; sel.selH = app.canvasHeight;
                    showToast("Selected all");
                    selectToolLastTap = 0;
                    touchHandled = true;
                    return true;
                }
                selectToolLastTap = now;
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
            if ((sel.hasSelection || sel.buffer != null) && handleSelectionButtons(tx, ty, sel)) {
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
            case HSV_PICKER:
                if (colorAdjustDialog.open) colorAdjustDialog.handleDrag(tx, ty);
                else hsvPicker.handleTouch(tx, ty);
                break;
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
        if (confirmDialog.open || newCanvasDialog.open || prefsDialog.open || colorAdjustDialog.open || fileMenu.open || hsvPicker.open || brushSizePopup.open || shapeSelector.open || tileSizePopup.open || zoomPopup.open) return true;
        if (statusBar.hit(tx, ty)) return true;
        if (toolbar.hit(tx, ty)) return true;
        if (paletteBar.hit(tx, ty)) return true;
        if (bottomStrip.hit(tx, ty)) return true;
        if (frameStrip.hit(tx, ty)) return true;
        if (docStrip.hit(tx, ty)) return true;
        if (layerPanel.open && tx >= layerPanel.x) return true;
        if (minimap.hit(tx, ty)) return true;
        return false;
    }

    /** Returns true if movement mode is active */
    public boolean isMovementMode() {
        return bottomStrip.movementMode;
    }

    public void resize(int width, int height) {
        dp = Math.max(1, Gdx.graphics.getDensity());
        font.getData().setScale(Math.max(1.0f, 1.1f * dp));
        buildUI();
    }

    private void drawSelectionButtons() {
        if (!(app.tools[app.activeToolIndex] instanceof SelectionTool)) return;
        SelectionTool sel = (SelectionTool) app.tools[app.activeToolIndex];
        if (!sel.hasSelection && sel.buffer == null) return;
        if (sel.drawing) return;
        if (colorAdjustDialog.open) return;
        float btnSize = 32 * dp;
        float gap = 8 * dp;
        float baseY = bottomStrip.height + 8 * dp;
        float lw = 2 * dp;

        // Row 1: Confirm + Cancel
        float row1W = btnSize * 2 + gap;
        float row1X = Gdx.graphics.getWidth() / 2f - row1W / 2f;
        float row1Y = baseY + btnSize + gap;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        // Confirm (green)
        sr.setColor(0.1f, 0.6f, 0.1f, 1);
        sr.rect(row1X, row1Y, btnSize, btnSize);
        sr.setColor(Color.WHITE);
        sr.rectLine(row1X + 8 * dp, row1Y + btnSize / 2f, row1X + btnSize / 2f - 2 * dp, row1Y + 8 * dp, lw);
        sr.rectLine(row1X + btnSize / 2f - 2 * dp, row1Y + 8 * dp, row1X + btnSize - 8 * dp, row1Y + btnSize - 8 * dp, lw);
        // Cancel (red)
        float cx = row1X + btnSize + gap;
        sr.setColor(0.6f, 0.1f, 0.1f, 1);
        sr.rect(cx, row1Y, btnSize, btnSize);
        sr.setColor(Color.WHITE);
        sr.rectLine(cx + 8 * dp, row1Y + 8 * dp, cx + btnSize - 8 * dp, row1Y + btnSize - 8 * dp, lw);
        sr.rectLine(cx + 8 * dp, row1Y + btnSize - 8 * dp, cx + btnSize - 8 * dp, row1Y + 8 * dp, lw);
        sr.end();

        // Row 2: Copy, Rot90, MirrorH, MirrorV, FreeRot, Outline, ColorAdj, Crop
        int numBtns = 8;
        float row2W = btnSize * numBtns + gap * (numBtns - 1);
        float row2X = Gdx.graphics.getWidth() / 2f - row2W / 2f;
        float row2Y = baseY;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        // Copy (blue)
        float bx = row2X;
        sr.setColor(0.15f, 0.35f, 0.7f, 1);
        sr.rect(bx, row2Y, btnSize, btnSize);
        sr.end();
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "C", bx + 10 * dp, row2Y + btnSize - 8 * dp);
        batch.end();

        sr.begin(ShapeRenderer.ShapeType.Filled);
        // Rotate 90 (orange)
        bx += btnSize + gap;
        sr.setColor(0.8f, 0.5f, 0.1f, 1);
        sr.rect(bx, row2Y, btnSize, btnSize);
        sr.setColor(Color.WHITE);
        sr.rectLine(bx + btnSize / 2f, row2Y + btnSize - 8 * dp, bx + btnSize - 8 * dp, row2Y + btnSize / 2f, lw);
        sr.rectLine(bx + btnSize - 8 * dp, row2Y + btnSize / 2f, bx + btnSize / 2f, row2Y + 8 * dp, lw);

        // Mirror H (teal)
        bx += btnSize + gap;
        sr.setColor(0.1f, 0.5f, 0.5f, 1);
        sr.rect(bx, row2Y, btnSize, btnSize);
        sr.setColor(Color.WHITE);
        sr.rectLine(bx + btnSize / 2f, row2Y + 6 * dp, bx + btnSize / 2f, row2Y + btnSize - 6 * dp, lw);
        sr.rectLine(bx + 8 * dp, row2Y + btnSize / 2f, bx + btnSize / 2f - 4 * dp, row2Y + btnSize / 2f, lw);
        sr.rectLine(bx + btnSize / 2f + 4 * dp, row2Y + btnSize / 2f, bx + btnSize - 8 * dp, row2Y + btnSize / 2f, lw);

        // Mirror V (teal)
        bx += btnSize + gap;
        sr.setColor(0.1f, 0.5f, 0.5f, 1);
        sr.rect(bx, row2Y, btnSize, btnSize);
        sr.setColor(Color.WHITE);
        sr.rectLine(bx + 6 * dp, row2Y + btnSize / 2f, bx + btnSize - 6 * dp, row2Y + btnSize / 2f, lw);
        sr.rectLine(bx + btnSize / 2f, row2Y + 8 * dp, bx + btnSize / 2f, row2Y + btnSize / 2f - 4 * dp, lw);
        sr.rectLine(bx + btnSize / 2f, row2Y + btnSize / 2f + 4 * dp, bx + btnSize / 2f, row2Y + btnSize - 8 * dp, lw);

        // Free Rotate (purple, highlight if active)
        bx += btnSize + gap;
        if (sel.freeRotateMode)
            sr.setColor(0.7f, 0.2f, 0.8f, 1);
        else
            sr.setColor(0.4f, 0.15f, 0.55f, 1);
        sr.rect(bx, row2Y, btnSize, btnSize);
        sr.setColor(Color.WHITE);
        sr.rectLine(bx + 10 * dp, row2Y + btnSize - 10 * dp, bx + btnSize - 10 * dp, row2Y + btnSize - 10 * dp, lw);
        sr.rectLine(bx + btnSize - 10 * dp, row2Y + btnSize - 10 * dp, bx + btnSize - 10 * dp, row2Y + 10 * dp, lw);
        sr.rectLine(bx + btnSize - 10 * dp, row2Y + 10 * dp, bx + 10 * dp, row2Y + 10 * dp, lw);

        // Outline (gray, star)
        bx += btnSize + gap;
        sr.setColor(0.35f, 0.35f, 0.35f, 1);
        sr.rect(bx, row2Y, btnSize, btnSize);
        sr.setColor(Color.YELLOW);
        sr.rectLine(bx + btnSize / 2f, row2Y + 8 * dp, bx + btnSize / 2f, row2Y + btnSize - 8 * dp, lw);
        sr.rectLine(bx + 8 * dp, row2Y + btnSize / 2f, bx + btnSize - 8 * dp, row2Y + btnSize / 2f, lw);

        // Color Adjust (rainbow-ish)
        bx += btnSize + gap;
        sr.setColor(0.3f, 0.4f, 0.5f, 1);
        sr.rect(bx, row2Y, btnSize, btnSize);
        sr.setColor(Color.CYAN);
        sr.rectLine(bx + 6 * dp, row2Y + btnSize * 0.3f, bx + btnSize - 6 * dp, row2Y + btnSize * 0.3f, lw);
        sr.setColor(Color.GREEN);
        sr.rectLine(bx + 6 * dp, row2Y + btnSize * 0.5f, bx + btnSize - 6 * dp, row2Y + btnSize * 0.5f, lw);
        sr.setColor(Color.RED);
        sr.rectLine(bx + 6 * dp, row2Y + btnSize * 0.7f, bx + btnSize - 6 * dp, row2Y + btnSize * 0.7f, lw);

        // Crop (dark red, scissors icon)
        bx += btnSize + gap;
        sr.setColor(0.5f, 0.2f, 0.1f, 1);
        sr.rect(bx, row2Y, btnSize, btnSize);
        sr.setColor(Color.WHITE);
        sr.rectLine(bx + 6 * dp, row2Y + 6 * dp, bx + btnSize - 6 * dp, row2Y + btnSize - 6 * dp, lw);
        sr.rectLine(bx + btnSize - 6 * dp, row2Y + 6 * dp, bx + 6 * dp, row2Y + btnSize - 6 * dp, lw);
        sr.rectLine(bx + 6 * dp, row2Y + 6 * dp, bx + 6 * dp, row2Y + btnSize - 6 * dp, lw);
        sr.rectLine(bx + btnSize - 6 * dp, row2Y + 6 * dp, bx + btnSize - 6 * dp, row2Y + btnSize - 6 * dp, lw);
        sr.end();
    }

    private boolean handleSelectionButtons(float tx, float ty, SelectionTool sel) {
        float btnSize = 32 * dp;
        float gap = 8 * dp;
        float baseY = bottomStrip.height + 8 * dp;

        // Row 1: Confirm + Cancel
        float row1W = btnSize * 2 + gap;
        float row1X = Gdx.graphics.getWidth() / 2f - row1W / 2f;
        float row1Y = baseY + btnSize + gap;

        // Confirm
        if (tx >= row1X && tx <= row1X + btnSize && ty >= row1Y && ty <= row1Y + btnSize) {
            app.undoManager.beginStroke(app.layers, app.activeLayerIndex);
            sel.commitSelection(app.layers.get(app.activeLayerIndex).pixmap);
            app.layers.get(app.activeLayerIndex).markDirty();
            app.undoManager.endStroke(app.layers, app.activeLayerIndex);
            return true;
        }
        // Cancel
        float cx = row1X + btnSize + gap;
        if (tx >= cx && tx <= cx + btnSize && ty >= row1Y && ty <= row1Y + btnSize) {
            app.undoManager.beginStroke(app.layers, app.activeLayerIndex);
            sel.cancelSelection(app.layers.get(app.activeLayerIndex).pixmap);
            app.layers.get(app.activeLayerIndex).markDirty();
            app.undoManager.endStroke(app.layers, app.activeLayerIndex);
            return true;
        }

        // Row 2: Copy, Rot90, MirrorH, MirrorV, FreeRot, Outline, ColorAdj, Crop
        int numBtns = 8;
        float row2W = btnSize * numBtns + gap * (numBtns - 1);
        float row2X = Gdx.graphics.getWidth() / 2f - row2W / 2f;
        float row2Y = baseY;

        if (ty < row2Y || ty > row2Y + btnSize) return false;

        int idx = (int) ((tx - row2X) / (btnSize + gap));
        float btnLeft = row2X + idx * (btnSize + gap);
        if (idx < 0 || idx >= numBtns) return false;
        if (tx < btnLeft || tx > btnLeft + btnSize) return false;

        // Auto-cut: if selection drawn but not yet cut into buffer, cut now
        if (sel.buffer == null && sel.hasSelection) {
            com.badlogic.gdx.graphics.Pixmap target = app.layers.get(app.activeLayerIndex).pixmap;
            app.undoManager.beginStroke(app.layers, app.activeLayerIndex);
            sel.buffer = new com.badlogic.gdx.graphics.Pixmap(sel.selW, sel.selH, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            sel.buffer.setBlending(com.badlogic.gdx.graphics.Pixmap.Blending.None);
            sel.buffer.drawPixmap(target, 0, 0, sel.selX, sel.selY, sel.selW, sel.selH);
            target.setBlending(com.badlogic.gdx.graphics.Pixmap.Blending.None);
            target.setColor(com.badlogic.gdx.graphics.Color.CLEAR);
            target.fillRectangle(sel.selX, sel.selY, sel.selW, sel.selH);
            target.setBlending(com.badlogic.gdx.graphics.Pixmap.Blending.SourceOver);
            app.undoManager.endStroke(app.layers, app.activeLayerIndex);
            app.layers.get(app.activeLayerIndex).markDirty();
        }

        if (sel.buffer == null) return true;

        switch (idx) {
            case 0: // Copy
                sel.copySelection(app.layers.get(app.activeLayerIndex).pixmap);
                app.layers.get(app.activeLayerIndex).markDirty();
                showToast("Copied");
                break;
            case 1: // Rotate 90
                sel.rotate90();
                app.layers.get(app.activeLayerIndex).markDirty();
                break;
            case 2: // Mirror H
                sel.mirrorH();
                app.layers.get(app.activeLayerIndex).markDirty();
                break;
            case 3: // Mirror V
                sel.mirrorV();
                app.layers.get(app.activeLayerIndex).markDirty();
                break;
            case 4: // Free Rotate toggle
                sel.freeRotateMode = !sel.freeRotateMode;
                showToast(sel.freeRotateMode ? "Free rotate: drag horizontally" : "Free rotate off");
                break;
            case 5: // Outline effect
                sel.applyOutline(app.palette.getSelected(), 1); showToast("Outline applied");
                break;
            case 6: // Color adjust
                colorAdjustDialog.show(sel);
                break;
            case 7: // Crop to selection
                confirmDialog.show("Crop canvas to selection?\nUndo history will be lost.", () -> {
                    if (sel.buffer != null) {
                        sel.commitSelection(app.layers.get(app.activeLayerIndex).pixmap);
                        app.layers.get(app.activeLayerIndex).markDirty();
                    }
                    app.cropToSelection(sel.selX, sel.selY, sel.selW, sel.selH);
                    sel.hasSelection = false;
                });
                break;
        }
        return true;
    }

    private void cancelActiveSelection() {
        if (app.tools[app.activeToolIndex] instanceof SelectionTool) {
            SelectionTool sel = (SelectionTool) app.tools[app.activeToolIndex];
            if (sel.hasSelection || sel.buffer != null) {
                sel.cancelSelection(app.layers.get(app.activeLayerIndex).pixmap);
                app.layers.get(app.activeLayerIndex).markDirty();
            }
        }
    }

    public void dispose() {
        sr.dispose();
        batch.dispose();
        font.dispose();
        hsvPicker.dispose();
        minimap.dispose();
    }
}
