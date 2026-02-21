package com.example.notewidget.model;

import java.nio.file.Path;

public class NoteDocument {

    private String title;
    private Path path;
    private String content;
    private boolean temporary;
    private boolean modified;

    public NoteDocument(String title, Path path, String content, boolean temporary, boolean modified) {
        this.title = title;
        this.path = path;
        this.content = content == null ? "" : content;
        this.temporary = temporary;
        this.modified = modified;
    }

    public String getTitle() {
        return title;
    }

    public Path getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public boolean isModified() {
        return modified;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public void setContent(String content) {
        this.content = content == null ? "" : content;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }
}
