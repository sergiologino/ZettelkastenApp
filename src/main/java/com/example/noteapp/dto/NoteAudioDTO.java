package com.example.noteapp.dto;

import java.io.Serializable;
import java.util.UUID;


public class NoteAudioDTO implements Serializable {
    private UUID id;
    private String name;
    private String url;

    // Геттеры и сеттеры
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAudioName() {
        return name;
    }

    public void setAudioName(String name) {
        this.name = name;
    }

    public String getAudioPath() {
        return url;
    }

    public void setAudioPath(String url) {
        this.url = url;
    }
}