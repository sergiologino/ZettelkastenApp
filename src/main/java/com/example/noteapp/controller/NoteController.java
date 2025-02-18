package com.example.noteapp.controller;

import com.example.noteapp.dto.NoteAudioDTO;
import com.example.noteapp.dto.NoteDTO;
import com.example.noteapp.dto.NoteFileDTO;
import com.example.noteapp.dto.OpenGraphRequest;
import com.example.noteapp.mapper.NoteConverter;
import com.example.noteapp.mapper.NoteFileConverter;
import com.example.noteapp.model.*;
import com.example.noteapp.repository.NoteAudioRepository;
import com.example.noteapp.repository.UserRepository;
import com.example.noteapp.service.NoteService;
import com.example.noteapp.service.ProjectService;
import com.example.noteapp.service.TagService;
import com.example.noteapp.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
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
@CrossOrigin(origins = "http://localhost:3000")
public class NoteController {

    private final NoteService noteService;
    private final ProjectService projectService;
    private final NoteConverter noteConverter;
    private final TagService tagService;
    private final UserRepository userRepository;
    private final NoteFileRepository noteFileRepository;
    private final NoteAudioRepository noteAudioRepository;
    private final NoteFileConverter noteFileConverter;


    public NoteController(NoteService noteService, ProjectService projectService, NoteConverter noteConverter, TagService tagService, UserRepository userRepository, NoteFileRepository noteFileRepository, NoteAudioRepository noteAudioRepository, NoteFileConverter noteFileConverter) {
        this.noteService = noteService;
        this.projectService = projectService;
        this.noteConverter = noteConverter;
        this.tagService = tagService;
        this.userRepository = userRepository;
        this.noteFileRepository = noteFileRepository;
        this.noteAudioRepository = noteAudioRepository;
        this.noteFileConverter = noteFileConverter;
    }

