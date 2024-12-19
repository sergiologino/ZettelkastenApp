package com.example.noteapp.model;

import jakarta.persistence.*;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
@Table(name = "note_audios")
public class NoteAudio {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String serverFilePath;

    @Column(nullable = false)
    private String originalName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    public NoteAudio() {
    }

    public NoteAudio(UUID id, String filePath, String fileName, Note note) {
        this.id = id;
        this.serverFilePath = filePath;
        this.originalName = fileName;
        this.note = note;
    }

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

    // Геттеры и сеттеры
}

