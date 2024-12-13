package com.example.noteapp.mapper;

import com.example.noteapp.dto.NoteDTO;
import com.example.noteapp.model.Note;
import com.example.noteapp.service.ProjectService;
import com.example.noteapp.service.TagService;
import org.springframework.stereotype.Component;

@Component
public class NoteConverter extends AbstractConverter {
    private final ProjectService projectService;
    private final TagService tagService;


    public NoteConverter(ProjectService projectService, TagService tagService) {
        super();
        this.projectService = projectService;
        this.tagService = tagService;

    }

    @Override
    public Note toEntity(NoteDTO dto) {
        if (dto == null) {
            return null;
        } else {
            Note note= new Note();
            note.setId(dto.getId());
            note.setContent (dto.getContent());
            note.setAnnotation(dto.getAnnotation());
            note.setAudioFilePath(dto.getAudioFilePath());
            note.setAiSummary(dto.isAiSummary());
            note.setRecognizedText(dto.getRecognizedText());
            note.setProject(projectService.getProjectById(dto.getProjectId()));
            note.setTags(tagService.getTagsByName(dto.getTags()));
            note.setNeuralNetwork(dto.getNeuralNetwork());
            note.setFilePath(dto.getFilePath());
            note.setFileType(dto.getFileType());
            note.setAnalyze(dto.isAnalyze());
            note.setPositionX(dto.getX());
            note.setPositionY(dto.getY());

            return note;
        }
    }

    @Override
    public NoteDTO toDTO(Note note) {
        if (note == null) {
            return null;
        } else {
            NoteDTO newNoteDTO =new NoteDTO();
            newNoteDTO.setId(note.getId());
            newNoteDTO.setContent (note.getContent());
            newNoteDTO.setAnnotation(note.getAnnotation());
            newNoteDTO.setAudioFilePath(note.getAudioFilePath());
            newNoteDTO.setAiSummary(note.isAiSummary());
            newNoteDTO.setRecognizedText(note.getRecognizedText());
            newNoteDTO.setProjectId(note.getProject().getId());
            newNoteDTO.setTags(tagService.tagNameList(note.getTags())); //получаем имена тэгов по тэгам из сущности
            newNoteDTO.setNeuralNetwork(note.getNeuralNetwork());
            newNoteDTO.setFileType(note.getFileType());
            newNoteDTO.setFilePath(note.getFilePath());
            newNoteDTO.setAnalyze(note.isAnalyze());
            newNoteDTO.setX(note.getPositionX());
            newNoteDTO.setY(note.getPositionY());

            return newNoteDTO;
        }
    }
}

