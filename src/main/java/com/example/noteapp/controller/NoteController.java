package com.example.noteapp.controller;

import com.example.noteapp.dto.NoteDTO;
import com.example.noteapp.dto.OpenGraphRequest;
import com.example.noteapp.mapper.NoteConverter;
import com.example.noteapp.model.Note;
import com.example.noteapp.model.OpenGraphData;
import com.example.noteapp.model.Project;
import com.example.noteapp.service.NoteService;
import com.example.noteapp.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notes")
@CrossOrigin(origins = "http://localhost:3000") // Укажите ваш фронтенд-URL
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

    @Operation(summary = "Получить все заметки")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список заметок успешно получен")
    })
    @GetMapping
    public List<NoteDTO> getAllNotes() {
        List<Note> notes = noteService.getAllNotes();
        return notes.stream().map(noteConverter::toDTO).collect(Collectors.toList());
    }


    @Operation(summary = "Получить все заметки с указанными тэгами")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список заметок успешно получен")
    })
    @GetMapping("/tags/search")
    public List<NoteDTO> getNotesByTags(@RequestParam List<String> tags) {
        List<Note> notes = noteService.getNotesByTags(tags);
        return notes.stream().map(noteConverter::toDTO).collect(Collectors.toList());
    }

    @Operation(summary = "Получить все уникальные тэги")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список уникальных тэгов успешно получен")
    })
    @GetMapping("/tags")
    public List<String> getAllUniqueTags() {
        return noteService.getAllUniqueTags();
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
    public Note updateNote(@RequestBody NoteDTO noteDTO, HttpServletRequest request) {

       noteDTO.setNeuralNetwork("YandexGPT-Lite");
       noteDTO.setAnalyze(true);
       List<String> newUrls = new ArrayList<>();

        if (noteDTO.getUrls()!=null && !noteDTO.getUrls().isEmpty()) {
            for (String url : noteDTO.getUrls()) {
                newUrls.add(url);
            }
        }

           // Извлечение URL из ключей OpenGraphData и добавление в список urls
           if (noteDTO.getOpenGraphData() != null) {
               noteDTO.getOpenGraphData().keySet().forEach(url -> {

                   if (!newUrls.contains(url)) {
                       System.out.println("Нет такого урла, добавляем! " + url);

                       newUrls.add(url);
                   } else {
                       System.out.println("Такой урл есть, пропускаем! " + url);
                   }
               });
           }

        Note note=noteService.getNoteById(noteDTO.getId(), request);

        noteService.updateNote(noteConverter.toEntity(noteDTO),newUrls);

        return note;
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

        noteDto.setProjectId(projectId);
        try {            // Проверяем, что поле content не пустое
            if (noteDto.getContent() == null || noteDto.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Текст заметки не может быть пустым.");
            }
            // Проверяем, что проект указан
            // ЕСЛИ НЕТ  - ТО СТАВИМ ДЕФОЛТНЫЙ

            //--------------------- ЗАГЛУШКИ ---------------------------
            noteDto.setNeuralNetwork("YandexGPT-Lite");
            noteDto.setAnalyze(true);


            Note savedNote = noteService.createNote(noteConverter.toEntity(noteDto), noteDto.getUrls());
            NoteDTO newNoteDTO = noteConverter.toDTO(savedNote);

            return ResponseEntity.ok(newNoteDTO);
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
                if (openGraphData != null) {

                    String title = "Заголовок OpenGraph: ";
                    String description = "Описание OpenGraph: ";
//                    String title = openGraphData.getTitle() != null ? openGraphData.getTitle() : " нет заголовка";
//                    String description = openGraphData.getDescription() != null ? openGraphData.getDescription() : "нет описания";
                    note.setContent(title + "\n" + description + "\n\n" + content); // Формируем полный контент

                }
                note.setOpenGraphData(openGraphData);
                note.setProject(projectService.getProjectById(UUID.fromString("5c4a3ca8-d911-4ee4-94d6-3386239f8c04")));

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



//    @Operation(summary = "Добавить файл к заметке", description = "Позволяет прикрепить файл к существующей заметке.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Файл успешно добавлен"),
//            @ApiResponse(responseCode = "404", description = "Заметка не найдена"),
//            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
//    })
//    @PutMapping("/{noteId}/upload")
//    public Note uploadFileToNote(
//            @PathVariable UUID noteId,
//            @RequestParam("file") MultipartFile file,
//            @RequestParam(required = false) String neuralNetwork
//    ) {
//        return noteService.addFileToNote(noteId, file, neuralNetwork);
//    }
//
//    @Operation(summary = "Загрузить звуковой файл к заметке", description = "Позволяет прикрепить звуковой файл к заметке.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Файл успешно добавлен"),
//            @ApiResponse(responseCode = "404", description = "Заметка не найдена"),
//            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
//    })
//    @PutMapping("/{noteId}/uploadAudio")
//    public Note uploadAudioToNote(
//            @PathVariable UUID noteId,
//            @RequestParam("file") MultipartFile file
//    ) {
//        return noteService.addAudioToNote(noteId, file);
//    }


    @Operation(summary = "Получить заметку по ID", description = "Возвращает заметку с указанным идентификатором.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заметка успешно возвращена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Note.class))),
            @ApiResponse(responseCode = "404", description = "Заметка не найдена"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    @GetMapping("/{id}")
    public Note getNoteById(@PathVariable UUID id, HttpServletRequest request) {
        return noteService.getNoteById(id, request);
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
    public List<NoteDTO> getNotesByProject(@PathVariable UUID projectId, HttpServletRequest request) {
        List<Note> notes = noteService.getNotesByProjectId(projectId, request);


        return notes.stream()
                .map(note -> {
                    // Конвертируем Note в NoteDTO
                    NoteDTO noteDTO = noteConverter.toDTO(note);

                    // Получаем список OpenGraphData для заметки
                    List<OpenGraphData> openGraphDataList = noteService.getOpenGraphDataForNote(note.getId());

                    // Преобразуем список OpenGraphData в карту, где ключ - URL
                    Map<String, OpenGraphData> openGraphDataMap = openGraphDataList.stream()
                            .collect(Collectors.toMap(OpenGraphData::getUrl, data -> data));
                    System.out.println(openGraphDataMap);

                    // Добавляем OpenGraphData в NoteDTO
                    noteDTO.setOpenGraphData(openGraphDataMap);

                    return noteDTO;
                })
                .collect(Collectors.toList());
    }

    @Operation(
            summary = "Получить OpenGraph объект по URL",
            description = "Возвращает объект OpenGraphData, связанный с указанным URL.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Объект OpenGraphData успешно найден",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = OpenGraphData.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Объект OpenGraphData не найден",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @GetMapping("/og-data-clear")
    public ResponseEntity<OpenGraphData> getOpenGraphDataByUrl(@RequestParam String url) {
        try {
            OpenGraphData openGraphData = noteService.fetchOpenGraphDataClear(url);
            return ResponseEntity.ok(openGraphData);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/og-data")
    @Operation(
            summary = "Получить OpenGraph данные для заметки",
            description = "Принимает массив URL и ID заметки, возвращает массив OpenGraph объектов, принадлежащих указанной заметке.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Массив OpenGraph данных успешно возвращен",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = OpenGraphData.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Заметка или OpenGraph данные не найдены",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    public ResponseEntity<List<OpenGraphData>> getOpenGraphDataForNote(
            @RequestBody OpenGraphRequest request
    ) {
        try {
            List<OpenGraphData> openGraphDataList = noteService.getOpenGraphDataForNote(request.getNoteId());
//            List<OpenGraphData> openGraphDataList = noteService.getOpenGraphDataForNote(request.getNoteId(), request.getUrls());
            return ResponseEntity.ok(openGraphDataList);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    @PostMapping("/{noteId}/files")
    public ResponseEntity<Note> uploadFilesToNote(
            @PathVariable UUID noteId,
            @RequestParam("files") List<MultipartFile> files) {
        Note updatedNote = noteService.addFilesToNote(noteId, files);
        return ResponseEntity.ok(updatedNote);
    }

    @PostMapping("/{noteId}/audios")
    public ResponseEntity<Note> uploadAudiosToNote(
            @PathVariable UUID noteId,
            @RequestParam("audios") List<MultipartFile> audios) {
        Note updatedNote = noteService.addAudiosToNote(noteId, audios);
        return ResponseEntity.ok(updatedNote);
    }

    @PutMapping("/{noteId}/coordinates")
    @Operation(summary = "Обновить координаты заметки", description = "Обновляет координаты X и Y для указанной заметки.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Координаты успешно обновлены"),
            @ApiResponse(responseCode = "404", description = "Заметка не найдена"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    public ResponseEntity<NoteDTO> updateNoteCoordinates(
            @PathVariable UUID noteId,
            @RequestBody Map<String, Long> coordinates) {
        try {
            Long x = coordinates.get("x");
            Long y = coordinates.get("y");

            Note updatedNote = noteService.updateNoteCoordinates(noteId, x, y);
            return ResponseEntity.ok(noteConverter.toDTO(updatedNote));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/download/audio/{filename:.+}")
    public ResponseEntity<Resource> downloadAudioFile(@PathVariable String filename) {
        Path filePath = Paths.get("E:/uploaded/uploaded-audio").resolve(filename).normalize();
        System.out.println("Эндпойнт /download/audio/filename downloadAudioFile: "+filePath);

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/download/file/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        Path filePath = Paths.get("E:/uploaded/uploaded-files").resolve(filename).normalize();
        System.out.println("Эндпойнт /download/file/filename downloadFile: "+filePath);

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

}

