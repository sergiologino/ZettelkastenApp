package com.example.noteapp.controller;

import com.example.noteapp.model.Note;
import com.example.noteapp.model.Project;
import com.example.noteapp.service.NoteService;
import com.example.noteapp.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;
    private final ProjectService projectService;

    public NoteController(NoteService noteService, ProjectService projectService) {
        this.noteService = noteService;
        this.projectService = projectService;
    }

    @Operation(summary = "Создать новую заметку", description = "Создает заметку с возможностью привязки к проекту.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Заметка успешно создана",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Note.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    @PostMapping
    public Note createNote(@RequestBody Note note, @RequestParam(required = false) UUID projectId) {
        if (projectId != null) {
            Project project = projectService.getProjectById(projectId);
            note.setProject(project);
        }
        return noteService.saveNote(note);
    }

    @Operation(summary = "Переместить заметку в другой проект", description = "Перемещает заметку между проектами.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заметка успешно перемещена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Note.class))),
            @ApiResponse(responseCode = "404", description = "Заметка или проект не найдены"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    @PutMapping("/{noteId}/move")
    public Note moveNoteToProject(@PathVariable UUID noteId, @RequestParam UUID projectId) {
        Project project = projectService.getProjectById(projectId);
        return noteService.moveNoteToProject(noteId, project);
    }

    @Operation(summary = "Проанализировать заметку", description = "Отправляет заметку на анализ и присваивает автоматически сгенерированные теги.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заметка успешно проанализирована",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Note.class))),
            @ApiResponse(responseCode = "404", description = "Заметка не найдена"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    @PutMapping("/{noteId}/analyze")
    public Note analyzeNote(@PathVariable UUID noteId) {
        return noteService.analyzeAndAssignTags(noteId);
    }

}
