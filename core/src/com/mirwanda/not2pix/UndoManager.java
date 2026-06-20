package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Pixmap;
import java.util.ArrayList;

/**
 * Region-based undo/redo manager.
 *
 * Drawing operations use beginStroke/endStroke which only snapshots the
 * active layer, then diffs before vs after to store a minimal bounding-rect
 * patch.  Structural edits (add/remove layer) use saveState for a full
 * snapshot of all layers.
 */
public class UndoManager {

    private static class Patch {
        // --- region patch (drawing edits) ---
        int layerIndex;
        int rx, ry, rw, rh;     // bounding rect
        byte[] beforePixels;     // RGBA of that rect before the edit
        byte[] afterPixels;      // RGBA of that rect after the edit

        // --- full snapshot (structural edits) ---
        boolean structural;
        byte[][] fullBefore;     // one raw byte[] per layer
    }

    private final ArrayList<Patch> undoStack = new ArrayList<>();
    private final ArrayList<Patch> redoStack = new ArrayList<>();
    private int maxStates = 30;

    // Pre-stroke state (active layer only)
    private byte[] strokeBefore;
    private int strokeLayerIdx;
    private int strokeW, strokeH;

    public void setCanvasSize(int w, int h) {
        long worst = (long) w * h * 4 * 2;
        long budget = 80L * 1024 * 1024;
        int computed = (int) (budget / Math.max(worst, 1));
        maxStates = Math.max(3, Math.min(computed, 50));
        trimStack(undoStack);
        trimStack(redoStack);
    }

    // ===== Drawing operations: beginStroke / endStroke =======================

    /**
     * Call on touch-down.  Snapshots only the active layer (one copy).
     */
    public void beginStroke(ArrayList<Layer> layers, int activeLayerIndex) {
        if (activeLayerIndex < 0 || activeLayerIndex >= layers.size()) {
            strokeBefore = null;
            return;
        }
        strokeLayerIdx = activeLayerIndex;
        Pixmap pm = layers.get(activeLayerIndex).pixmap;
        strokeW = pm.getWidth();
        strokeH = pm.getHeight();
        strokeBefore = grabRaw(pm);
    }

