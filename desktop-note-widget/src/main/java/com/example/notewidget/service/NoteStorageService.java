package com.example.notewidget.service;

import com.example.notewidget.util.AppPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public class NoteStorageService {

    public String readMarkdown(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    public void writeMarkdown(Path path, String content) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(
                path,
                content == null ? "" : content,
                StandardCharsets.UTF_8,
                CREATE,
                TRUNCATE_EXISTING,
                WRITE
        );
    }

    public String loadQuickDraft() {
        Path draft = AppPaths.quickDraftFile();
        if (!Files.exists(draft)) {
            return "";
        }
        try {
            return Files.readString(draft, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    public void saveQuickDraft(String content) {
        Path draft = AppPaths.quickDraftFile();
        try {
            writeMarkdown(draft, content);
        } catch (IOException ignored) {
            // 草稿保存失败不阻塞主流程
        }
    }

    public void clearQuickDraft() {
        Path draft = AppPaths.quickDraftFile();
        try {
            Files.deleteIfExists(draft);
        } catch (IOException ignored) {
            // 清理失败可忽略
        }
    }
}
