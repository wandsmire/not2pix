package com.mirwanda.not2pix;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.Preferences;
import com.mirwanda.not2pix.ui.EditorUI;
import java.util.ArrayList;

/**
 * Not2Pix - Pixel art editor core.
 */
public class Not2Pix extends ApplicationAdapter {

    public PlatformBridge platform;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;

    // Canvas
    public int canvasWidth = 32;
    public int canvasHeight = 32;
    private String intentFilePath;

    // Layers
    public ArrayList<Layer> layers = new ArrayList<>();
    public int activeLayerIndex = 0;

    // Tools
    public Tool[] tools;
    public int activeToolIndex = 0;
    public Palette palette;
    public UndoManager undoManager;

    // Zoom/Pan
    private static final float[] ZOOM_LEVELS = {1, 2, 4, 8, 16, 32, 64, 128};
    private int zoomIndex = 0;
    private float zoom = 1f;
    private float minZoom = 0.1f, maxZoom = 50f;
    private float panX = 0, panY = 0;
    private boolean isPanning = false;
    private int pointerCount = 0;

    // Grid
    public boolean showGrid = true;
    public int tileSize = 0;
    public Color gridColor = new Color(0, 0, 0, 0.5f);
    public Color tileGridColor = new Color(0, 0, 0, 0.7f);
    public Color bgColor = new Color(0.12f, 0.12f, 0.12f, 1f);
    public Color checkerLight = new Color(0.9f, 0.9f, 0.9f, 1f);
    public Color checkerDark = new Color(0.7f, 0.7f, 0.7f, 1f);

    // Minimap settings
    public boolean showMinimap = true;
    public int minimapSize = 96; // dp units
    public boolean frameStripOpen = false;

    // Mirror modifiers
    public boolean mirrorX = false;
    public boolean mirrorY = false;

    // Animation
    public ArrayList<AnimFrame> frames = new ArrayList<>();
    public int currentFrameIndex = 0;
    public boolean playing = false;
    public float frameRate = 8f; // fps
    private float animTimer = 0;
    public boolean onionSkin = false;

    // Background Trace Image
    public Texture bgTraceTexture;
    public float bgTraceX = 0f;
    public float bgTraceY = 0f;
    public float bgTraceWidth = 0f;
    public float bgTraceHeight = 0f;
    public float bgTraceOpacity = 0.6f;

    // Multi-document
    public ArrayList<Document> documents = new ArrayList<>();
    public int activeDocIndex = 0;

    // Touch state
    private boolean touching = false;
    private float lastPanX, lastPanY;
    public EditorUI ui;
    private boolean uiConsuming = false;
    public boolean colorPickerActive = false;

    // Movement mode touch state
    private float touchStartTime = 0;
    private float touchStartX = 0, touchStartY = 0;
    private boolean longPressDrawing = false;
    private static final float LONG_PRESS_DRAW_TIME = 0.3f;

    // Pinch zoom state
    private float initialPinchDist = 0;
    private float initialPinchZoom = 0;

    public Not2Pix(PlatformBridge platform) {
        this.platform = platform;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();

        palette = new Palette();
        loadPrefs();
        undoManager = new UndoManager();
        undoManager.setCanvasSize(canvasWidth, canvasHeight);
        tools = new Tool[]{ new PencilTool(), new EraserTool(), new FillTool(), new ShapeTool(), new SelectionTool(), new BackgroundTool(this) };

        intentFilePath = platform.getIntentFilePath();

        if (intentFilePath != null) {
            loadImage(intentFilePath);
        } else {
            createBlankCanvas(canvasWidth, canvasHeight);
        }

        // Create first animation frame — but only if loadImage didn't already populate one
        if (frames.isEmpty()) {
            frames.add(new AnimFrame(canvasWidth, canvasHeight));
        }

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Initialize UI
        ui = new EditorUI(this);

        fitToWidth();

        // Wrap initial state into a Document
        Document initialDoc = new Document("Untitled 1", canvasWidth, canvasHeight);
        initialDoc.layers.get(0).dispose();
        initialDoc.layers.clear();
        initialDoc.frames.get(0).dispose();
        initialDoc.frames.clear();
        initialDoc.layers = layers;
        initialDoc.activeLayerIndex = activeLayerIndex;
        initialDoc.frames = frames;
        initialDoc.currentFrameIndex = currentFrameIndex;
        initialDoc.undoManager = undoManager;
        initialDoc.frameRate = frameRate;
        initialDoc.onionSkin = onionSkin;
        initialDoc.canvasWidth = canvasWidth;
        initialDoc.canvasHeight = canvasHeight;
        if (intentFilePath != null) {
            String fn = intentFilePath.substring(intentFilePath.lastIndexOf('/') + 1);
            if (fn.contains(".")) fn = fn.substring(0, fn.lastIndexOf('.'));
            initialDoc.name = fn;
        } else {
            initialDoc.name = "Untitled 1";
        }
        documents.add(initialDoc);
        activeDocIndex = 0;
    }

    /** Maximum texture dimension the GPU safely supports (conservative default). */
    private static final int MAX_TEXTURE_SIZE = 4096;

    private void loadImage(String path) {
        try {
            Pixmap loaded = new Pixmap(Gdx.files.absolute(path));
            int w = loaded.getWidth();
            int h = loaded.getHeight();
            if (w > MAX_TEXTURE_SIZE || h > MAX_TEXTURE_SIZE) {
                Gdx.app.error("Not2Pix", "Image too large for GPU texture: " + w + "x" + h +
                        " (max " + MAX_TEXTURE_SIZE + "x" + MAX_TEXTURE_SIZE + ")");
                loaded.dispose();
                createBlankCanvas(canvasWidth, canvasHeight);
                // Show a warning via the UI after it initialises
                Gdx.app.postRunnable(() -> {
                    if (ui != null) ui.showToast("Image too large (max " + MAX_TEXTURE_SIZE + "px)");
                });
                return;
            }
            canvasWidth = w;
            canvasHeight = h;
            // Ensure RGBA8888 format — other formats cause crashes in undo/draw
            if (loaded.getFormat() != Pixmap.Format.RGBA8888) {
                Pixmap converted = new Pixmap(w, h, Pixmap.Format.RGBA8888);
                converted.setBlending(Pixmap.Blending.None);
                converted.drawPixmap(loaded, 0, 0);
                loaded.dispose();
                loaded = converted;
            }
            layers.clear();
            Layer l = new Layer("Background", canvasWidth, canvasHeight);
            l.pixmap.dispose();
            l.pixmap = loaded;
            l.markDirty();
            l.refreshTexture();
            layers.add(l);
            // Populate the first animation frame from the loaded image
            frames.clear();
            AnimFrame af = new AnimFrame(canvasWidth, canvasHeight);
            af.pixmap.setBlending(Pixmap.Blending.None);
            af.pixmap.drawPixmap(loaded, 0, 0);
            af.pixmap.setBlending(Pixmap.Blending.SourceOver);
            af.refreshTexture();
            frames.add(af);
        } catch (Exception e) {
            Gdx.app.error("Not2Pix", "Failed to load: " + path, e);
            createBlankCanvas(canvasWidth, canvasHeight);
        }
    }

