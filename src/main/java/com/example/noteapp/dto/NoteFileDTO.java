package com.example.noteapp.dto;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

public class NoteFileDTO implements Serializable {

    UUID id;

    String url;

    String name;

    public NoteFileDTO(UUID id, String url, String name) {
        this.id = id;
        this.url = url;
        this.name = name;
    }

    public NoteFileDTO() {
    }


    public void setFileName(String name) {
            this.name = name;
        }

    public void setFilePath(String url) {
            this.url = url;
        }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFilePath() {
        return url;
    }

    public String getFileName() {
        return name;
    }
}