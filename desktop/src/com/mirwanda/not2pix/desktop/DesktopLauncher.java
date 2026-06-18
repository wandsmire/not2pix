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
            public void closeApp() {
                System.exit(0);
            }
        };

        new Lwjgl3Application(new Not2Pix(bridge), config);
    }
}
