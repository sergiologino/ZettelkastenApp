package ru.altacod.noteapp.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "note_audios")
public class NoteAudio {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String serverFilePath;

    @Column(nullable = true)
    private String url;

    @Column(nullable = false)
    private String originalName;

    @Column(name="unique_audio_name",nullable = true)
    private String uniqueAudioName;

    @Column(name="audio_type", nullable = true)
    private String audioType;

    @Column(name = "size", nullable = true)
    private BigDecimal size;

    @Column(name = "created_at", nullable = true)
    private LocalDateTime createdAt;

    //@JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    @JsonBackReference // Указывает, что это обратная ссылка
    private Note note;

    @Column(name = "user_id", nullable = true)
    private UUID userId;

    public NoteAudio() {
    }

    public NoteAudio(UUID id, String filePath, String fileName, Note note, String url, UUID userId, String uniqueAudioName, BigDecimal size, LocalDateTime createdAt) {
        this.id = id;
        this.serverFilePath = filePath;
        this.originalName = fileName;
        this.note = note;
        this.url = url;
        this.userId = userId;
        this.uniqueAudioName = uniqueAudioName;

    }

    // Геттеры и сеттеры


    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUniqueAudioName() {return uniqueAudioName;}

    public void setUniqueAudioName(String uniqueAudiolName) {this.uniqueAudioName = uniqueAudiolName;}

    public LocalDateTime getCreatedAt() {return createdAt;}

    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}

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

    public String getUrl() { return url; }

    public void setUrl(String url) { this.url = url; }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAudioFilePath() {
        return serverFilePath;
    }

    public void setAudioFilePath(String filePath) {
        this.serverFilePath = filePath;
    }

    public String getAudioFileName() {
        return originalName;
    }

    public void setAudioFileName(String fileName) {
        this.originalName = fileName;
    }

    public Note getNote() {
        return note;
    }

    public void setNote(Note note) {
        this.note = note;
    }

    public String getAudioType() {
        return audioType;
    }

    public void setAudioType(String audioType) {
        this.audioType = audioType;
    }

    public BigDecimal getSize() {
        return size;
    }

    public void setSize(BigDecimal size) {
        this.size = size;
    }

}

