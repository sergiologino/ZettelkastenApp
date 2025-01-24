package com.example.noteapp.mapper;

import com.example.noteapp.dto.NoteFileDTO;
import com.example.noteapp.model.NoteFile;
import org.springframework.stereotype.Component;

@Component
public class NoteFileConverter {

    public NoteFileDTO toDTO(NoteFile noteFile) {
        if (noteFile == null) {
            return null;
        }
        NoteFileDTO dto = new NoteFileDTO();
        dto.setId(noteFile.getId());
        dto.setFileName(noteFile.getFileName());
        dto.setFilePath(noteFile.getFilePath());
        dto.setFileUrl(noteFile.getUrl());
        dto.setCreatedAt(noteFile.getCreatedAt());
        return dto;
    }

    public NoteFile toEntity(NoteFileDTO dto) {
        if (dto == null) {
            return null;
        }
        NoteFile noteFile = new NoteFile();
        noteFile.setId(dto.getId());
        noteFile.setFileName(dto.getFileName());
        noteFile.setFilePath(dto.getFilePath());
        noteFile.setUrl(dto.getFileUrl());
        noteFile.setCreatedAt(dto.getCreatedAt());
        return noteFile;
    }
}

