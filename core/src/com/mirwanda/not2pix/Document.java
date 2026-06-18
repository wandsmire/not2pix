package com.mirwanda.not2pix;

import java.util.ArrayList;

public class Document {
    public String name;
    public int canvasWidth, canvasHeight;
    public ArrayList<Layer> layers = new ArrayList<>();
    public int activeLayerIndex = 0;
    public ArrayList<AnimFrame> frames = new ArrayList<>();
    public int currentFrameIndex = 0;
    public UndoManager undoManager = new UndoManager();
    public float frameRate = 8f;
    public boolean onionSkin = false;

    public Document(String name, int w, int h) {
        this.name = name;
        this.canvasWidth = w;
        this.canvasHeight = h;
        layers.add(new Layer("Background", w, h));
        frames.add(new AnimFrame(w, h));
    }

    public void dispose() {
        for (Layer l : layers) l.dispose();
        for (AnimFrame f : frames) f.dispose();
    }
}