    public UUID getCurrentUserId() {
        return userRepository.findByUsername(SecurityUtils.getCurrentUserId()).getId();
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
        UUID userId = userRepository.findByUsername(SecurityUtils.getCurrentUserId()).getId();
        Project project = projectService.getProjectById(projectId, userId );
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

    @Operation(summary = "Поиск заметок", description = "Ищет заметки по тексту, OpenGraph URL и именам файлов.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Результаты поиска успешно получены"),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    @GetMapping("/search")
    public ResponseEntity<List<NoteDTO>> searchNotes(@RequestParam String query) {
        List<NoteDTO> results = noteService.searchNotes(query);
        return ResponseEntity.ok(results);
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

//


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
       noteDTO.setChangedAt(LocalDateTime.now());

        List<String> newUrls = new ArrayList<>();
       if (noteDTO.getUrls()!=null && !noteDTO.getUrls().isEmpty()) {
           newUrls.addAll(noteDTO.getUrls());
       }

           // Извлечение URL из ключей OpenGraphData и добавление в список urls
           if (noteDTO.getOpenGraphData() != null) {
               noteDTO.getOpenGraphData().keySet().forEach(url -> {

                   if (!newUrls.contains(url)) {
                       System.out.println("Нет такого урла, добавляем! " + url);
                       newUrls.add(url);
                   }
               });
           }

       Note note = noteService.getNoteById(noteDTO.getId(), request);
       noteService.updateNote(noteConverter.toEntity(noteDTO), newUrls);

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
        noteDto.setCreatedAt(LocalDateTime.now());
        noteDto.setChangedAt(LocalDateTime.now());
        try {            // Проверяем, что поле content не пустое
            if (noteDto.getContent() == null || noteDto.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Текст заметки не может быть пустым.");
            }

            List<NoteFileDTO> newFiles = Optional.ofNullable(noteDto.getFiles())
                    .orElse(Collections.emptyList()) // ✅ Если null, подставляем пустой список
                    .stream()
                    .map(file -> new NoteFileDTO(
                            file.getId(),
                            file.getFileUrl(),
                            file.getName(),
                            file.getFilePath(),
                            file.getCreatedAt())) // ✅ Корректный маппинг
                    .collect(Collectors.toList());

            List<NoteAudioDTO> newAudios = Optional.ofNullable(noteDto.getAudios())
                    .orElse(Collections.emptyList()) // ✅ Если null, подставляем пустой список
                    .stream()
                    .map(audio -> new NoteAudioDTO(
                            audio.getId(),
                            audio.getAudioName(),
                            audio.getUrl(),
                            audio.getCreatedAt(),
                            audio.getType(),
                            audio.getSize()))
                    .collect(Collectors.toList());

            //--------------------- ЗАГЛУШКИ ---------------------------

            noteDto.setNeuralNetwork("YandexGPT-Lite");
            noteDto.setAnalyze(false);

            List<String> newUrls = new ArrayList<>();
            if(noteDto.getUrls()!=null || !noteDto.getUrls().isEmpty()) {
                  newUrls=noteDto.getUrls();
            };
            if (noteDto.getId() == null) {
                noteDto.setId(UUID.randomUUID()); // ✅ Генерируем ID
            }

            Note newNote = noteConverter.toEntity(noteDto);

            Note savedNote = noteService.createNote(newNote, newUrls, getCurrentUserId());
            NoteDTO newNoteDTO = noteConverter.toDTO(savedNote);

            return ResponseEntity.ok(newNoteDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при создании заметки: " + e.getMessage());
        }
    }

    @Operation(summary = "Создать новую заметку", description = "Создает новую заметку. Может содержать текст, файл или голосовой файл.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заметка успешно создана",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Note.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })

    // создать смешанную заметку из телеграм
    @PostMapping("/mixed")
    public ResponseEntity<?> createMixedNote(@RequestBody Map<String, Object> requestBody) {
        try {
            UUID userId = UUID.fromString((String) requestBody.get("userId"));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            String content = (String) requestBody.get("content");
            List<String> links = (List<String>) requestBody.getOrDefault("openGraph", new ArrayList<>());
            List<Map<String, Object>> audioFiles = (List<Map<String, Object>>) requestBody.getOrDefault("audios", new ArrayList<>());
            List<Map<String, Object>> noteFiles = (List<Map<String, Object>>) requestBody.getOrDefault("files", new ArrayList<>());

            Note note = new Note();
            note.setId(UUID.randomUUID());
            note.setContent(content);
            note.setUser(user);
            note.setCreatedAt(LocalDateTime.now());
            note.setChangedAt(LocalDateTime.now());

            // Добавление OpenGraph ссылок
            if (!links.isEmpty()) {
                List<OpenGraphData> openGraphData = links.stream()
                        .map(link -> noteService.fetchOpenGraphData(link, note))
                        .filter(Objects::nonNull)
                        .toList();
                note.setOpenGraphData(openGraphData);
            }

            // Сохранение файлов
            List<NoteFile> files = noteFiles.stream().map(data -> {
                NoteFile file = new NoteFile();
                file.setId(UUID.randomUUID());
                file.setServerFilePath((String) data.get("serverFilePath"));
                file.setOriginalName((String) data.get("originalName"));
                file.setFileType((String) data.get("fileType"));
                file.setUserId(userId);
                file.setCreatedAt(LocalDateTime.now());
                file.setNote(note);
                return file;
            }).toList();
            note.setFiles(files);

            // Сохранение аудио
            List<NoteAudio> audios = audioFiles.stream().map(data -> {
                NoteAudio audio = new NoteAudio();
                audio.setId(UUID.randomUUID());
                audio.setServerFilePath((String) data.get("serverFilePath"));
                audio.setOriginalName((String) data.get("originalName"));
                audio.setAudioType((String) data.get("audioType"));
                audio.setSize(new BigDecimal(data.get("size").toString()));
                audio.setUserId(userId);
                audio.setCreatedAt(LocalDateTime.now());
                audio.setNote(note);
                return audio;
            }).toList();

            note.setAudios(audios);
            // ✅ Проверяем и назначаем проект по умолчанию
            Project defaultProject = projectService.getDefaultBotProjectForUser(userId);
            note.setProject(defaultProject);

            // добавляем тэг - признак телеги

            Tag newTag=tagService.findOrCreateTagForBot("telegram",true, userId);
            note.getTags().add(newTag);
            if (note.getContent().isEmpty()){
                note.setContent("message from telegram");
            }

            noteService.saveNote(note, userId);
            return ResponseEntity.ok("Заметка успешно создана.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при создании заметки: " + e.getMessage());
        }
    }



    // Обработка смешанных сообщений
//    @PostMapping({"/mixed", })
//    @Operation(
//            summary = "Создать смешанную заметку",
//            description = "Создает новую заметку с текстом, ссылками и/или изображениями. Если проект не указан, используется проект по умолчанию.",
//            responses = {
//                    @ApiResponse(
//                            responseCode = "200",
//                            description = "Заметка успешно создана.",
//                            content = @Content(mediaType = "application/json")
//                    ),
//                    @ApiResponse(
//                            responseCode = "400",
//                            description = "Некорректные данные запроса.",
//                            content = @Content(mediaType = "application/json")
//                    ),
//                    @ApiResponse(
//                            responseCode = "500",
//                            description = "Ошибка на сервере.",
//                            content = @Content(mediaType = "application/json")
//                    )
//            }
//    )
//    public ResponseEntity<?> createMixedNote(@RequestBody Map<String, Object> requestBody) {
//        if (requestBody.get("userId") == null) {
//            return ResponseEntity.badRequest().body("Ошибка: Не указан пользователь.");
//        }
//
//        try {
//            String content = (String) requestBody.get("content");
//            String url = (String) requestBody.get("url");
//            String photoUrl = (String) requestBody.get("photoUrl");
//            Boolean fromTelegram =(Boolean) requestBody.get("fromTelegram");
//            UUID userId = UUID.fromString((String) requestBody.get("userId"));
//            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
//
//            Note note = new Note();
//            note.setId(UUID.randomUUID() );
//            note.setContent(content);
//            if (user==null) {
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                        .body("в NoteController не найден пользователь получатель заметки: ");
//            } else {
//                note.setUser(user);
//            }
//
//            // Добавляем OpenGraph данные
//            if (url != null && !url.isEmpty()) {
//                List<String> urls = List.of(url);
//                List<OpenGraphData> openGraphData = urls.stream()
//                        .map(link -> noteService.fetchOpenGraphData(link, note))
//                        .filter(Objects::nonNull)
//                        .collect(Collectors.toList());
//                if (openGraphData != null) {
//
//                    String title = "заголовок";
//                    String  description = "description";
////                    String title = openGraphData.getTitle() != null ? openGraphData.getTitle() : " нет заголовка";
////                    String description = openGraphData.getDescription() != null ? openGraphData.getDescription() : "нет описания";
////                    note.setContent(title + "\n" + description + "\n\n" + content); // Формируем полный контент
//
//                }
//                note.setOpenGraphData(openGraphData);
//                note.setProject(projectService.getDefaultBotProjectForUser(userId));
//
//            }
//
//            // Сохраняем фото
//            String tempPhotoPath = "";
//            String tempFileName = UUID.randomUUID() + ".jpg";
//
//            note.setCreatedAt(LocalDateTime.now());
//            note.setChangedAt(LocalDateTime.now());
//
//            // Добавляем тег "telegram"
//            if (note.getTags() == null) {
//                note.setTags(new ArrayList<>());
//            }
//            Tag newTag=tagService.findOrCreateTagForBot("telegram",true, userId);
//            note.getTags().add(newTag);
//            note.setProject(projectService.getDefaultBotProjectForUser(userId));
////            Note savedNote = noteService.saveNote(note, userId);
//
//            if (photoUrl != null && !photoUrl.isEmpty()) {
//                tempPhotoPath = noteService.downloadFileFromBot(photoUrl, "photos/", tempFileName);
//                List<NoteFile> newFiles = new ArrayList<>();
//
//                NoteFile newFile = new NoteFile();
//                newFile.setId(UUID.randomUUID());
//                newFile.setFileUrl(tempPhotoPath);
//                newFile.setUserId(userId);
//                newFile.setNote(note);
//                newFile.setFileName(tempFileName);
//                newFile.setCreatedAt(LocalDateTime.now());
//
//
//                newFiles.add(newFile);
//
//                note.setFilePath(tempPhotoPath);
//                note.setFileType("image");
//                note.setFiles(newFiles);
//                noteService.saveNote(note, userId);
//
//            }
//            Map<String, Object> response = new HashMap<>();
//            response.put("message", "Заметка успешно создана");
//            response.put("id", note.getId());
//
//            return ResponseEntity.ok("Заметка успешно создана с текстом, ссылкой и/или изображением.");
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Ошибка при создании смешанной заметки: " + e.getMessage());
//        }
//    }
//    @PostMapping("/mixed")
//    public ResponseEntity<?> createMixedNote(@RequestBody Map<String, Object> requestBody) {
//        try {
//            UUID userId = UUID.fromString((String) requestBody.get("userId"));
//            User user = userRepository.findById(userId)
//                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
//
//            String content = (String) requestBody.get("content");
//            List<String> links = (List<String>) requestBody.getOrDefault("openGraph", new ArrayList<>());
//            List<String> audioUrls = (List<String>) requestBody.getOrDefault("audios", new ArrayList<>());
//            List<String> fileUrls = (List<String>) requestBody.getOrDefault("files", new ArrayList<>());
//
//            Note note = new Note();
//            note.setId(UUID.randomUUID());
//            note.setContent(content);
//            note.setUser(user);
//            note.setCreatedAt(LocalDateTime.now());
//            note.setChangedAt(LocalDateTime.now());
//
//            // Добавление OpenGraph ссылок
//            if (!links.isEmpty()) {
//                List<OpenGraphData> openGraphData = links.stream()
//                        .map(link -> noteService.fetchOpenGraphData(link, note))
//                        .filter(Objects::nonNull)
//                        .toList();
//                note.setOpenGraphData(openGraphData);
//            }
//
//            // Сохранение файлов
//            if (!fileUrls.isEmpty()) {
//                List<NoteFile> files = fileUrls.stream().map(url -> {
//                    NoteFile file = new NoteFile();
//                    file.setId(UUID.randomUUID());
//                    file.setFileUrl(url);
//                    file.setUserId(userId);
//                    file.setNote(note);
//                    return file;
//                }).toList();
//                note.setFiles(files);
//            }
//
//            // Сохранение аудио
//            if (!audioUrls.isEmpty()) {
//                List<NoteAudio> audios = audioUrls.stream().map(url -> {
//                    NoteAudio audio = new NoteAudio();
//                    audio.setId(UUID.randomUUID());
//                    audio.setAudioFilePath(url);
//                    audio.setUserId(userId);
//                    audio.setNote(note);
//                    return audio;
//                }).toList();
//                note.setAudios(audios);
//            }
//
//            noteService.saveNote(note, userId);
//            return ResponseEntity.ok("Заметка успешно создана.");
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Ошибка при создании заметки: " + e.getMessage());
//        }
//    }


    @PostMapping("/{noteId}/files")
    public ResponseEntity<Note> uploadFilesToNote(@PathVariable UUID noteId, @RequestParam(value = "files", required = false)  List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            System.out.println("Нет файлов для загрузки, удаляем все существующие файлы.");
            Note note = noteService.getNoteById(noteId, null);

            note.getFiles().clear();
            noteFileRepository.deleteAll(note.getFiles()); // Удаляем файлы через репозиторий
            return ResponseEntity.ok(noteService.saveNote(note,null));
        }

        Note updatedNote = noteService.addFilesToNote(noteId, files);
        return ResponseEntity.ok(updatedNote);
    }

