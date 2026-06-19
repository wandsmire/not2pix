package com.mirwanda.not2pix;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import java.io.InputStream;
import java.io.OutputStream;

public class AndroidLauncher extends AndroidApplication implements PlatformBridge {

    private static final int REQUEST_OPEN = 1001;
    private static final int REQUEST_SAVE = 1002;
    private static final int REQUEST_EXPORT_GIF = 1003;
    private static final int REQUEST_SAVE_ASE = 1004;
    private static final int REQUEST_OPEN_ASE = 1005;

    private String intentFilePath;
    private Uri intentContentUri;
    private Uri currentFileUri; // URI from SAF open, can write back to
    private boolean fromNotTiled = false;
    private Not2Pix app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {
            Uri uri = intent.getData();
            if (uri != null) {
                fromNotTiled = true;
                if ("file".equals(uri.getScheme())) {
                    intentFilePath = uri.getPath();
                } else if ("content".equals(uri.getScheme())) {
                    intentContentUri = uri;
                    intentFilePath = copyUriToCache(uri);
                }
            }
            if (intentFilePath == null && intent.hasExtra("file_path")) {
                fromNotTiled = true;
                intentFilePath = intent.getStringExtra("file_path");
            }
        }

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;
        app = new Not2Pix(this);
        initialize(app, config);
    }

    private String copyUriToCache(Uri uri) {
        try {
            String name = "image.png";
            try (android.database.Cursor c = getContentResolver().query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) name = c.getString(idx);
                }
            } catch (Exception ignored) {}
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return null;
            java.io.File cache = new java.io.File(getCacheDir(), name);
            OutputStream os = new java.io.FileOutputStream(cache);
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
            os.close();
            is.close();
            return cache.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getIntentFilePath() { return intentFilePath; }

    @Override
    public boolean isFromNotTiled() { return fromNotTiled; }

    @Override
    public void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        // Also add ACTION_GET_CONTENT as a secondary option for third-party file managers
        Intent getContent = new Intent(Intent.ACTION_GET_CONTENT);
        getContent.addCategory(Intent.CATEGORY_OPENABLE);
        getContent.setType("image/*");
        // Use chooser to let user pick which file manager to use
        Intent chooser = Intent.createChooser(getContent, "Open image with...");
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{intent});
        startActivityForResult(chooser, REQUEST_OPEN);
    }

    @Override
    public void saveFileAs() {
        if (currentFileUri != null) {
            // Save directly to the previously opened file
            Gdx.app.postRunnable(() -> {
                String tmpPath = getCacheDir().getAbsolutePath() + "/save_tmp.png";
                app.saveToPath(tmpPath);
                writeToUri(currentFileUri, tmpPath);
            });
        } else {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_TITLE, "untitled.png");
            startActivityForResult(intent, REQUEST_SAVE);
        }
    }

    @Override
    public void closeApp() {
        finishWithResult(false);
    }

    @Override
    public void exportGif() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/gif");
        intent.putExtra(Intent.EXTRA_TITLE, "animation.gif");
        startActivityForResult(intent, REQUEST_EXPORT_GIF);
    }

    @Override
    public void saveAse() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, "sprite.aseprite");
        startActivityForResult(intent, REQUEST_SAVE_ASE);
    }

    @Override
    public void openAse() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_OPEN_ASE);
    }

    @Override
    public void finishWithResult(boolean saved) {
        if (saved && intentContentUri != null && intentFilePath != null) {
            try {
                java.io.File cached = new java.io.File(intentFilePath);
                InputStream is = new java.io.FileInputStream(cached);
                OutputStream os = getContentResolver().openOutputStream(intentContentUri, "wt");
                if (os != null) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
                    os.flush();
                    os.close();
                }
                is.close();
            } catch (Exception e) { }
        }
        setResult(saved ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;

        if (requestCode == REQUEST_OPEN) {
            // Persist write access
            try {
                getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception e) { }
            currentFileUri = uri;
            String path = copyUriToCache(uri);
            if (path != null) {
                Gdx.app.postRunnable(() -> app.loadFromPath(path));
            }
        } else if (requestCode == REQUEST_SAVE) {
            currentFileUri = uri;
            Gdx.app.postRunnable(() -> {
                String tmpPath = getCacheDir().getAbsolutePath() + "/save_tmp.png";
                app.saveToPath(tmpPath);
                writeToUri(uri, tmpPath);
            });
        } else if (requestCode == REQUEST_EXPORT_GIF) {
            Gdx.app.postRunnable(() -> {
                String tmpPath = getCacheDir().getAbsolutePath() + "/export_tmp.gif";
                java.util.ArrayList<com.badlogic.gdx.graphics.Pixmap> pixmaps = new java.util.ArrayList<>();
                for (AnimFrame f : app.frames) pixmaps.add(f.pixmap);
                int delayMs = Math.round(1000f / app.frameRate);
                GifEncoder.write(tmpPath, pixmaps, delayMs);
                writeToUri(uri, tmpPath);
            });
        } else if (requestCode == REQUEST_SAVE_ASE) {
            Gdx.app.postRunnable(() -> {
                String tmpPath = getCacheDir().getAbsolutePath() + "/export_tmp.ase";
                AseWriter.write(tmpPath, app.canvasWidth, app.canvasHeight, app.layers, app.frames, app.frameRate);
                writeToUri(uri, tmpPath);
            });
        } else if (requestCode == REQUEST_OPEN_ASE) {
            String path = copyUriToCache(uri);
            if (path != null) {
                Gdx.app.postRunnable(() -> app.loadAseFromPath(path));
            }
        }
    }

    private void writeToUri(Uri uri, String srcPath) {
        try {
            InputStream is = new java.io.FileInputStream(srcPath);
            OutputStream os = getContentResolver().openOutputStream(uri, "wt");
            if (os != null) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
                os.flush();
                os.close();
            }
            is.close();
        } catch (Exception e) { }
    }
}
