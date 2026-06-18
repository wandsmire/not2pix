package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Pixmap;
import java.io.*;
import java.util.ArrayList;
import java.util.zip.InflaterInputStream;

public class AseReader {

    public static class LayerInfo {
        public String name;
        public boolean visible;
        public float opacity;
    }

    public static class CelData {
        public int layerIndex;
        public Pixmap pixels;
    }

    public static class FrameData {
        public int duration;
        public ArrayList<CelData> cels = new ArrayList<>();
    }

    public static class AseData {
        public int width, height;
        public ArrayList<LayerInfo> layers = new ArrayList<>();
        public ArrayList<FrameData> frames = new ArrayList<>();
    }

    public static AseData read(String path) {
        try {
            byte[] file = readFile(path);
            AseData data = new AseData();
            int pos = 0;

            // Header
            /* int fileSize = */ readDword(file, pos); pos += 4;
            int magic = readWord(file, pos); pos += 2;
            if (magic != 0xA5E0) throw new IOException("Not an ASE file");
            int numFrames = readWord(file, pos); pos += 2;
            data.width = readWord(file, pos); pos += 2;
            data.height = readWord(file, pos); pos += 2;
            pos = 128; // skip rest of header

            for (int f = 0; f < numFrames; f++) {
                FrameData frame = new FrameData();
                int frameStart = pos;
                int frameSize = readDword(file, pos); pos += 4;
                /* int frameMagic = */ readWord(file, pos); pos += 2;
                int oldChunks = readWord(file, pos); pos += 2;
                frame.duration = readWord(file, pos); pos += 2;
                pos += 2; // reserved
                int newChunks = readDword(file, pos); pos += 4;
                int chunks = newChunks > 0 ? newChunks : oldChunks;

                for (int c = 0; c < chunks; c++) {
                    int chunkStart = pos;
                    int chunkSize = readDword(file, pos); pos += 4;
                    int chunkType = readWord(file, pos); pos += 2;

                    if (chunkType == 0x2004) { // Layer
                        LayerInfo layer = new LayerInfo();
                        int flags = readWord(file, pos); pos += 2;
                        layer.visible = (flags & 1) != 0;
                        pos += 2; // type
                        pos += 2; // child level
                        pos += 2; // default w
                        pos += 2; // default h
                        pos += 2; // blend mode
                        layer.opacity = (file[pos] & 0xFF) / 255f; pos += 1;
                        pos += 3; // reserved
                        int nameLen = readWord(file, pos); pos += 2;
                        layer.name = new String(file, pos, nameLen, "UTF-8"); pos += nameLen;
                        data.layers.add(layer);
                    } else if (chunkType == 0x2005) { // Cel
                        CelData cel = new CelData();
                        cel.layerIndex = readWord(file, pos); pos += 2;
                        pos += 2; // x
                        pos += 2; // y
                        pos += 1; // opacity
                        int celType = readWord(file, pos); pos += 2;
                        pos += 2; // zIndex
                        pos += 5; // reserved
                        int celW = readWord(file, pos); pos += 2;
                        int celH = readWord(file, pos); pos += 2;

                        if (celType == 2) { // compressed
                            int dataLen = chunkSize - 6 - (pos - chunkStart - 6);
                            dataLen = (chunkStart + chunkSize) - pos;
                            byte[] compressed = new byte[dataLen];
                            System.arraycopy(file, pos, compressed, 0, dataLen);
                            byte[] rgba = zlibDecompress(compressed, celW * celH * 4);
                            cel.pixels = new Pixmap(celW, celH, Pixmap.Format.RGBA8888);
                            for (int y = 0; y < celH; y++) {
                                for (int x = 0; x < celW; x++) {
                                    int idx = (y * celW + x) * 4;
                                    int r = rgba[idx] & 0xFF;
                                    int g = rgba[idx+1] & 0xFF;
                                    int b = rgba[idx+2] & 0xFF;
                                    int a = rgba[idx+3] & 0xFF;
                                    cel.pixels.drawPixel(x, y, (r << 24) | (g << 16) | (b << 8) | a);
                                }
                            }
                        }
                        frame.cels.add(cel);
                    }
                    pos = chunkStart + chunkSize;
                }
                pos = frameStart + frameSize;
                data.frames.add(frame);
            }
            return data;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read ASE: " + e.getMessage(), e);
        }
    }

    private static byte[] readFile(String path) throws IOException {
        FileInputStream fis = new FileInputStream(path);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = fis.read(buf)) > 0) bos.write(buf, 0, len);
        fis.close();
        return bos.toByteArray();
    }

    private static int readDword(byte[] data, int pos) {
        return (data[pos] & 0xFF) | ((data[pos+1] & 0xFF) << 8) |
               ((data[pos+2] & 0xFF) << 16) | ((data[pos+3] & 0xFF) << 24);
    }

    private static int readWord(byte[] data, int pos) {
        return (data[pos] & 0xFF) | ((data[pos+1] & 0xFF) << 8);
    }

    private static byte[] zlibDecompress(byte[] compressed, int expectedSize) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
        InflaterInputStream iis = new InflaterInputStream(bis);
        byte[] result = new byte[expectedSize];
        int off = 0;
        while (off < expectedSize) {
            int r = iis.read(result, off, expectedSize - off);
            if (r <= 0) break;
            off += r;
        }
        iis.close();
        return result;
    }
}
