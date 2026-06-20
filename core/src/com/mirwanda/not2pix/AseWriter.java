package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Pixmap;
import java.io.*;
import java.util.ArrayList;
import java.util.zip.DeflaterOutputStream;

public class AseWriter {

    public static void write(String path, int w, int h, ArrayList<Layer> layers, ArrayList<AnimFrame> frames, float frameRate) {
        try {
            ByteArrayOutputStream fileBytes = new ByteArrayOutputStream();
            int durationMs = Math.round(1000f / frameRate);
            int numFrames = frames.size();
            int numLayers = layers.size();

            // Build all frames data first
            ArrayList<byte[]> frameDataList = new ArrayList<>();
            for (int f = 0; f < numFrames; f++) {
                ByteArrayOutputStream frameBody = new ByteArrayOutputStream();
                int chunkCount = 0;

                // First frame: write layer chunks
                if (f == 0) {
                    for (int l = 0; l < numLayers; l++) {
                        Layer layer = layers.get(l);
                        byte[] nameBytes = layer.name.getBytes("UTF-8");
                        ByteArrayOutputStream chunkData = new ByteArrayOutputStream();
                        writeWord(chunkData, layer.visible ? 1 : 0); // flags
                        writeWord(chunkData, 0); // type normal
                        writeWord(chunkData, 0); // child level
                        writeWord(chunkData, 0); // default width
                        writeWord(chunkData, 0); // default height
                        writeWord(chunkData, 0); // blend mode
                        chunkData.write((int)(layer.opacity * 255)); // opacity
                        chunkData.write(new byte[3]); // reserved
                        writeWord(chunkData, nameBytes.length);
                        chunkData.write(nameBytes);

                        byte[] cd = chunkData.toByteArray();
                        writeDword(frameBody, cd.length + 6); // chunk size
                        writeWord(frameBody, 0x2004); // chunk type
                        frameBody.write(cd);
                        chunkCount++;
                    }
                }

                // Cel chunks for each layer
                for (int l = 0; l < numLayers; l++) {
                    // Get pixel data from frame's pixmap for layer 0, or layer pixmap
                    Pixmap px = (l == 0) ? frames.get(f).pixmap : layers.get(l).pixmap;
                    byte[] rgba = pixmapToRGBA(px, w, h);
                    byte[] compressed = zlibCompress(rgba);

                    ByteArrayOutputStream chunkData = new ByteArrayOutputStream();
                    writeWord(chunkData, l); // layer index
                    writeShort(chunkData, (short) 0); // x
                    writeShort(chunkData, (short) 0); // y
                    chunkData.write(255); // opacity
                    writeWord(chunkData, 2); // cel type = compressed
                    writeShort(chunkData, (short) 0); // zIndex
                    chunkData.write(new byte[5]); // reserved
                    writeWord(chunkData, w); // width
                    writeWord(chunkData, h); // height
                    chunkData.write(compressed);

                    byte[] cd = chunkData.toByteArray();
                    writeDword(frameBody, cd.length + 6);
                    writeWord(frameBody, 0x2005);
                    frameBody.write(cd);
                    chunkCount++;
                }

                // Build frame header + body
                byte[] body = frameBody.toByteArray();
                ByteArrayOutputStream frame = new ByteArrayOutputStream();
                int frameSize = 16 + body.length;
                writeDword(frame, frameSize); // frame size
                writeWord(frame, 0xF1FA); // magic
                writeWord(frame, chunkCount < 0xFFFF ? chunkCount : 0xFFFF); // old chunks
                writeWord(frame, durationMs); // duration
                frame.write(new byte[2]); // reserved
                writeDword(frame, chunkCount); // new chunks
                frame.write(body);
                frameDataList.add(frame.toByteArray());
            }

            // Write header (128 bytes)
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            int totalSize = 128;
            for (byte[] fd : frameDataList) totalSize += fd.length;
            writeDword(header, totalSize); // file size
            writeWord(header, 0xA5E0); // magic
            writeWord(header, numFrames); // frames
            writeWord(header, w); // width
            writeWord(header, h); // height
            writeWord(header, 32); // color depth
            writeDword(header, 1); // flags
            writeWord(header, durationMs); // speed
            writeDword(header, 0); // reserved
            writeDword(header, 0); // reserved
            header.write(0); // palette entry (transparent index)
            header.write(new byte[3]); // reserved
            writeWord(header, 0); // numColors at offset 32
            // Fill rest to 128 bytes
            int written = header.size();
            header.write(new byte[128 - written]);

            // Write to file
            FileOutputStream fos = new FileOutputStream(path);
            try {
                fos.write(header.toByteArray());
                for (byte[] fd : frameDataList) fos.write(fd);
            } finally {
                fos.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to write ASE: " + e.getMessage(), e);
        }
    }

    private static byte[] pixmapToRGBA(Pixmap pm, int w, int h) {
        int pmW = Math.min(w, pm.getWidth());
        int pmH = Math.min(h, pm.getHeight());
        byte[] data = new byte[w * h * 4];
        for (int y = 0; y < pmH; y++) {
            for (int x = 0; x < pmW; x++) {
                int pixel = pm.getPixel(x, y); // RGBA8888
                int idx = (y * w + x) * 4;
                data[idx] = (byte)((pixel >> 24) & 0xFF); // R
                data[idx+1] = (byte)((pixel >> 16) & 0xFF); // G
                data[idx+2] = (byte)((pixel >> 8) & 0xFF); // B
                data[idx+3] = (byte)(pixel & 0xFF); // A
            }
        }
        return data;
    }

    private static byte[] zlibCompress(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(bos);
        dos.write(data);
        dos.close();
        return bos.toByteArray();
    }

    private static void writeDword(OutputStream os, int v) throws IOException {
        os.write(v & 0xFF);
        os.write((v >> 8) & 0xFF);
        os.write((v >> 16) & 0xFF);
        os.write((v >> 24) & 0xFF);
    }

    private static void writeWord(OutputStream os, int v) throws IOException {
        os.write(v & 0xFF);
        os.write((v >> 8) & 0xFF);
    }

    private static void writeShort(OutputStream os, short v) throws IOException {
        os.write(v & 0xFF);
        os.write((v >> 8) & 0xFF);
    }
}
