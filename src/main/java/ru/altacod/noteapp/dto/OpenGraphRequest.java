package ru.altacod.noteapp.dto;

import java.util.List;
import java.util.UUID;

public class OpenGraphRequest {

    private UUID noteId;
    private List<String> urls;

    // Геттеры и сеттеры
    public UUID getNoteId() {
        return noteId;
    }

    public void setNoteId(UUID noteId) {
        this.noteId = noteId;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }
}
