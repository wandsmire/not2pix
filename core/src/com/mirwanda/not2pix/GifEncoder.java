package com.mirwanda.not2pix;

import com.badlogic.gdx.graphics.Pixmap;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class GifEncoder {

    public static void write(String path, ArrayList<Pixmap> frames, int delayMs) {
        if (frames.isEmpty()) return;
        FileOutputStream fos = null;
        try {
            int w = frames.get(0).getWidth();
            int h = frames.get(0).getHeight();

            // Build global color table from all frames
            HashMap<Integer, Integer> colorMap = new HashMap<>();
            ArrayList<Integer> palette = new ArrayList<>();
            for (Pixmap pm : frames) {
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int px = pm.getPixel(x, y);
                        int a = px & 0xFF;
                        int rgb = (a < 128) ? 0x000000 : ((px >>> 8) & 0xFFFFFF);
                        if (!colorMap.containsKey(rgb) && palette.size() < 256) {
                            colorMap.put(rgb, palette.size());
                            palette.add(rgb);
                        }
                    }
                }
            }
            // Ensure at least 2 colors and find transparent index
            while (palette.size() < 2) palette.add(0);
            int transparentIdx = palette.size() < 256 ? palette.size() : 0;
            if (palette.size() < 256) palette.add(0); // transparent color slot

            // Pad to power of 2
            int tableSize = 2;
            int tableBits = 1;
            while (tableSize < palette.size()) { tableSize *= 2; tableBits++; }
            while (palette.size() < tableSize) palette.add(0);

            fos = new FileOutputStream(path);

            // GIF Header
            fos.write("GIF89a".getBytes());

            // Logical Screen Descriptor
            writeWordLE(fos, w);
            writeWordLE(fos, h);
            int packed = 0x80 | ((tableBits - 1) << 4) | (tableBits - 1); // GCT flag, color res, size
            fos.write(packed);
            fos.write(0); // bg color
            fos.write(0); // pixel aspect ratio

            // Global Color Table
            for (int c : palette) {
                fos.write((c >> 16) & 0xFF);
                fos.write((c >> 8) & 0xFF);
                fos.write(c & 0xFF);
            }

            // NETSCAPE2.0 Application Extension (looping)
            fos.write(0x21); // extension
            fos.write(0xFF); // app extension
            fos.write(11); // block size
            fos.write("NETSCAPE2.0".getBytes());
            fos.write(3); // sub-block size
            fos.write(1); // loop sub-block id
            writeWordLE(fos, 0); // loop count (0 = infinite)
            fos.write(0); // terminator

            int delay = delayMs / 10; // GIF delay is in 1/100th sec

            for (Pixmap pm : frames) {
                // Graphics Control Extension
                fos.write(0x21);
                fos.write(0xF9);
                fos.write(4); // block size
                fos.write(0x09); // disposal=restore to bg, has transparent
                writeWordLE(fos, delay);
                fos.write(transparentIdx);
                fos.write(0); // terminator

                // Image Descriptor
                fos.write(0x2C);
                writeWordLE(fos, 0); // left
                writeWordLE(fos, 0); // top
                writeWordLE(fos, w);
                writeWordLE(fos, h);
                fos.write(0); // no local color table

                // Encode pixels
                byte[] pixels = new byte[w * h];
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int px = pm.getPixel(x, y);
                        int a = px & 0xFF;
                        if (a < 128) {
                            pixels[y * w + x] = (byte) transparentIdx;
                        } else {
                            int rgb = (px >>> 8) & 0xFFFFFF;
                            Integer idx = colorMap.get(rgb);
                            pixels[y * w + x] = (byte)(idx != null ? idx : findClosest(palette, rgb));
                        }
                    }
                }

                // LZW encode
                int minCodeSize = tableBits < 2 ? 2 : tableBits;
                fos.write(minCodeSize);
                byte[] lzwData = lzwEncode(pixels, minCodeSize, tableSize);
                // Write sub-blocks
                int off = 0;
                while (off < lzwData.length) {
                    int blockLen = Math.min(255, lzwData.length - off);
                    fos.write(blockLen);
                    fos.write(lzwData, off, blockLen);
                    off += blockLen;
                }
                fos.write(0); // block terminator
            }

            fos.write(0x3B); // GIF trailer
        } catch (Exception e) {
            throw new RuntimeException("Failed to write GIF: " + e.getMessage(), e);
        } finally {
            try { if (fos != null) fos.close(); } catch (Exception ignored) {}
        }
    }

    private static int findClosest(ArrayList<Integer> palette, int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        int best = 0, bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < palette.size(); i++) {
            int c = palette.get(i);
            int dr = r - ((c >> 16) & 0xFF);
            int dg = g - ((c >> 8) & 0xFF);
            int db = b - (c & 0xFF);
            int dist = dr*dr + dg*dg + db*db;
            if (dist < bestDist) { bestDist = dist; best = i; }
        }
        return best;
    }

    private static byte[] lzwEncode(byte[] pixels, int minCodeSize, int colorCount) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int clearCode = 1 << minCodeSize;
        int eoiCode = clearCode + 1;
        int codeSize = minCodeSize + 1;
        int nextCode = eoiCode + 1;
        int maxCode = (1 << codeSize) - 1;

        HashMap<Long, Integer> dict = new HashMap<>();

        BitWriter bw = new BitWriter(out);
        bw.write(clearCode, codeSize);

        if (pixels.length == 0) {
            bw.write(eoiCode, codeSize);
            bw.flush();
            return out.toByteArray();
        }

        int prefix = pixels[0] & 0xFF;
        for (int i = 1; i < pixels.length; i++) {
            int suffix = pixels[i] & 0xFF;
            long key = ((long)prefix << 16) | suffix;
            Integer code = dict.get(key);
            if (code != null) {
                prefix = code;
            } else {
                bw.write(prefix, codeSize);
                if (nextCode <= 4095) {
                    dict.put(key, nextCode);
                    if (nextCode > maxCode && codeSize < 12) {
                        codeSize++;
                        maxCode = (1 << codeSize) - 1;
                    }
                    nextCode++;
                } else {
                    bw.write(clearCode, codeSize);
                    dict.clear();
                    nextCode = eoiCode + 1;
                    codeSize = minCodeSize + 1;
                    maxCode = (1 << codeSize) - 1;
                }
                prefix = suffix;
            }
        }
        bw.write(prefix, codeSize);
        bw.write(eoiCode, codeSize);
        bw.flush();
        return out.toByteArray();
    }

    private static void writeWordLE(OutputStream os, int v) throws IOException {
        os.write(v & 0xFF);
        os.write((v >> 8) & 0xFF);
    }

    private static class BitWriter {
        private OutputStream out;
        private int buffer = 0;
        private int bits = 0;

        BitWriter(OutputStream out) { this.out = out; }

        void write(int code, int numBits) throws IOException {
            buffer |= (code << bits);
            bits += numBits;
            while (bits >= 8) {
                out.write(buffer & 0xFF);
                buffer >>= 8;
                bits -= 8;
            }
        }

        void flush() throws IOException {
            if (bits > 0) out.write(buffer & 0xFF);
            buffer = 0;
            bits = 0;
        }
    }
}
