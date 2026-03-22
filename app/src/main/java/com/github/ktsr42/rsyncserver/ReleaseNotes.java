package com.github.ktsr42.rsyncserver;

public class ReleaseNotes {
    private static String notes_9_10_0 = "Enable accessing SD card storage in addition to the internal shared storage (\"emulated storage\").\n"
            + "Each storage area (SD card or internal) will be mapped to a well-defined rsync \"module\" name.";
    static String getReleaseNotes(String versionName) {
        if(versionName.equals("0.9.10")) return notes_9_10_0;
        return null;
    }
}
