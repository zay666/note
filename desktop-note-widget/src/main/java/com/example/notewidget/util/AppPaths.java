package com.example.notewidget.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AppPaths {

    private static final Path APP_DIR =
            Paths.get(System.getProperty("user.home"), ".desktop-note-widget");

    private AppPaths() {
    }

    public static Path appDir() {
        try {
            Files.createDirectories(APP_DIR);
            return APP_DIR;
        } catch (IOException e) {
            throw new RuntimeException("无法创建应用目录: " + APP_DIR, e);
        }
    }

    public static Path quickDraftFile() {
        return appDir().resolve("quick-draft.md");
    }
}