    /**
     * Call on touch-up.  Diffs before vs after for the active layer and pushes
     * a minimal patch.  If nothing changed, no entry is created.
     */
    public void endStroke(ArrayList<Layer> layers, int activeLayerIndex) {
        byte[] before = strokeBefore;
        strokeBefore = null; // allow GC if we return early

        if (before == null) return;
        if (activeLayerIndex != strokeLayerIdx) return;
        if (activeLayerIndex < 0 || activeLayerIndex >= layers.size()) return;

        Pixmap pm = layers.get(activeLayerIndex).pixmap;
        int w = pm.getWidth(), h = pm.getHeight();
        if (w != strokeW || h != strokeH) return; // dimension mismatch guard

        byte[] after = grabRaw(pm);

        // Scan for bounding rect of changes
        int minX = w, maxX = -1, minY = h, maxY = -1;
        for (int y = 0; y < h; y++) {
            int rowOff = y * w * 4;
            for (int x = 0; x < w; x++) {
                int off = rowOff + x * 4;
                if (before[off]     != after[off]     ||
                    before[off + 1] != after[off + 1] ||
                    before[off + 2] != after[off + 2] ||
                    before[off + 3] != after[off + 3]) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < 0) return; // no pixels changed

        int rw = maxX - minX + 1;
        int rh = maxY - minY + 1;
        int regionBytes = rw * rh * 4;

        byte[] bPatch = new byte[regionBytes];
        byte[] aPatch = new byte[regionBytes];
        for (int ry = 0; ry < rh; ry++) {
            int srcOff = ((minY + ry) * w + minX) * 4;
            int dstOff = ry * rw * 4;
            System.arraycopy(before, srcOff, bPatch, dstOff, rw * 4);
            System.arraycopy(after,  srcOff, aPatch, dstOff, rw * 4);
        }

        Patch p = new Patch();
        p.layerIndex = activeLayerIndex;
        p.rx = minX; p.ry = minY; p.rw = rw; p.rh = rh;
        p.beforePixels = bPatch;
        p.afterPixels  = aPatch;
        pushUndo(p);
        redoStack.clear();
    }

    // ===== Structural edits: saveState (add/remove layer) ====================

    public void saveState(ArrayList<Layer> layers) {
        Patch p = new Patch();
        p.structural = true;
        p.fullBefore = new byte[layers.size()][];
        for (int i = 0; i < layers.size(); i++) {
            p.fullBefore[i] = grabRaw(layers.get(i).pixmap);
        }
        pushUndo(p);
        redoStack.clear();
    }

    // ===== Undo / Redo =======================================================

    public boolean undo(ArrayList<Layer> layers) {
        if (undoStack.isEmpty()) return false;
        Patch p = undoStack.remove(undoStack.size() - 1);

        if (p.structural) {
            // Capture current for redo
            Patch redo = new Patch();
            redo.structural = true;
            redo.fullBefore = snapshotAll(layers);
            redoStack.add(redo);
            // Restore
            restoreAll(layers, p.fullBefore);
        } else {
            // Capture current region for redo
            Patch redo = new Patch();
            redo.layerIndex = p.layerIndex;
            redo.rx = p.rx; redo.ry = p.ry; redo.rw = p.rw; redo.rh = p.rh;
            redo.beforePixels = p.afterPixels;  // redo's "before" = current "after"
            redo.afterPixels  = p.beforePixels; // redo's "after"  = current "before"
            redoStack.add(redo);
            // Apply the patch's beforePixels
            if (p.layerIndex < layers.size()) {
                putRegion(layers.get(p.layerIndex).pixmap,
                          p.rx, p.ry, p.rw, p.rh, p.beforePixels);
                layers.get(p.layerIndex).markDirty();
            }
        }
        return true;
    }

    public boolean redo(ArrayList<Layer> layers) {
        if (redoStack.isEmpty()) return false;
        Patch p = redoStack.remove(redoStack.size() - 1);

        if (p.structural) {
            Patch undo = new Patch();
            undo.structural = true;
            undo.fullBefore = snapshotAll(layers);
            undoStack.add(undo);
            restoreAll(layers, p.fullBefore);
        } else {
            Patch undo = new Patch();
            undo.layerIndex = p.layerIndex;
            undo.rx = p.rx; undo.ry = p.ry; undo.rw = p.rw; undo.rh = p.rh;
            undo.beforePixels = p.afterPixels;
            undo.afterPixels  = p.beforePixels;
            undoStack.add(undo);
            if (p.layerIndex < layers.size()) {
                putRegion(layers.get(p.layerIndex).pixmap,
                          p.rx, p.ry, p.rw, p.rh, p.beforePixels);
                layers.get(p.layerIndex).markDirty();
            }
        }
        return true;
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
    public void clear() { undoStack.clear(); redoStack.clear(); strokeBefore = null; }

    // ===== Helpers ============================================================

    private byte[] grabRaw(Pixmap pm) {
        int bytes = pm.getWidth() * pm.getHeight() * 4;
        byte[] raw = new byte[bytes];
        pm.getPixels().rewind();
        pm.getPixels().get(raw, 0, bytes);
        pm.getPixels().rewind();
        return raw;
    }

    private byte[][] snapshotAll(ArrayList<Layer> layers) {
        byte[][] snap = new byte[layers.size()][];
        for (int i = 0; i < layers.size(); i++) {
            snap[i] = grabRaw(layers.get(i).pixmap);
        }
        return snap;
    }

    private void restoreAll(ArrayList<Layer> layers, byte[][] state) {
        for (int i = 0; i < state.length && i < layers.size(); i++) {
            Pixmap pm = layers.get(i).pixmap;
            if (state[i].length != pm.getWidth() * pm.getHeight() * 4) continue; // size mismatch guard
            pm.getPixels().rewind();
            pm.getPixels().put(state[i], 0, state[i].length);
            pm.getPixels().rewind();
            layers.get(i).markDirty();
        }
    }

    private void putRegion(Pixmap pm, int rx, int ry, int rw, int rh, byte[] data) {
        int pmW = pm.getWidth();
        int pmH = pm.getHeight();
        if (rx + rw > pmW || ry + rh > pmH) return; // dimension mismatch guard
        java.nio.ByteBuffer buf = pm.getPixels();
        for (int y = 0; y < rh; y++) {
            buf.position(((ry + y) * pmW + rx) * 4);
            buf.put(data, y * rw * 4, rw * 4);
        }
        buf.rewind();
    }

    private void pushUndo(Patch p) {
        undoStack.add(p);
        if (undoStack.size() > maxStates) undoStack.remove(0);
    }

    private void trimStack(ArrayList<Patch> stack) {
        while (stack.size() > maxStates) stack.remove(0);
    }
}