    @PostMapping("/{noteId}/audios")
    public ResponseEntity<Note> uploadAudiosToNote(
            @PathVariable UUID noteId,
            @RequestParam(value = "audios", required = false) List<MultipartFile> audios) {

        if (audios == null || audios.isEmpty()) {
            System.out.println("Запрос отсутствует, возвращаем заметку без изменений.");
            Note note = noteService.getNoteById(noteId, null);
            note.getAudios().clear();
            noteAudioRepository.deleteAll(note.getAudios());
            return ResponseEntity.ok(noteService.saveNote(note,null));
        }

        Note note = noteService.getNoteById(noteId, null);

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
    @PostMapping("/bot/files")
    public ResponseEntity<?> uploadFilesFromBot(@RequestBody Map<String, Object> requestBody) {
        if (requestBody.get("userId") == null || requestBody.get("noteId") == null || requestBody.get("fileUrls") == null) {
            return ResponseEntity.badRequest().body("Ошибка: не указаны обязательные параметры.");
        }

        UUID userId = UUID.fromString((String) requestBody.get("userId"));
        UUID noteId = UUID.fromString((String) requestBody.get("noteId"));
        List<String> fileUrls = (List<String>) requestBody.get("fileUrls");

        try {
            Note note = noteService.getNoteById(noteId, null);
            if (!note.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Ошибка: пользователь не владеет этой заметкой.");
            }

            List<NoteFile> newFiles = new ArrayList<>();
            for (String fileUrl : fileUrls) {
                String fileName = UUID.randomUUID() + ".file";
                String savedPath = noteService.downloadFileFromBot(fileUrl, "files/", fileName);

                NoteFileDTO newFile = new NoteFileDTO();
                newFile.setId(UUID.randomUUID());
                newFile.setFileUrl(savedPath);
                newFile.setFileName(fileName);
                newFile.setCreatedAt(LocalDateTime.now());
                newFile.setFilePath("files/" + fileName);
                NoteFile newFileEntity = noteFileConverter.toEntity(newFile);

                newFiles.add(newFileEntity);
            }

            note.setFiles(newFiles);
            Note updatedNote = noteService.saveNote(note, userId);
            return ResponseEntity.ok(updatedNote);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при загрузке файлов: " + e.getMessage());
        }
    }
    @DeleteMapping("/{noteId}")
    @Operation(summary = "Удалить заметку", description = "Удаляет заметку по ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заметка успешно удалена"),
            @ApiResponse(responseCode = "404", description = "Заметка не найдена"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    public ResponseEntity<?> deleteNote(@PathVariable UUID noteId) {
        try {
            noteService.deleteNoteById(noteId);
            return ResponseEntity.ok("Заметка успешно удалена");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при удалении заметки: " + e.getMessage());
        }
    }

}

