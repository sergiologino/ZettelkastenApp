package com.example.noteapp.dto;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;


@Getter
@Setter
@Value
public class NoteFileDTO implements Serializable {
    String filePath;
    String fileName;


    public NoteFileDTO(String filePath, String fileName) {

        this.filePath = filePath;
        this.fileName = fileName;

    }

}