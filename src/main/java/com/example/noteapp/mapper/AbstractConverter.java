package com.example.noteapp.mapper;

import com.example.noteapp.NoteappApplication;
import com.example.noteapp.dto.NoteDTO;
import com.example.noteapp.model.Note;

import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractConverter<T, DTO> {

    public List<Note> fromEntitys(List<NoteDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return null;
        } else {
            return dtos.stream().map(this::toEntity).collect(Collectors.toList());
        }
    }



    public List<NoteDTO> toDTOs(List<Note> ts) {
        if (ts == null || ts.isEmpty()) {
            return null;
        } else {
            return ts.stream().map(this::toDTO).collect(Collectors.toList());
        }
    }

    public abstract Note toEntity(NoteDTO dto);

    public abstract NoteDTO toDTO(Note space);
}