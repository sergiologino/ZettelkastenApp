package com.example.noteapp.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class NoteFileDTO implements Serializable {

    UUID id;

    private String fileUrl;

    private String filePath;


    private String name;


    private LocalDateTime createdAt;



    public NoteFileDTO(UUID id, String url, String name, String filePath, String fileUrl) {
        this.id = id;
        this.name = name;
        this.fileUrl = fileUrl;
        this.filePath = filePath;

    }

    public NoteFileDTO() {
    }

    public LocalDateTime getCreatedAt() {return createdAt;}

    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFileName(String name) {
            this.name = name;
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
        return name;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}