    private void createBlankCanvas(int w, int h) {
        layers.clear();
        layers.add(new Layer("Background", w, h));
        activeLayerIndex = 0;
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
        camera.position.set(canvasWidth / 2f, canvasHeight / 2f, 0);
        camera.update();
        if (ui != null) ui.resize(width, height);
        fitToWidth();
    }

    @Override
    public void render() {
        // Animation playback
        if (playing && !frames.isEmpty()) {
            animTimer += Gdx.graphics.getDeltaTime();
            if (animTimer >= 1f / frameRate) {
                animTimer = 0;
                currentFrameIndex = (currentFrameIndex + 1) % frames.size();
                loadFrame(currentFrameIndex);
            }
        }

        handleInput();

        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, bgColor.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Set camera zoom/pan
        camera.zoom = 1f / zoom;
        camera.position.set(canvasWidth / 2f - panX, canvasHeight / 2f - panY, 0);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Draw checkerboard background
        drawCheckerboard();

        // Draw background trace image
        if (bgTraceTexture != null) {
            batch.begin();
            batch.setColor(1, 1, 1, bgTraceOpacity);
            batch.draw(bgTraceTexture, bgTraceX, bgTraceY, bgTraceWidth, bgTraceHeight);
            batch.setColor(1, 1, 1, 1);
            batch.end();
        }

        // Draw onion skin (previous frame, semi-transparent)
        if (onionSkin && currentFrameIndex > 0 && frames.size() > 1) {
            batch.begin();
            batch.setColor(1, 1, 1, 0.3f);
            AnimFrame prev = frames.get(currentFrameIndex - 1);
            if (prev.texture != null) {
                batch.draw(prev.texture, 0, 0);
            }
            batch.setColor(1, 1, 1, 1);
            batch.end();
        }

        // Refresh dirty layer textures BEFORE starting the batch — texture.draw()
        // calls bind()+glTexSubImage2D which would corrupt SpriteBatch state if
        // done between batch.begin()/end().
        for (int i = 0; i < layers.size(); i++) {
            layers.get(i).refreshTexture();
        }

        // Draw layers
        batch.begin();
        for (int i = 0; i < layers.size(); i++) {
            Layer l = layers.get(i);
            if (!l.visible) continue;
            batch.setColor(1, 1, 1, l.opacity);
            batch.draw(l.texture, 0, 0);
        }
        batch.setColor(1, 1, 1, 1);
        batch.end();

        // Draw pencil stroke preview (pixel-perfect mode: not committed yet)
        Tool t = tools[activeToolIndex];
        if (t instanceof PencilTool) {
            PencilTool pen = (PencilTool) t;
            pen.updateAnim(Gdx.graphics.getDeltaTime());
            if (pen.isStrokeActive() && !pen.strokePoints.isEmpty()) {
                Color pc = palette.getSelected();
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(pc);
                for (int[] p : pen.strokePoints) {
                    shapeRenderer.rect(p[0], canvasHeight - 1 - p[1], 1, 1);
                }
                // Also draw mirror strokes
                if (mirrorX && mirrorToolX instanceof PencilTool)
                    for (int[] p : ((PencilTool) mirrorToolX).strokePoints)
                        shapeRenderer.rect(p[0], canvasHeight - 1 - p[1], 1, 1);
                if (mirrorY && mirrorToolY instanceof PencilTool)
                    for (int[] p : ((PencilTool) mirrorToolY).strokePoints)
                        shapeRenderer.rect(p[0], canvasHeight - 1 - p[1], 1, 1);
                if (mirrorX && mirrorY && mirrorToolXY instanceof PencilTool)
                    for (int[] p : ((PencilTool) mirrorToolXY).strokePoints)
                        shapeRenderer.rect(p[0], canvasHeight - 1 - p[1], 1, 1);
                shapeRenderer.end();
            }
            // Removal animation: show removed pixels in red then fade
            if (pen.isAnimating() && !pen.removedPoints.isEmpty()) {
                float alpha = pen.removeAnimTimer / 0.15f;
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(1f, 0f, 0f, alpha);
                for (int[] p : pen.removedPoints) {
                    shapeRenderer.rect(p[0], canvasHeight - 1 - p[1], 1, 1);
                }
                shapeRenderer.end();
            }
        }

        // Draw shape tool preview with ShapeRenderer (no Texture needed)
        if (t instanceof ShapeTool && ((ShapeTool) t).dragging) {
            ShapeTool st = (ShapeTool) t;
            Color previewColor = palette.getSelected();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(previewColor);
            if (st.currentShape == ShapeTool.Shape.LASSO_FILL && st.lassoPoints.size() > 1) {
                // Draw lasso path preview
                for (int i = 0; i < st.lassoPoints.size() - 1; i++) {
                    int[] a = st.lassoPoints.get(i), b = st.lassoPoints.get(i + 1);
                    shapeRenderer.rectLine(a[0] + 0.5f, canvasHeight - a[1] - 0.5f,
                                           b[0] + 0.5f, canvasHeight - b[1] - 0.5f, 0.5f);
                }
                // Draw closing line back to start (dashed appearance via different color)
                int[] first = st.lassoPoints.get(0);
                int[] last = st.lassoPoints.get(st.lassoPoints.size() - 1);
                shapeRenderer.setColor(1f, 1f, 1f, 0.4f);
                shapeRenderer.rectLine(last[0] + 0.5f, canvasHeight - last[1] - 0.5f,
                                       first[0] + 0.5f, canvasHeight - first[1] - 0.5f, 0.3f);
                // Draw start marker dot
                shapeRenderer.setColor(Color.WHITE);
                shapeRenderer.circle(first[0] + 0.5f, canvasHeight - first[1] - 0.5f, 1.5f);
            } else {
                st.drawPreviewSR(shapeRenderer, canvasWidth, canvasHeight);
            }
            shapeRenderer.end();
        }

        // Draw selection tool overlay (outline only - content commits on deselect)
        if (t instanceof SelectionTool && ((SelectionTool) t).hasSelection) {
            SelectionTool sel = (SelectionTool) t;
            float wx = sel.selX;
            float wy = canvasHeight - sel.selY - sel.selH;
            float sw2 = sel.selW;
            float sh2 = sel.selH;
            // Draw buffer texture preview if cutting
            Texture bufTex = sel.getBufferTexture();
            if (bufTex != null) {
                batch.begin();
                batch.draw(bufTex, wx, wy, sw2 / 2f, sh2 / 2f, sw2, sh2, 1, 1, -sel.rotationDeg, 0, 0, bufTex.getWidth(), bufTex.getHeight(), false, false);
                batch.end();
            }
            // Draw thick visible outline
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.CYAN);
            float lw = 0.3f;
            shapeRenderer.rectLine(wx, wy, wx + sw2, wy, lw);
            shapeRenderer.rectLine(wx + sw2, wy, wx + sw2, wy + sh2, lw);
            shapeRenderer.rectLine(wx + sw2, wy + sh2, wx, wy + sh2, lw);
            shapeRenderer.rectLine(wx, wy + sh2, wx, wy, lw);
            shapeRenderer.end();

            // Draw selection size label in screen space so it stays readable at any zoom
            Vector3 labelScreenPos = camera.project(new Vector3(wx, wy, 0));
            float dp = (float) Math.max(1, com.badlogic.gdx.Gdx.graphics.getDensity());
            float labelScale = Math.max(1.0f, 1.1f * dp);
            Matrix4 savedProj = batch.getProjectionMatrix().cpy();
            batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0,
                com.badlogic.gdx.Gdx.graphics.getWidth(), com.badlogic.gdx.Gdx.graphics.getHeight()));
            ui.getFont().getData().setScale(labelScale);
            float labelY = labelScreenPos.y - 4 * dp;
            batch.begin();
            ui.getFont().setColor(Color.BLACK);
            ui.getFont().draw(batch, sel.selW + "x" + sel.selH, labelScreenPos.x + 1, labelY - 1);
            ui.getFont().setColor(Color.WHITE);
            ui.getFont().draw(batch, sel.selW + "x" + sel.selH, labelScreenPos.x, labelY);
            batch.end();
            batch.setProjectionMatrix(savedProj);
            ui.getFont().getData().setScale(labelScale);
        }

        // Draw background tool overlay (bounding box + handles) if selected
        if (activeToolIndex == 5 && bgTraceTexture != null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.ORANGE);
            float lw = 0.3f;
            // Draw box outline
            shapeRenderer.rectLine(bgTraceX, bgTraceY, bgTraceX + bgTraceWidth, bgTraceY, lw);
            shapeRenderer.rectLine(bgTraceX + bgTraceWidth, bgTraceY, bgTraceX + bgTraceWidth, bgTraceY + bgTraceHeight, lw);
            shapeRenderer.rectLine(bgTraceX + bgTraceWidth, bgTraceY + bgTraceHeight, bgTraceX, bgTraceY + bgTraceHeight, lw);
            shapeRenderer.rectLine(bgTraceX, bgTraceY + bgTraceHeight, bgTraceX, bgTraceY, lw);

            // Draw corner handles
            float hs = Math.max(0.5f, 8f / zoom); // handle size in canvas coords
            float hsHalf = hs / 2f;
            shapeRenderer.rect(bgTraceX - hsHalf, bgTraceY - hsHalf, hs, hs);
            shapeRenderer.rect(bgTraceX + bgTraceWidth - hsHalf, bgTraceY - hsHalf, hs, hs);
            shapeRenderer.rect(bgTraceX + bgTraceWidth - hsHalf, bgTraceY + bgTraceHeight - hsHalf, hs, hs);
            shapeRenderer.rect(bgTraceX - hsHalf, bgTraceY + bgTraceHeight - hsHalf, hs, hs);
            shapeRenderer.end();
        }

        // Draw grid
        if (showGrid && zoom >= 4f) {
            drawGrid();
        }
        if (showGrid && tileSize > 0) {
            drawTileGrid();
        }

        // Draw mirror center lines
        if (mirrorX || mirrorY) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(1f, 0f, 0f, 0.4f);
            float lw = 4.0f / zoom;
            if (mirrorX) {
                shapeRenderer.rectLine(canvasWidth / 2f, 0, canvasWidth / 2f, canvasHeight, lw);
            }
            if (mirrorY) {
                shapeRenderer.rectLine(0, canvasHeight / 2f, canvasWidth, canvasHeight / 2f, lw);
            }
            shapeRenderer.end();
        }

        // Draw UI overlay
        if (ui != null) ui.draw();
    }

    private void handleInput() {
        int touches = 0;
        for (int i = 0; i < 5; i++) {
            if (Gdx.input.isTouched(i)) touches++;
        }

        // Check UI first on touch down
        if (touches == 1 && !touching && !isPanning && !uiConsuming) {
            if (ui != null && ui.touchDown(Gdx.input.getX(0), Gdx.input.getY(0))) {
                uiConsuming = true;
                return;
            }
        }
        if (uiConsuming) {
            if (touches >= 1) {
                if (ui != null) ui.touchDragged(Gdx.input.getX(0), Gdx.input.getY(0));
                return;
            } else {
                if (ui != null) ui.touchUp();
                uiConsuming = false;
                return;
            }
        }

        boolean movementMode = ui != null && ui.isMovementMode();

        if (movementMode) {
            handleInputMovementMode(touches);
        } else {
            handleInputDrawMode(touches);
        }
    }

    /**
     * Movement mode ON: 1-finger pan, longpress+drag = draw, zoom via buttons only.
     */
    private void handleInputMovementMode(int touches) {
        if (touches == 1) {
            float screenX = Gdx.input.getX(0);
            float screenY = Gdx.input.getY(0);

            if (!touching && !isPanning) {
                // New touch: start timer for longpress detection
                touchStartTime = System.nanoTime() / 1e9f;
                touchStartX = screenX;
                touchStartY = screenY;
                longPressDrawing = false;
                isPanning = true;
                lastPanX = screenX;
                lastPanY = screenY;
                touching = true;
            } else if (touching && !longPressDrawing) {
                // Check if longpress threshold reached (and finger hasn't moved much)
                float elapsed = (System.nanoTime() / 1e9f) - touchStartTime;
                float dx = Math.abs(screenX - touchStartX);
                float dy = Math.abs(screenY - touchStartY);

                if (elapsed >= LONG_PRESS_DRAW_TIME && dx < 15 && dy < 15) {
                    // Switch to draw mode
                    longPressDrawing = true;
                    isPanning = false;
                    // Start drawing at this point
                    Vector3 world = camera.unproject(new Vector3(screenX, screenY, 0));
                    int px = (int) world.x;
                    int py = canvasHeight - 1 - (int) world.y;
                    if (px >= 0 && px < canvasWidth && py >= 0 && py < canvasHeight) {
                        Layer activeLayer = layers.get(activeLayerIndex);
                        Color color = palette.getSelected();
                        Tool tool = tools[activeToolIndex];
                        if (colorPickerActive) {
                            pickColor(px, py);
                        } else {
                            undoManager.beginStroke(layers, activeLayerIndex);
                            tool.onDown(activeLayer.pixmap, px, py, color);
                            applyMirror(tool, activeLayer.pixmap, px, py, color, 0);
                            activeLayer.markDirty();
                        }
                    }
                } else if (isPanning) {
                    // Pan
                    float ddx = (screenX - lastPanX) / zoom;
                    float ddy = (screenY - lastPanY) / zoom;
                    panX += ddx;
                    panY -= ddy;
                    lastPanX = screenX;
                    lastPanY = screenY;
                }
            } else if (longPressDrawing) {
                // Continue drawing
                Vector3 world = camera.unproject(new Vector3(screenX, screenY, 0));
                int px = (int) world.x;
                int py = canvasHeight - 1 - (int) world.y;
                if (px >= 0 && px < canvasWidth && py >= 0 && py < canvasHeight && !colorPickerActive) {
                    Layer activeLayer = layers.get(activeLayerIndex);
                    Color color = palette.getSelected();
                    Tool tool = tools[activeToolIndex];
                    tool.onDrag(activeLayer.pixmap, px, py, color);
                    applyMirror(tool, activeLayer.pixmap, px, py, color, 1);
                    activeLayer.markDirty();
                }
            }
        } else if (touching) {
            // Touch ended
            if (longPressDrawing && !colorPickerActive) {
                float screenX = Gdx.input.getX(0);
                float screenY = Gdx.input.getY(0);
                Vector3 world = camera.unproject(new Vector3(screenX, screenY, 0));
                int px = MathUtils.clamp((int) world.x, 0, canvasWidth - 1);
                int py = MathUtils.clamp(canvasHeight - 1 - (int) world.y, 0, canvasHeight - 1);
                Layer activeLayer = layers.get(activeLayerIndex);
                Tool tool = tools[activeToolIndex];
                tool.onUp(activeLayer.pixmap, px, py, palette.getSelected());
                applyMirror(tool, activeLayer.pixmap, px, py, palette.getSelected(), 2);
                activeLayer.markDirty();
                undoManager.endStroke(layers, activeLayerIndex);
            }
            touching = false;
            isPanning = false;
            longPressDrawing = false;
        }
    }

    /**
     * Movement mode OFF: 2-finger pan, pinch zoom, single-finger tap/drag = draw.
     */
    private void handleInputDrawMode(int touches) {
        // Two fingers = pan + pinch zoom
        if (touches >= 2) {
            float x0 = Gdx.input.getX(0), y0 = Gdx.input.getY(0);
            float x1 = Gdx.input.getX(1), y1 = Gdx.input.getY(1);
            float midX = (x0 + x1) / 2f, midY = (y0 + y1) / 2f;
            float dist = (float) Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));

            if (isPanning) {
                // Pan
                float dx = (midX - lastPanX) / zoom;
                float dy = (midY - lastPanY) / zoom;
                panX += dx;
                panY -= dy;
                // Pinch zoom
                if (initialPinchDist > 0) {
                    float scale = dist / initialPinchDist;
                    float newZoom = initialPinchZoom * scale;
                    setZoom(newZoom);
                }
            } else {
                initialPinchDist = dist;
                initialPinchZoom = zoom;
            }
            isPanning = true;
            lastPanX = midX;
            lastPanY = midY;
            pointerCount = touches;

            // If was drawing, end the stroke
            if (touching) {
                float screenX = Gdx.input.getX(0);
                float screenY = Gdx.input.getY(0);
                Vector3 world = camera.unproject(new Vector3(screenX, screenY, 0));
                int px = MathUtils.clamp((int) world.x, 0, canvasWidth - 1);
                int py = MathUtils.clamp(canvasHeight - 1 - (int) world.y, 0, canvasHeight - 1);
                Layer activeLayer = layers.get(activeLayerIndex);
                Tool tool = tools[activeToolIndex];
                tool.onUp(activeLayer.pixmap, px, py, palette.getSelected());
                applyMirror(tool, activeLayer.pixmap, px, py, palette.getSelected(), 2);
                activeLayer.markDirty();
                undoManager.endStroke(layers, activeLayerIndex);
                touching = false;
            }
            return;
        }

        if (touches < 2 && isPanning) {
            isPanning = false;
            pointerCount = 0;
            initialPinchDist = 0;
            return;
        }
 
        // Single finger = draw immediately (no longpress needed)
        if (touches == 1 && !isPanning) {
            float screenX = Gdx.input.getX(0);
            float screenY = Gdx.input.getY(0);
            Vector3 world = camera.unproject(new Vector3(screenX, screenY, 0));
            int px = (int) world.x;
            int py = canvasHeight - 1 - (int) world.y;

            if (activeToolIndex == 5) {
                Tool tool = tools[activeToolIndex];
                if (!touching) {
                    tool.onDown(null, px, py, palette.getSelected());
                    touching = true;
                } else {
                    tool.onDrag(null, px, py, palette.getSelected());
                }
            } else if (px >= 0 && px < canvasWidth && py >= 0 && py < canvasHeight) {
                if (colorPickerActive) {
                    if (!touching) {
                        pickColor(px, py);
                        touching = true;
                    }
                } else {
                    Layer activeLayer = layers.get(activeLayerIndex);
                    Color color = palette.getSelected();
                    Tool tool = tools[activeToolIndex];

                    if (!touching) {
                        undoManager.beginStroke(layers, activeLayerIndex);
                        tool.onDown(activeLayer.pixmap, px, py, color);
                        applyMirror(tool, activeLayer.pixmap, px, py, color, 0);
                        activeLayer.markDirty();
                        touching = true;
                    } else {
                        tool.onDrag(activeLayer.pixmap, px, py, color);
                        applyMirror(tool, activeLayer.pixmap, px, py, color, 1);
                        activeLayer.markDirty();
                    }
                }
            }
        } else if (touching) {
            // Touch ended
            float screenX = Gdx.input.getX(0);
            float screenY = Gdx.input.getY(0);
            Vector3 world = camera.unproject(new Vector3(screenX, screenY, 0));
            int px = (int) world.x;
            int py = canvasHeight - 1 - (int) world.y;
            if (activeToolIndex == 5) {
                Tool tool = tools[activeToolIndex];
                tool.onUp(null, px, py, palette.getSelected());
            } else if (!colorPickerActive) {
                int cpx = MathUtils.clamp(px, 0, canvasWidth - 1);
                int cpy = MathUtils.clamp(py, 0, canvasHeight - 1);
                Layer activeLayer = layers.get(activeLayerIndex);
                Tool tool = tools[activeToolIndex];
                tool.onUp(activeLayer.pixmap, cpx, cpy, palette.getSelected());
                applyMirror(tool, activeLayer.pixmap, cpx, cpy, palette.getSelected(), 2);
                activeLayer.markDirty();
                undoManager.endStroke(layers, activeLayerIndex);
            }
            touching = false;
        }
    }

    /** Pick color from the active layer at pixel position */
    private void pickColor(int px, int py) {
        Layer activeLayer = layers.get(activeLayerIndex);
        int rgba = activeLayer.pixmap.getPixel(px, py);
        Color picked = new Color();
        Color.rgba8888ToColor(picked, rgba);
        palette.colors.set(palette.selectedIndex, picked);
        colorPickerActive = false;
        if (ui != null) ui.showToast("Color picked");
    }

    /** Apply mirror modifier to tool action. mode: 0=onDown, 1=onDrag, 2=onUp */
    private Tool mirrorToolX, mirrorToolY, mirrorToolXY;

    private Tool getMirrorTool(Tool src) {
        if (src instanceof PencilTool) {
            PencilTool p = new PencilTool();
            p.brushSize = ((PencilTool) src).brushSize;
            p.pixelPerfect = ((PencilTool) src).pixelPerfect;
            return p;
        }
        if (src instanceof EraserTool) {
            EraserTool e = new EraserTool();
            e.brushSize = ((EraserTool) src).brushSize;
            return e;
        }
        return src;
    }

    private void applyMirror(Tool tool, Pixmap target, int px, int py, Color color, int mode) {
        if (tool instanceof SelectionTool) return;
        if (mode == 0) {
            mirrorToolX = getMirrorTool(tool);
            mirrorToolY = getMirrorTool(tool);
            mirrorToolXY = getMirrorTool(tool);
        }
        int mx = canvasWidth - 1 - px;
        int my = canvasHeight - 1 - py;
        if (mirrorX && mirrorToolX != null) callTool(mirrorToolX, target, mx, py, color, mode);
        if (mirrorY && mirrorToolY != null) callTool(mirrorToolY, target, px, my, color, mode);
        if (mirrorX && mirrorY && mirrorToolXY != null) callTool(mirrorToolXY, target, mx, my, color, mode);
    }

    private void callTool(Tool tool, Pixmap target, int px, int py, Color color, int mode) {
        if (px < 0 || px >= canvasWidth || py < 0 || py >= canvasHeight) return;
        switch (mode) {
            case 0: tool.onDown(target, px, py, color); break;
            case 1: tool.onDrag(target, px, py, color); break;
            case 2: tool.onUp(target, px, py, color); break;
        }
    }

    private Texture checkerboardTex;
    private int lastCheckerLight, lastCheckerDark;

    private void drawCheckerboard() {
        int cl = Color.rgba8888(checkerLight);
        int cd = Color.rgba8888(checkerDark);
        if (checkerboardTex == null || cl != lastCheckerLight || cd != lastCheckerDark) {
            if (checkerboardTex != null) checkerboardTex.dispose();
            Pixmap pm = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
            pm.setBlending(Pixmap.Blending.None);
            pm.drawPixel(0, 0, cl); pm.drawPixel(1, 0, cd);
            pm.drawPixel(0, 1, cd);  pm.drawPixel(1, 1, cl);
            checkerboardTex = new Texture(pm);
            checkerboardTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            checkerboardTex.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            pm.dispose();
            lastCheckerLight = cl;
            lastCheckerDark = cd;
        }
        // Make each checker cell at least 8 screen pixels wide, but never smaller than 1 canvas pixel
        float minScreenPx = 8f;
        float cellCanvasSize = Math.max(1f, minScreenPx / zoom);
        // Each texture repeat = 2 cells, so UV repeats = canvasSize / (cellCanvasSize * 2)
        float uRepeat = canvasWidth / (cellCanvasSize * 2f);
        float vRepeat = canvasHeight / (cellCanvasSize * 2f);
        batch.begin();
        batch.draw(checkerboardTex, 0, 0, canvasWidth, canvasHeight,
                   0f, 0f, uRepeat, vRepeat);
        batch.end();
    }

    private void drawGrid() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(gridColor);
        float lw = 1.5f / zoom;
        for (int x = 0; x <= canvasWidth; x++) {
            shapeRenderer.rectLine(x, 0, x, canvasHeight, lw);
        }
        for (int y = 0; y <= canvasHeight; y++) {
            shapeRenderer.rectLine(0, y, canvasWidth, y, lw);
        }
        shapeRenderer.end();
    }

    private void drawTileGrid() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(tileGridColor);
        float lw = 2.5f / zoom;
        for (int x = 0; x <= canvasWidth; x += tileSize) {
            shapeRenderer.rectLine(x, 0, x, canvasHeight, lw);
        }
        for (int y = canvasHeight; y >= 0; y -= tileSize) {
            shapeRenderer.rectLine(0, y, canvasWidth, y, lw);
        }
        shapeRenderer.end();
    }

    // === Multi-document ===

    private void saveToDocument(Document doc) {
        doc.layers = layers;
        doc.activeLayerIndex = activeLayerIndex;
        doc.frames = frames;
        doc.currentFrameIndex = currentFrameIndex;
        doc.undoManager = undoManager;
        doc.canvasWidth = canvasWidth;
        doc.canvasHeight = canvasHeight;
        doc.frameRate = frameRate;
        doc.onionSkin = onionSkin;
    }

    private void loadFromDocument(Document doc) {
        layers = doc.layers;
        activeLayerIndex = doc.activeLayerIndex;
        frames = doc.frames;
        currentFrameIndex = doc.currentFrameIndex;
        undoManager = doc.undoManager;
        canvasWidth = doc.canvasWidth;
        canvasHeight = doc.canvasHeight;
        frameRate = doc.frameRate;
        onionSkin = doc.onionSkin;
        // Recalculate undo budget for this canvas size to avoid OOM on large images
        undoManager.setCanvasSize(canvasWidth, canvasHeight);
    }

    public void switchDocument(int index) {
        if (index < 0 || index >= documents.size() || index == activeDocIndex) return;
        saveToDocument(documents.get(activeDocIndex));
        activeDocIndex = index;
        loadFromDocument(documents.get(activeDocIndex));
        fitToWidth();
        if (ui != null) ui.docStrip.scrollToActive();
    }

    public void newDocument() { newDocument(32, 32); }

    public void newDocument(int w, int h) {
        saveToDocument(documents.get(activeDocIndex));
        Document doc = new Document("Untitled " + (documents.size() + 1), w, h);
        documents.add(doc);
        activeDocIndex = documents.size() - 1;
        loadFromDocument(doc);
        fitToWidth();
        if (ui != null) ui.docStrip.scrollToActive();
    }

    public void closeDocument() {
        documents.get(activeDocIndex).dispose();
        documents.remove(activeDocIndex);
        if (documents.isEmpty()) {
            // Create a fresh document instead of exiting
            Document doc = new Document("Untitled 1", 32, 32);
            documents.add(doc);
            activeDocIndex = 0;
            loadFromDocument(doc);
        } else {
            if (activeDocIndex >= documents.size()) activeDocIndex = documents.size() - 1;
            loadFromDocument(documents.get(activeDocIndex));
        }
        fitToWidth();
        if (ui != null) ui.docStrip.scrollToActive();
    }

    // === Public API for UI ===

    public boolean isFromNotTiled() { return platform.isFromNotTiled(); }

    public void newCanvas() {
        newDocument(32, 32);
    }

    public void newCanvas(int w, int h) {
        newDocument(w, h);
    }

    /** Crop all layers and frames to the given rectangle. */
    public void cropToSelection(int cx, int cy, int cw, int ch) {
        // Crop all layers
        for (Layer l : layers) {
            Pixmap cropped = new Pixmap(cw, ch, Pixmap.Format.RGBA8888);
            cropped.setBlending(Pixmap.Blending.None);
            cropped.drawPixmap(l.pixmap, 0, 0, cx, cy, cw, ch);
            l.pixmap.dispose();
            l.pixmap = cropped;
            l.markDirty();
        }
        // Crop animation frames
        for (AnimFrame f : frames) {
            Pixmap cropped = new Pixmap(cw, ch, Pixmap.Format.RGBA8888);
            cropped.setBlending(Pixmap.Blending.None);
            cropped.drawPixmap(f.pixmap, 0, 0, cx, cy, cw, ch);
            f.pixmap.dispose();
            f.pixmap = cropped;
            f.refreshTexture();
        }
        canvasWidth = cw;
        canvasHeight = ch;
        undoManager.clear();
        undoManager.setCanvasSize(cw, ch);
        if (documents.size() > activeDocIndex) {
            documents.get(activeDocIndex).canvasWidth = cw;
            documents.get(activeDocIndex).canvasHeight = ch;
        }
        fitToWidth();
    }

    /** Resize canvas to new dimensions. If stretch is true, scale content; otherwise just crop/expand. */
    public void resizeCanvas(int newW, int newH, boolean stretch) {
        for (Layer l : layers) {
            Pixmap resized = new Pixmap(newW, newH, Pixmap.Format.RGBA8888);
            resized.setBlending(Pixmap.Blending.None);
            if (stretch) {
                resized.drawPixmap(l.pixmap, 0, 0, canvasWidth, canvasHeight, 0, 0, newW, newH);
            } else {
                resized.drawPixmap(l.pixmap, 0, 0);
            }
            l.pixmap.dispose();
            l.pixmap = resized;
            l.markDirty();
        }
        for (AnimFrame f : frames) {
            Pixmap resized = new Pixmap(newW, newH, Pixmap.Format.RGBA8888);
            resized.setBlending(Pixmap.Blending.None);
            if (stretch) {
                resized.drawPixmap(f.pixmap, 0, 0, canvasWidth, canvasHeight, 0, 0, newW, newH);
            } else {
                resized.drawPixmap(f.pixmap, 0, 0);
            }
            f.pixmap.dispose();
            f.pixmap = resized;
            f.refreshTexture();
        }
        canvasWidth = newW;
        canvasHeight = newH;
        undoManager.clear();
        undoManager.setCanvasSize(newW, newH);
        if (documents.size() > activeDocIndex) {
            documents.get(activeDocIndex).canvasWidth = newW;
            documents.get(activeDocIndex).canvasHeight = newH;
        }
        fitToWidth();
    }

    public void openFile() { platform.openFile(); }

    public void saveFile() {
        if (platform.isFromNotTiled()) {
            save(); // write back to intent URI
        } else {
            platform.saveFileAs(); // SAF picker
        }
    }

    public void saveFileAs() {
        platform.saveFileAs();
    }

    public void closeApp() { platform.closeApp(); }

    public void exportPng() { platform.saveFileAs(); }
    public void exportGif() { platform.exportGif(); }
    public void saveAse() { platform.saveAse(); }
    public void loadAse() { platform.openAse(); }

    public void loadAseFromPath(String path) {
        try {
            AseReader.AseData data = AseReader.read(path);
            canvasWidth = data.width;
            canvasHeight = data.height;
            // Rebuild layers
            for (Layer l : layers) l.dispose();
            layers.clear();
            for (AseReader.LayerInfo li : data.layers) {
                Layer l = new Layer(li.name, canvasWidth, canvasHeight);
                l.visible = li.visible;
                l.opacity = li.opacity;
                layers.add(l);
            }
            if (layers.isEmpty()) layers.add(new Layer("Background", canvasWidth, canvasHeight));
            activeLayerIndex = 0;
            // Rebuild frames
            for (AnimFrame f : frames) f.dispose();
            frames.clear();
            for (AseReader.FrameData fd : data.frames) {
                AnimFrame af = new AnimFrame(canvasWidth, canvasHeight);
                for (AseReader.CelData cel : fd.cels) {
                    if (cel.pixels != null && cel.layerIndex == 0) {
                        af.pixmap.setBlending(Pixmap.Blending.None);
                        af.pixmap.drawPixmap(cel.pixels, 0, 0);
                        af.pixmap.setBlending(Pixmap.Blending.SourceOver);
                    }
                    if (cel.pixels != null && cel.layerIndex < layers.size()) {
                        Layer l = layers.get(cel.layerIndex);
                        l.pixmap.setBlending(Pixmap.Blending.None);
                        l.pixmap.drawPixmap(cel.pixels, 0, 0);
                        l.pixmap.setBlending(Pixmap.Blending.SourceOver);
                        l.markDirty();
                    }
                }
                af.refreshTexture();
                frames.add(af);
                for (AseReader.CelData cel : fd.cels) {
                    if (cel.pixels != null) cel.pixels.dispose();
                }
            }
            if (frames.isEmpty()) frames.add(new AnimFrame(canvasWidth, canvasHeight));
            currentFrameIndex = 0;
            loadFrame(0);
            for (Layer l : layers) l.markDirty();
            undoManager.clear();
            undoManager.setCanvasSize(canvasWidth, canvasHeight);
            Document activeDoc = documents.get(activeDocIndex);
            activeDoc.canvasWidth = canvasWidth;
            activeDoc.canvasHeight = canvasHeight;
            activeDoc.layers = layers;
            activeDoc.frames = frames;
            activeDoc.undoManager = undoManager;
            fitToWidth();
        } catch (Exception e) {
            Gdx.app.error("Not2Pix", "Failed to load ASE: " + path, e);
            if (layers.isEmpty()) layers.add(new Layer("Background", canvasWidth, canvasHeight));
            if (frames.isEmpty()) frames.add(new AnimFrame(canvasWidth, canvasHeight));
            if (ui != null) ui.showToast("Failed to load .ase file");
        }
    }

    /** Called by platform after SAF open completes */
    public void loadFromPath(String path) {
        saveToDocument(documents.get(activeDocIndex));
        String filename = path.substring(path.lastIndexOf('/') + 1);
        if (filename.contains(".")) filename = filename.substring(0, filename.lastIndexOf('.'));
        Document doc = new Document(filename, 32, 32);
        doc.layers.get(0).dispose();
        doc.layers.clear();
        doc.frames.get(0).dispose();
        doc.frames.clear();
        try {
            Pixmap loaded = new Pixmap(Gdx.files.absolute(path));
            int w = loaded.getWidth();
            int h = loaded.getHeight();
            if (w > MAX_TEXTURE_SIZE || h > MAX_TEXTURE_SIZE) {
                Gdx.app.error("Not2Pix", "Image too large: " + w + "x" + h);
                loaded.dispose();
                doc.canvasWidth = 32; doc.canvasHeight = 32;
                doc.layers.add(new Layer("Background", 32, 32));
                doc.frames.add(new AnimFrame(32, 32));
                if (ui != null) ui.showToast("Image too large (max " + MAX_TEXTURE_SIZE + "px)");
            } else {
                doc.canvasWidth = w;
                doc.canvasHeight = h;
                // Ensure RGBA8888 format
                if (loaded.getFormat() != Pixmap.Format.RGBA8888) {
                    Pixmap converted = new Pixmap(w, h, Pixmap.Format.RGBA8888);
                    converted.setBlending(Pixmap.Blending.None);
                    converted.drawPixmap(loaded, 0, 0);
                    loaded.dispose();
                    loaded = converted;
                }
                Layer l = new Layer("Background", doc.canvasWidth, doc.canvasHeight);
                l.pixmap.dispose();
                l.pixmap = loaded;
                l.markDirty();
                l.refreshTexture();
                doc.layers.add(l);
                // Build frame directly from loaded pixmap — avoids a second blank allocation
                AnimFrame af = new AnimFrame(doc.canvasWidth, doc.canvasHeight);
                af.pixmap.setBlending(Pixmap.Blending.None);
                af.pixmap.drawPixmap(loaded, 0, 0);
                af.pixmap.setBlending(Pixmap.Blending.SourceOver);
                af.refreshTexture();
                doc.frames.add(af);
            }
        } catch (Exception e) {
            doc.canvasWidth = 32;
            doc.canvasHeight = 32;
            doc.layers.add(new Layer("Background", 32, 32));
            doc.frames.add(new AnimFrame(32, 32));
        }
        documents.add(doc);
        activeDocIndex = documents.size() - 1;
        loadFromDocument(doc);
        fitToWidth();
    }

    /** Called by platform after SAF save location chosen */
    public void saveToPath(String path) {
        Pixmap flat = flattenLayers();
        try {
            PixmapIO.writePNG(Gdx.files.absolute(path), flat);
        } finally {
            flat.dispose();
        }
    }

    public void undo() { undoManager.undo(layers); for (Layer l : layers) l.markDirty(); }
    public void redo() { undoManager.redo(layers); for (Layer l : layers) l.markDirty(); }

    public void setTool(int index) {
        int newIdx = MathUtils.clamp(index, 0, tools.length - 1);
        if (newIdx != activeToolIndex && tools[activeToolIndex] instanceof SelectionTool) {
            SelectionTool sel = (SelectionTool) tools[activeToolIndex];
            if (sel.hasSelection && !layers.isEmpty()) {
                sel.commitSelection(layers.get(activeLayerIndex).pixmap);
                layers.get(activeLayerIndex).markDirty();
            }
        }
        activeToolIndex = newIdx;
    }

    public void addLayer() {
        undoManager.saveState(layers);
        layers.add(new Layer("Layer " + (layers.size() + 1), canvasWidth, canvasHeight));
        activeLayerIndex = layers.size() - 1;
    }

    public void removeLayer() {
        if (layers.size() <= 1) return;
        undoManager.saveState(layers);
        layers.remove(activeLayerIndex).dispose();
        if (activeLayerIndex >= layers.size()) activeLayerIndex = layers.size() - 1;
    }

    public void moveLayerUp() {
        if (activeLayerIndex >= layers.size() - 1) return;
        Layer l = layers.remove(activeLayerIndex);
        layers.add(activeLayerIndex + 1, l);
        activeLayerIndex++;
    }

    public void moveLayerDown() {
        if (activeLayerIndex <= 0) return;
        Layer l = layers.remove(activeLayerIndex);
        layers.add(activeLayerIndex - 1, l);
        activeLayerIndex--;
    }

    // === Animation ===

    public void addFrame() {
        saveFrameState();
        frames.add(new AnimFrame(canvasWidth, canvasHeight));
        currentFrameIndex = frames.size() - 1;
        loadFrame(currentFrameIndex);
    }

    public void duplicateFrame() {
        saveFrameState();
        AnimFrame src = frames.get(currentFrameIndex);
        AnimFrame dup = new AnimFrame(canvasWidth, canvasHeight);
        dup.pixmap.setBlending(Pixmap.Blending.None);
        dup.pixmap.drawPixmap(src.pixmap, 0, 0);
        dup.pixmap.setBlending(Pixmap.Blending.SourceOver);
        dup.refreshTexture();
        frames.add(currentFrameIndex + 1, dup);
        currentFrameIndex++;
        loadFrame(currentFrameIndex);
    }

    public void deleteFrame() {
        if (frames.size() <= 1) return;
        frames.remove(currentFrameIndex).dispose();
        if (currentFrameIndex >= frames.size()) currentFrameIndex = frames.size() - 1;
        loadFrame(currentFrameIndex);
    }

    public void nextFrame() {
        saveFrameState();
        currentFrameIndex = (currentFrameIndex + 1) % frames.size();
        loadFrame(currentFrameIndex);
    }

    public void prevFrame() {
        saveFrameState();
        currentFrameIndex = (currentFrameIndex - 1 + frames.size()) % frames.size();
        loadFrame(currentFrameIndex);
    }

    /** Save current layer 0 pixels into current frame */
    public void saveFrameState() {
        if (frames.isEmpty() || layers.isEmpty()) return;
        AnimFrame frame = frames.get(currentFrameIndex);
        frame.pixmap.setBlending(Pixmap.Blending.None);
        frame.pixmap.setColor(Color.CLEAR);
        frame.pixmap.fill();
        frame.pixmap.drawPixmap(layers.get(0).pixmap, 0, 0);
        frame.pixmap.setBlending(Pixmap.Blending.SourceOver);
        frame.refreshTexture();
    }

    /** Load frame into layer 0 */
    private void loadFrame(int index) {
        if (layers.isEmpty()) return;
        AnimFrame frame = frames.get(index);
        Pixmap target = layers.get(0).pixmap;
        target.setBlending(Pixmap.Blending.None);
        target.setColor(Color.CLEAR);
        target.fill();
        target.drawPixmap(frame.pixmap, 0, 0);
        target.setBlending(Pixmap.Blending.SourceOver);
        layers.get(0).markDirty();
    }

    public void loadFramePublic(int index) { loadFrame(index); }

    public void togglePlayback() {
        if (playing) {
            playing = false;
        } else {
            saveFrameState();
            playing = true;
            animTimer = 0;
        }
    }

    // === Save ===

    public void save() {
        if (intentFilePath != null) {
            Pixmap flat = flattenLayers();
            try {
                PixmapIO.writePNG(Gdx.files.absolute(intentFilePath), flat);
            } finally {
                flat.dispose();
            }
            platform.finishWithResult(true);
        }
    }

    private Pixmap flattenLayers() {
        Pixmap flat = new Pixmap(canvasWidth, canvasHeight, Pixmap.Format.RGBA8888);
        flat.setBlending(Pixmap.Blending.SourceOver);
        for (Layer l : layers) {
            if (l.visible) flat.drawPixmap(l.pixmap, 0, 0);
        }
        return flat;
    }

    public void setZoom(float z) {
        // Find nearest zoom level
        int best = 0;
        float bestDist = Math.abs(z - ZOOM_LEVELS[0]);
        for (int i = 1; i < ZOOM_LEVELS.length; i++) {
            float d = Math.abs(z - ZOOM_LEVELS[i]);
            if (d < bestDist) { bestDist = d; best = i; }
        }
        zoomIndex = best;
        zoom = ZOOM_LEVELS[zoomIndex];
    }
    public void zoomIn()  { if (zoomIndex < ZOOM_LEVELS.length - 1) { zoomIndex++; zoom = ZOOM_LEVELS[zoomIndex]; } }
    public void zoomOut() { if (zoomIndex > 0) { zoomIndex--; zoom = ZOOM_LEVELS[zoomIndex]; } }
    public float getZoom() { return zoom; }
    public float getPanX() { return panX; }
    public float getPanY() { return panY; }
    public void setPan(float x, float y) { panX = x; panY = y; }

    public void fitToWidth() {
        float ideal = (float) Gdx.graphics.getWidth() / canvasWidth;
        setZoom(ideal);
        panX = 0;
        panY = 0;
    }

    public void loadPrefs() {
        Preferences p = Gdx.app.getPreferences("Not2Pix");
        gridColor.set(p.getFloat("gridR", 0), p.getFloat("gridG", 0), p.getFloat("gridB", 0), p.getFloat("gridA", 0.5f));
        tileGridColor.set(p.getFloat("tgridR", 0), p.getFloat("tgridG", 0), p.getFloat("tgridB", 0), p.getFloat("tgridA", 0.7f));
        bgColor.set(p.getFloat("bgR", 0.12f), p.getFloat("bgG", 0.12f), p.getFloat("bgB", 0.12f), p.getFloat("bgA", 1f));
        checkerLight.set(p.getFloat("chkLR", 0.9f), p.getFloat("chkLG", 0.9f), p.getFloat("chkLB", 0.9f), 1f);
        checkerDark.set(p.getFloat("chkDR", 0.7f), p.getFloat("chkDG", 0.7f), p.getFloat("chkDB", 0.7f), 1f);
        tileSize = p.getInteger("tileSize", 0);
        showGrid = p.getBoolean("showGrid", true);
        mirrorX = p.getBoolean("mirrorX", false);
        mirrorY = p.getBoolean("mirrorY", false);
        onionSkin = p.getBoolean("onionSkin", false);
        showMinimap = p.getBoolean("showMinimap", true);
        minimapSize = p.getInteger("minimapSize", 96);
    }

    public void savePrefs() {
        Preferences p = Gdx.app.getPreferences("Not2Pix");
        p.putFloat("gridR", gridColor.r); p.putFloat("gridG", gridColor.g); p.putFloat("gridB", gridColor.b); p.putFloat("gridA", gridColor.a);
        p.putFloat("tgridR", tileGridColor.r); p.putFloat("tgridG", tileGridColor.g); p.putFloat("tgridB", tileGridColor.b); p.putFloat("tgridA", tileGridColor.a);
        p.putFloat("bgR", bgColor.r); p.putFloat("bgG", bgColor.g); p.putFloat("bgB", bgColor.b); p.putFloat("bgA", bgColor.a);
        p.putFloat("chkLR", checkerLight.r); p.putFloat("chkLG", checkerLight.g); p.putFloat("chkLB", checkerLight.b);
        p.putFloat("chkDR", checkerDark.r); p.putFloat("chkDG", checkerDark.g); p.putFloat("chkDB", checkerDark.b);
        p.putInteger("tileSize", tileSize);
        p.putBoolean("showGrid", showGrid);
        p.putBoolean("mirrorX", mirrorX);
        p.putBoolean("mirrorY", mirrorY);
        p.putBoolean("onionSkin", onionSkin);
        p.putBoolean("showMinimap", showMinimap);
        p.putInteger("minimapSize", minimapSize);
        p.flush();
    }

    @Override
    public void pause() {
        // Save layers to local temp files
        for (int i = 0; i < layers.size(); i++) {
            PixmapIO.writePNG(Gdx.files.local("temp/layer_" + i + ".png"), layers.get(i).pixmap);
        }
        // Save state
        Preferences p = Gdx.app.getPreferences("Not2Pix");
        p.putFloat("viewZoom", zoom);
        p.putFloat("viewPanX", panX);
        p.putFloat("viewPanY", panY);
        p.putInteger("tempLayerCount", layers.size());
        p.putInteger("tempCanvasW", canvasWidth);
        p.putInteger("tempCanvasH", canvasHeight);
        p.putBoolean("tempSaved", true);
        p.flush();
    }

    @Override
    public void resume() {
        Preferences p = Gdx.app.getPreferences("Not2Pix");
        if (!p.getBoolean("tempSaved", false)) return;
        int count = p.getInteger("tempLayerCount", 0);
        int tw = p.getInteger("tempCanvasW", canvasWidth);
        int th = p.getInteger("tempCanvasH", canvasHeight);
        if (count > 0 && tw > 0 && th > 0) {
            canvasWidth = tw;
            canvasHeight = th;
            for (int i = 0; i < layers.size(); i++) layers.get(i).dispose();
            layers.clear();
            for (int i = 0; i < count; i++) {
                com.badlogic.gdx.files.FileHandle f = Gdx.files.local("temp/layer_" + i + ".png");
                if (f.exists()) {
                    Pixmap pm = new Pixmap(f);
                    Layer l = new Layer("Layer " + (i + 1), tw, th);
                    l.pixmap.dispose();
                    l.pixmap = pm;
                    l.markDirty();
                    layers.add(l);
                }
            }
            if (layers.isEmpty()) layers.add(new Layer("Background", tw, th));
            if (activeLayerIndex >= layers.size()) activeLayerIndex = 0;
        }
        // Restore zoom/pan
        zoom = p.getFloat("viewZoom", zoom);
        panX = p.getFloat("viewPanX", panX);
        panY = p.getFloat("viewPanY", panY);
        p.putBoolean("tempSaved", false);
        p.flush();
        // Clean up temp files
        for (int i = 0; i < count; i++) {
            com.badlogic.gdx.files.FileHandle f = Gdx.files.local("temp/layer_" + i + ".png");
            if (f.exists()) f.delete();
        }
    }

    public void loadBackgroundImage(String path) {
        try {
            Pixmap pm = new Pixmap(Gdx.files.absolute(path));
            if (bgTraceTexture != null) bgTraceTexture.dispose();
            bgTraceTexture = new Texture(pm);
            bgTraceTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            bgTraceWidth = pm.getWidth();
            bgTraceHeight = pm.getHeight();
            // Center it on the canvas
            bgTraceX = (canvasWidth - bgTraceWidth) / 2f;
            bgTraceY = (canvasHeight - bgTraceHeight) / 2f;
            pm.dispose();
            if (ui != null) ui.showToast("Background loaded");
        } catch (Exception e) {
            Gdx.app.error("Not2Pix", "Failed to load background image: " + path, e);
            if (ui != null) ui.showToast("Failed to load background image");
        }
    }

    public void removeBackgroundImage() {
        if (bgTraceTexture != null) {
            bgTraceTexture.dispose();
            bgTraceTexture = null;
            if (ui != null) ui.showToast("Background removed");
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        if (checkerboardTex != null) checkerboardTex.dispose();
        if (bgTraceTexture != null) bgTraceTexture.dispose();
        for (Document doc : documents) {
            if (doc.layers != layers) doc.dispose();
        }
        for (Layer l : layers) l.dispose();
        for (AnimFrame f : frames) f.dispose();
        if (ui != null) ui.dispose();
    }
}
