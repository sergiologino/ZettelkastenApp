package com.example.noteapp.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.Value;

import java.io.Serializable;
import java.util.UUID;


@Value
@Getter
@Setter
public class NoteAudioDTO implements Serializable {
    String filePath;
    String fileName;

    public NoteAudioDTO(String filePath, String fileName) {
        this.filePath = filePath;
        this.fileName = fileName;
    }
}