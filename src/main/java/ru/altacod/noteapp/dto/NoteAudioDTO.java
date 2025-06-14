package ru.altacod.noteapp.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class NoteAudioDTO implements Serializable {
    // Геттеры и сеттеры
    @Setter
    @Getter
    private UUID id;
    private String name;
    private String uniqueAudioName;
    private String url;
    private LocalDateTime createdAt;
    @Setter
    @Getter
    private String type;
    private BigDecimal size;

    public String getAudioName() {
        return name;
    }

    public LocalDateTime getCreatedAt() {return createdAt;}

    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}

    public void setAudioName(String name) {
        this.name = name;
    }

    public String getAudioPath() {
        return url;
    }

    public void setAudioPath(String url) {
        this.url = url;
    }

    public NoteAudioDTO(UUID id, String name, String url, LocalDateTime createdAt, String type, BigDecimal size, String uniqueAudioName) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.createdAt = createdAt;
        this.type = type;
        this.size = size;
        this.uniqueAudioName = uniqueAudioName;
    }

    public NoteAudioDTO() {
    }
}