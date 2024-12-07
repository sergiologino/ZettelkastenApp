package com.example.noteapp.controller;

import com.example.noteapp.model.Note;
import com.example.noteapp.model.Project;
import com.example.noteapp.repository.ProjectRepository;
import com.example.noteapp.service.NoteService;
import com.example.noteapp.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
    public Note analyzeNote(@PathVariable UUID noteId, @RequestParam String chatId) {
        return noteService.analyzeAndAssignTags(noteId, chatId);
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
    public ResponseEntity<?> createNote(@RequestBody Note note) {
        System.out.println("Полученные данные: content:" + note.getContent()+" proj_id: "+note.getProject().getId()+" note: "+note.toString());
        try {
            // Проверяем, что поле content не пустое
            if (note.getContent() == null || note.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Текст заметки не может быть пустым.");
            }

            // Проверяем, что проект указан
            if (note.getProject() == null) {
                Project newProject = projectService.getProjectById(UUID.fromString("3637ff4b-98bc-402b-af00-97bf35f84be3"));
                //return ResponseEntity.badRequest().body("Проект обязателен для создания заметки.");
            }

//            Note savedNote = noteService.createNote(note.getContent(),note.getFilePath(),note.getFileType());
            Note savedNote = noteService.createNote(note);
            return ResponseEntity.ok(savedNote);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при создании заметки: " + e.getMessage());
        }
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

    @PutMapping("/group/analyze")
    @Operation(summary = "Обработать группу заметок",
            description = "Обрабатывает группу заметок по их ID.")
    public Note analyzeGroup(
            @RequestParam List<UUID> noteIds,
            @RequestParam String chatId
    ) {
        return noteService.analyzeGroupNotes(noteIds, chatId);
    }

    @PutMapping("/project/{projectId}/analyze")
    @Operation(summary = "Обработать заметки проекта",
            description = "Обрабатывает все заметки внутри проекта.")
    public Note analyzeProjectNotes(
            @PathVariable UUID projectId,
            @RequestParam String chatId
    ) {
        return noteService.analyzeProjectNotes(projectId, chatId);
    }

    @RequestMapping("/api/projects")
    public class ProjectController {

        private final ProjectRepository projectRepository;

        public ProjectController(ProjectRepository projectRepository) {
            this.projectRepository = projectRepository;
        }

        @GetMapping("/{projectId}/notes")
        public List<Note> getNotesByProject(@PathVariable UUID projectId) {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Проект не найден"));

            return project.getNotes(); // Возвращаем список заметок проекта
        }
    }
}
