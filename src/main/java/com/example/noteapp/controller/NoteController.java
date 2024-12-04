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
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Operation(summary = "Добавить файл к заметке", description = "Позволяет прикрепить файл к существующей заметке.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Файл успешно добавлен"),
            @ApiResponse(responseCode = "404", description = "Заметка не найдена"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    @PutMapping("/{noteId}/upload")
    public Note uploadFileToNote(
            @PathVariable UUID noteId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String neuralNetwork
    ) {
        return noteService.addFileToNote(noteId, file, neuralNetwork);
    }

    @Operation(summary = "Загрузить звуковой файл к заметке", description = "Позволяет прикрепить звуковой файл к заметке.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Файл успешно добавлен"),
            @ApiResponse(responseCode = "404", description = "Заметка не найдена"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    @PutMapping("/{noteId}/uploadAudio")
    public Note uploadAudioToNote(
            @PathVariable UUID noteId,
            @RequestParam("file") MultipartFile file
    ) {
        return noteService.addAudioToNote(noteId, file);
    }

    @Operation(summary = "Создать новую заметку", description = "Создает новую заметку. Может содержать текст, файл или голосовой файл.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заметка успешно создана",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    @PostMapping
    public Map<String, Object> createNote(
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "fileUrl", required = false) String fileUrl,
            @RequestParam(value = "fileName", required = false) String fileName
    ) {
        Note note = noteService.createNote(content, fileUrl, fileName);

        // Формируем ответ со ссылкой на заметку и результатом анализа
        Map<String, Object> response = new HashMap<>();
        response.put("noteUrl", "/api/notes/" + note.getId());
        response.put("analysis", note.getAnnotation());
        response.put("tags", note.getTags());
        return response;
    }

    @Operation(summary = "Получить заметку по ID", description = "Возвращает заметку с указанным идентификатором.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заметка успешно возвращена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Note.class))),
            @ApiResponse(responseCode = "404", description = "Заметка не найдена"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    @GetMapping("/{id}")
    public Note getNoteById(@PathVariable UUID id) {
        return noteService.getNoteById(id);
    }
}
