package com.mirwanda.not2pix;

public interface PlatformBridge {

    /** Returns the file path passed via intent, or null if launched normally */
    String getIntentFilePath();

    /** True if launched from NotTiled (has intent data) */
    boolean isFromNotTiled();

    /** Notify the calling app that editing is done and finish */
    void finishWithResult(boolean saved);

    /** Open a file via SAF (ACTION_OPEN_DOCUMENT) */
    void openFile();

    /** Save to a new file via SAF (ACTION_CREATE_DOCUMENT) */
    void saveFileAs();

    /** Close/finish the activity */
    void closeApp();

    /** Export animation as GIF */
    void exportGif();

    /** Save project as .ase */
    void saveAse();

    /** Open .ase file */
    void openAse();
}
