package com.mirwanda.not2pix.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.mirwanda.not2pix.Not2Pix;
import com.mirwanda.not2pix.PlatformBridge;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Not2Pix");
        config.setWindowedMode(800, 600);

        // Pass file path from command line args if provided
        final String filePath = args.length > 0 ? args[0] : null;

        final Not2Pix[] appHolder = new Not2Pix[1];
        PlatformBridge bridge = new PlatformBridge() {
            @Override
            public String getIntentFilePath() {
                return filePath;
            }

            @Override
            public boolean isFromNotTiled() {
                return filePath != null;
            }

            @Override
            public void finishWithResult(boolean saved) {
                System.exit(0);
            }

            @Override
            public void openFile() { }

            @Override
            public void saveFileAs() { }

            @Override
            public void exportGif() { }

            @Override
            public void saveAse() { }

            @Override
            public void openAse() { }

            @Override
            public void selectBackgroundImage() { }

            @Override
            public void importImage() {
                com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                    try {
                        javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception e) {}
                    javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
                    javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter(
                        "Image files", "png", "jpg", "jpeg", "bmp");
                    chooser.setFileFilter(filter);
                    int returnVal = chooser.showOpenDialog(null);
                    if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
                        String path = chooser.getSelectedFile().getAbsolutePath();
                        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                            if (appHolder[0] != null) {
                                appHolder[0].loadImportedImage(path);
                            }
                        });
                    }
                });
            }

            @Override
            public void importPalette() {
                com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                    try {
                        javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception e) {}
                    javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
                    javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter(
                        "Palette files (*.gpl, *.hex, *.txt)", "gpl", "hex", "txt");
                    chooser.setFileFilter(filter);
                    int returnVal = chooser.showOpenDialog(null);
                    if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
                        String path = chooser.getSelectedFile().getAbsolutePath();
                        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                            if (appHolder[0] != null) {
                                appHolder[0].loadPaletteFromPath(path);
                            }
                        });
                    }
                });
            }

            @Override
            public void exportPalette() {
                com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                    try {
                        javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception e) {}
                    javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
                    chooser.setSelectedFile(new java.io.File("palette.gpl"));
                    int returnVal = chooser.showSaveDialog(null);
                    if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
                        String path = chooser.getSelectedFile().getAbsolutePath();
                        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                            if (appHolder[0] != null) {
                                appHolder[0].savePaletteToPath(path);
                            }
                        });
                    }
                });
            }

            @Override
            public void closeApp() {
                System.exit(0);
            }
        };

        appHolder[0] = new Not2Pix(bridge);
        new Lwjgl3Application(appHolder[0], config);
    }
}
