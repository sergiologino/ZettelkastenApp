package com.example.noteapp.controller;

import com.example.noteapp.dto.NoteDTO;
import com.example.noteapp.mapper.NoteConverter;
import com.example.noteapp.model.Note;
import com.example.noteapp.model.OpenGraphData;
import com.example.noteapp.model.Project;
import com.example.noteapp.repository.ProjectRepository;
import com.example.noteapp.service.NoteService;
import com.example.noteapp.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.Objects;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;
    private final ProjectService projectService;
    private final NoteConverter noteConverter;


    public NoteController(NoteService noteService, ProjectService projectService, NoteConverter noteConverter) {
        this.noteService = noteService;
        this.projectService = projectService;
        this.noteConverter = noteConverter;
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

    @Operation(summary = "Обновить заметку ", description = "Обновляет заметку ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заметка успешно обновлена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NoteDTO.class))),
            @ApiResponse(responseCode = "404", description = "Заметка не найдена"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    @PutMapping
    public Note updateNote(@RequestBody NoteDTO noteDTO ) {
        return noteService.updateNote(noteDTO);
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
                            schema = @Schema(implementation = Note.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })


    @PostMapping("/{projectId}")
    public ResponseEntity<?> createNote(@PathVariable UUID projectId, @RequestBody NoteDTO noteDto) {
        System.out.println("Полученные данные: content:" + noteDto.getContent()+" note: "+noteDto.toString());
        noteDto.setProjectId(projectId);
        try {
            // Проверяем, что поле content не пустое
            if (noteDto.getContent() == null || noteDto.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Текст заметки не может быть пустым.");
            }

            // Проверяем, что проект указан
            // ЕСЛИ НЕТ  - ТО СТАВИМ ДЕФОЛТНЫЙ
            if (noteDto.getProjectId() == null) {
                Project newProject = projectService.getProjectById(UUID.fromString("3637ff4b-98bc-402b-af00-97bf35f84be3"));
                //return ResponseEntity.badRequest().body("Проект обязателен для создания заметки.");
            }
            //
            //--------------------- ЗАГЛУШКИ ---------------------------
            //
            noteDto.setNeuralNetwork("YandexGPT-Lite");
            noteDto.setAnalyze(false);

//            Note savedNote = noteService.createNote(note.getContent(),note.getFilePath(),note.getFileType());
            if (noteDto.getUrl() != null && !noteDto.getUrl().isEmpty()) {
                // Обрабатываем ссылки и получаем Open Graph данные
                Map<String, OpenGraphData> openGraphData = noteService.processOpenGraphData(noteDto.getUrl());
                noteDto.setOpenGraphData(openGraphData);
            }
            Note savedNote = noteService.createNote(noteConverter.toEntity(noteDto), noteDto.getUrls());
            return ResponseEntity.ok(noteDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при создании заметки: " + e.getMessage());
        }
    }

    // Обработка смешанных сообщений

    @PostMapping("/mixed")
    @Operation(
            summary = "Создать смешанную заметку",
            description = "Создает новую заметку с текстом, ссылками и/или изображениями. Если проект не указан, используется проект 'from Telegram'.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Заметка успешно создана.",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Некорректные данные запроса.",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Ошибка на сервере.",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    public ResponseEntity<?> createMixedNote(@RequestBody Map<String, Object> requestBody) {
        try {
            String content = (String) requestBody.get("content");
            String url = (String) requestBody.get("url");
            String photoUrl = (String) requestBody.get("photoUrl");

            Note note = new Note();
            note.setContent(content);

            // Добавляем OpenGraph данные
            if (url != null && !url.isEmpty()) {
                List<String> urls = List.of(url);
                List<OpenGraphData> openGraphData = urls.stream()
                        .map(link -> noteService.fetchOpenGraphData(link, note))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                note.setOpenGraphData(openGraphData);
            }

            // Сохраняем фото
            if (photoUrl != null && !photoUrl.isEmpty()) {
                String photoPath = noteService.downloadFile(photoUrl, "photos/", UUID.randomUUID() + ".jpg");
                note.setFilePath(photoPath);
                note.setFileType("image");
            }

            Note savedNote = noteService.saveNote(note);
            return ResponseEntity.ok("Заметка успешно создана с текстом, ссылкой и/или изображением.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при создании смешанной заметки: " + e.getMessage());
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

    @GetMapping("/{projectId}/notes")
    public List<Note> getNotesByProject(@PathVariable UUID projectId) {
        Project project = projectService.getProjectById(projectId);
        if (project == null) {
            new ResponseStatusException(HttpStatus.NOT_FOUND,"Project not found");
        }

        List<Note> foundedNotes=noteService.getNotesByProjectId(projectId);

        return foundedNotes; // Возвращаем список заметок проекта
    }


}

