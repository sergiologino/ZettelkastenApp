package com.example.noteapp.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class NoteAudioDTO implements Serializable {
    // Геттеры и сеттеры
    @Setter
    @Getter
    private UUID id;
    private String name;
    private String url;

    @Setter
    @Getter
    private String type;
    private BigDecimal size;

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