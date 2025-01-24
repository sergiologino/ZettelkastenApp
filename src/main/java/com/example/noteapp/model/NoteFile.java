package com.example.noteapp.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "note_files")
public class NoteFile {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String serverFilePath;



    @Column(nullable = true)
    private String fileUrl;

    @Column(nullable = false)
    private String originalName;

    @Column(name = "created_at", nullable = true)
    private LocalDateTime createdAt;

    //@JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    @JsonBackReference // Указывает, что это обратная ссылка
    private Note note;

    public NoteFile() {
    }

    public NoteFile(UUID id, String filePath, String fileName, Note note, String fileUrl) {
        this.id = id;
        this.serverFilePath = filePath;
        this.originalName = fileName;
        this.note = note;
        this.fileUrl = fileUrl;
    }

    public LocalDateTime getCreatedAt() {return createdAt;}

    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFilePath() {
        return serverFilePath;
    }

    public void setFilePath(String filePath) {
        this.serverFilePath = filePath;
    }

    public String getFileName() {
        return originalName;
    }

    public void setFileName(String fileName) {
        this.originalName = fileName;
    }


    public Note getNote() {
        return note;
    }

    public void setNote(Note note) {
        this.note = note;
    }

    public String getServerFilePath() {
        return serverFilePath;
    }

    public void setServerFilePath(String serverFilePath) {
        this.serverFilePath = serverFilePath;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getUrl() {return fileUrl; }

    public void setUrl(String fileUrl) { this.fileUrl = fileUrl; }


}

