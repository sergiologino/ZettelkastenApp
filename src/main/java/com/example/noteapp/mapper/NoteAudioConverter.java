package com.example.noteapp.mapper;

import com.example.noteapp.dto.NoteAudioDTO;
import com.example.noteapp.model.NoteAudio;
import org.springframework.stereotype.Component;

@Component
public class NoteAudioConverter {

    public NoteAudioDTO toDTO(NoteAudio noteAudio) {
        if (noteAudio == null) {
            return null;
        }
        NoteAudioDTO dto = new NoteAudioDTO();
        dto.setId(noteAudio.getId());
        dto.setAudioName(noteAudio.getAudioFileName());
        dto.setAudioPath(noteAudio.getAudioFilePath());
        dto.setType(noteAudio.getAudioType());
        dto.setSize(noteAudio.getSize());

        return dto;
    }

    public NoteAudio toEntity(NoteAudioDTO dto) {
        if (dto == null) {
            return null;
        }
        NoteAudio noteAudio = new NoteAudio();
        noteAudio.setId(dto.getId());
        noteAudio.setAudioFileName(dto.getAudioName());
        noteAudio.setAudioFilePath(dto.getAudioPath());
        noteAudio.setAudioType(dto.getType());
        noteAudio.setSize(dto.getSize());

        return noteAudio;
    }
}
