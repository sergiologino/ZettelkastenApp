package com.example.noteapp.mapper;

import com.example.noteapp.controller.UserController;
import com.example.noteapp.dto.NoteDTO;
import com.example.noteapp.model.*;
import com.example.noteapp.repository.UserRepository;
import com.example.noteapp.service.NoteService;
import com.example.noteapp.service.ProjectService;
import com.example.noteapp.service.TagService;
import com.example.noteapp.service.UserService;
import com.example.noteapp.utils.SecurityUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class NoteConverter extends AbstractConverter {
    private final ProjectService projectService;
    private final TagService tagService;

    private final NoteFileConverter noteFileConverter;
    private final NoteAudioConverter noteAudioConverter;
    private final UserController userController;
    private final UserRepository userRepository;
    private final UserService userService;


    public NoteConverter(ProjectService projectService, TagService tagService, NoteFileConverter noteFileConverter, NoteAudioConverter noteAudioConverter, UserController userController, UserRepository userRepository, UserService userService) {
        super();
        this.projectService = projectService;
        this.tagService = tagService;

        this.noteFileConverter = noteFileConverter;
        this.noteAudioConverter = noteAudioConverter;
        this.userController = userController;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public Note toEntity(NoteDTO dto) {
        if (dto == null) {
            return null;
        } else {
            Note note = new Note();
            UUID currentUserId = null;
            if (dto.getUserId() != null) {
                currentUserId = dto.getUserId();
                User currentUser = userRepository.findById(dto.getUserId()).get();
                note.setUser(currentUser);;
            }else {
                currentUserId = userRepository.findByUsername(SecurityUtils.getCurrentUserId()).getId();
                note.setUser(userController.getUserByUserId(currentUserId));
            }

            note.setId(dto.getId());
            note.setContent(dto.getContent());
            note.setAnnotation(dto.getAnnotation());
            note.setAudioFilePath(dto.getAudioFilePath());
            note.setAiSummary(dto.isAiSummary());
            note.setRecognizedText(dto.getRecognizedText());
            if(dto.getProjectId()!=null){
            note.setProject(projectService.getProjectById(dto.getProjectId(),currentUserId));
            }else{
                note.setProject(projectService.getProjectById(dto.getProjectId(),currentUserId));
            }
            note.setTags(tagService.getTagsByNameAndUserId(dto.getTags(), currentUserId));
            note.setNeuralNetwork(dto.getNeuralNetwork());
            note.setFilePath(dto.getFilePath());
            note.setFileType(dto.getFileType());
            note.setAnalyze(dto.isAnalyze());
            note.setPositionX(dto.getX());
            note.setPositionY(dto.getY());
            note.setWidth(dto.getWidth());
            note.setHeight(dto.getHeight());
            note.setTitle(dto.getTitle());
            note.setCreatedAt(dto.getCreatedAt());
            note.setChangedAt(dto.getChangedAt());






            if (dto.getOpenGraphData() != null) {
                List<OpenGraphData> openGraphDataList = dto.getOpenGraphData().entrySet().stream()
                        .map(entry -> {
                            OpenGraphData ogData = new OpenGraphData();
                            ogData.setUrl(entry.getKey());
                            ogData.setTitle(entry.getValue().getTitle());
                            ogData.setDescription(entry.getValue().getDescription());
                            ogData.setImage(entry.getValue().getImage());
                            ogData.setNote(note);
                            return ogData;
                        })
                        .collect(Collectors.toList());
                note.setOpenGraphData(openGraphDataList);
            }



//TODO снять комментарий после преобразования audios и files в массив на фронте
            note.setFiles(dto.getFiles().stream()
                    .map(noteFileConverter::toEntity)
                    .collect(Collectors.toList()));
            note.setAudios(dto.getAudios().stream()
                    .map(noteAudioConverter::toEntity)
                    .collect(Collectors.toList()));



            return note;
        }
    }

    @Override
    public NoteDTO toDTO(Note note) {
        if (note == null) {
            return null;
        } else {
            NoteDTO newNoteDTO = new NoteDTO();
            newNoteDTO.setId(note.getId());
            newNoteDTO.setContent(note.getContent());
            newNoteDTO.setAnnotation(note.getAnnotation());
            newNoteDTO.setAudioFilePath(note.getAudioFilePath());
            newNoteDTO.setAiSummary(note.isAiSummary());
            newNoteDTO.setRecognizedText(note.getRecognizedText());
            newNoteDTO.setProjectId(note.getProject().getId());
            newNoteDTO.setTags(tagService.tagNameList(note.getTags()));
            newNoteDTO.setNeuralNetwork(note.getNeuralNetwork());
            newNoteDTO.setFileType(note.getFileType());
            newNoteDTO.setFilePath(note.getFilePath());
            newNoteDTO.setAnalyze(note.isAnalyze());
            newNoteDTO.setX(note.getPositionX());
            newNoteDTO.setY(note.getPositionY());
            newNoteDTO.setWidth(note.getWidth());
            newNoteDTO.setHeight(note.getHeight());
            newNoteDTO.setUrls(new ArrayList<String>());
            newNoteDTO.setTitle(note.getTitle());
            newNoteDTO.setCreatedAt(note.getCreatedAt());
            newNoteDTO.setChangedAt(note.getChangedAt());
            newNoteDTO.setUserId(note.getUser().getId());

            if (note.getOpenGraphData() != null) {
                Map<String, OpenGraphData> openGraphDataMap = note.getOpenGraphData().stream()
                        .collect(Collectors.toMap(OpenGraphData::getUrl, data -> data));
                newNoteDTO.setOpenGraphData(openGraphDataMap);
            }

            newNoteDTO.setFiles(note.getFiles().stream()
                    .map(noteFileConverter::toDTO)
                    .collect(Collectors.toList()));
            newNoteDTO.setAudios(note.getAudios().stream()
                    .map(noteAudioConverter::toDTO)
                    .collect(Collectors.toList()));
            return newNoteDTO;
        }


    }
}


