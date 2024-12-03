package com.example.noteapp.mapper;

import com.example.noteapp.dto.NoteDTO;
import com.example.noteapp.model.Note;
import org.springframework.stereotype.Component;

@Component(value = "spaceConverter")
public class NoteConverter extends AbstractConverter {
    @Override
    public Note toEntity(NoteDTO dto) {
        if (dto == null) {
            return null;
        } else {
            Note note= new Note();
            note.setContent (dto.getContent());
            note.setAnnotation(dto.getAnnotation());
            note.setAudioFilePath(dto.getAudioFilePath());
            note.setUrl(dto.getUrl());
            note.setAiSummary(dto.isAiSummary());
            note.setRecognizedText(dto.getRecognizedText());
            return note;
        }
    }

    @Override
    public NoteDTO toDTO(Note note) {
        if (note == null) {
            return null;
        } else {
            NoteDTO newNoteDTO =new NoteDTO();
            newNoteDTO.setContent (note.getContent());
            newNoteDTO.setAnnotation(note.getAnnotation());
            newNoteDTO.setAudioFilePath(note.getAudioFilePath());
            newNoteDTO.setUrl(note.getUrl());
            newNoteDTO.setAiSummary(note.isAiSummary());
            newNoteDTO.setRecognizedText(note.getRecognizedText());

            return newNoteDTO;
        }
    }
}

