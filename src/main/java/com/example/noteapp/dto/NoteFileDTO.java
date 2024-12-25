package com.example.noteapp.dto;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

public class NoteFileDTO implements Serializable {

    UUID id;

    String filePath;

    String fileName;

    public NoteFileDTO(UUID id, String filePath, String fileName) {
        this.id = id;
        this.filePath = filePath;
        this.fileName = fileName;
    }

    public NoteFileDTO() {
    }


    public void setFileName(String fileName) {
            this.fileName = fileName;
        }

    public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }
}