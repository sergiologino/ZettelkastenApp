package ru.altacod.noteapp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.altacod.noteapp.dto.NoteAudioDTO;
import ru.altacod.noteapp.dto.NoteDTO;
import ru.altacod.noteapp.dto.NoteFileDTO;
import ru.altacod.noteapp.dto.OpenGraphRequest;
import ru.altacod.noteapp.mapper.NoteAudioConverter;
import ru.altacod.noteapp.mapper.NoteConverter;
import ru.altacod.noteapp.mapper.NoteFileConverter;
import ru.altacod.noteapp.model.*;
import ru.altacod.noteapp.repository.NoteAudioRepository;
import ru.altacod.noteapp.repository.NoteFileRepository;
import ru.altacod.noteapp.repository.UserRepository;
import ru.altacod.noteapp.service.NoteService;
import ru.altacod.noteapp.service.ProjectService;
import ru.altacod.noteapp.service.TagService;
import ru.altacod.noteapp.utils.SecurityUtils;
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

import jakarta.persistence.EntityNotFoundException;
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

    // Загружаем пути из переменных окружения
    private final String fileStoragePath = System.getenv("FILE_STORAGE_PATH");
    private final String audioStoragePath = System.getenv("AUDIO_STORAGE_PATH");


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


    @PutMapping(value="/full",  consumes = {"multipart/form-data"})
    @Operation(summary = "Обновить или создать заметку с вложениями", description = "Обновляет существующую или создает новую заметку со всеми файлами и аудиозаписями.")
    public ResponseEntity<?> saveFullNote(
            @RequestPart("note") NoteDTO noteDTO,
            @RequestPart(value = "files", required = false) List<MultipartFile> uploadedFiles,
            @RequestPart(value = "audios", required = false) List<MultipartFile> uploadedAudios,
            @RequestPart(value = "deletedFiles", required = false) String deletedFilesJson,
            @RequestPart(value = "deletedAudios", required = false) String deletedAudiosJson) throws JsonProcessingException {

        // 🔹 Преобразуем JSON в список UUID
        List<UUID> deletedFiles = new ArrayList<>();
        try {
            if (deletedFilesJson != null && !deletedFilesJson.isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                deletedFiles = objectMapper.readValue(deletedFilesJson, new TypeReference<List<UUID>>() {
                });
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // 🔹 Преобразуем JSON в список UUID
        List<UUID> deletedAudios = new ArrayList<>();
        try {
            if (deletedAudiosJson != null && !deletedAudiosJson.isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                deletedAudios = objectMapper.readValue(deletedAudiosJson, new TypeReference<List<UUID>>() {
                });
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }


        try {
            Note savedNote = noteService.saveFullNote(noteDTO, uploadedFiles, uploadedAudios, deletedFiles, deletedAudios);
            System.out.println("Заметка сохранена на сервере: " + savedNote);
            System.out.println("Convert to DTO: " + noteConverter.toDTO(savedNote));
            return ResponseEntity.ok(noteConverter.toDTO(savedNote));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при сохранении заметки: " + e.getMessage());
        }
    };
//    } catch (JsonMappingException e) {
//                throw new RuntimeException(e);
//            } catch (JsonProcessingException e) {
//                throw new RuntimeException(e);
//
//
//
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
    public ResponseEntity<NoteDTO> updateNote(@RequestBody NoteDTO noteDTO, HttpServletRequest request) {
        // Получаем текущую заметку из БД
        Note existingNote = noteService.getNoteById(noteDTO.getId(), request);
        if (existingNote == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        // Обновляем основное содержимое заметки
        Note updatedNote = noteService.updateNote(existingNote, noteDTO);
        NoteDTO returnedNote = noteConverter.toDTO(updatedNote);

        return ResponseEntity.ok(returnedNote);
    }

    @PostMapping("/{noteId}/files")
    @Operation(
            summary = "Добавить или удалить файлы заметки",
            description = "Этот эндпоинт позволяет загружать новые файлы и удалять существующие файлы из заметки."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Файлы успешно обновлены",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Note.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Заметка не найдена",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<NoteDTO> updateNoteFiles(
            @PathVariable UUID noteId,
            @RequestParam(value = "files", required = false) List<MultipartFile> newFiles,
            @RequestParam(value = "deletedFiles", required = false) List<UUID> deletedFileIds) {

        Note updatedNote = noteService.updateNoteFiles(noteId, newFiles, deletedFileIds);
        NoteDTO returnedNote=noteConverter.toDTO(updatedNote);
        return ResponseEntity.ok(returnedNote);

    }


    @PostMapping("/{noteId}/audios")
    @Operation(
            summary = "Добавить или удалить аудиофайлы заметки",
            description = "Этот эндпоинт позволяет загружать новые аудиофайлы и удалять существующие аудиофайлы из заметки."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аудиофайлы успешно обновлены",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Note.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Заметка не найдена",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<NoteDTO> updateNoteAudios(
            @PathVariable UUID noteId,
            @RequestParam(value = "audios", required = false) List<MultipartFile> newAudios,
            @RequestParam(value = "deletedAudios", required = false) List<UUID> deletedAudioIds) {

        Note updatedNote = noteService.updateNoteAudios(noteId, newAudios, deletedAudioIds);
        return ResponseEntity.ok(noteConverter.toDTO(updatedNote));
    }


    @Operation(summary = "Создать новую заметку", description = "Создает новую заметку. Может содержать текст, url, файл или аудиозапись.")
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
                            file.getCreatedAt(),
                            file.getFileType(),
                            file.getOriginalName())) // ✅ Корректный маппинг
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
            noteService.autoFillNoteAttributes(newNote); // Автозаполнение title и content

            Note savedNote = noteService.createNote(newNote, newUrls, getCurrentUserId());
            NoteDTO newNoteDTO = noteConverter.toDTO(savedNote);

            return ResponseEntity.ok(newNoteDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при создании заметки: " + e.getMessage());
        }
    }

    @Operation(summary = "Создать смешанную заметку из телеграм", description = "Создает новую заметку из сообщения бота. Может содержать текст, файл или голосовой файл.")
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
//            User user = userRepository.findById(userId)
//                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            String content = (String) requestBody.get("content");
            if (content==null || content.isEmpty()){
                content="note from telegram";
            }
            String caption =  (String) requestBody.get("caption");
            List<String> links = (List<String>) requestBody.getOrDefault("openGraph", new ArrayList<>());
            List<Map<String, Object>> audioFiles = (List<Map<String, Object>>) requestBody.getOrDefault("audios", new ArrayList<>());
            List<Map<String, Object>> noteFiles = (List<Map<String, Object>>) requestBody.getOrDefault("files", new ArrayList<>());

            NoteDTO note = new NoteDTO();
            note.setId(UUID.randomUUID());
            note.setContent(content+" files caption : "+caption);
            note.setUserId(userId);
            note.setCreatedAt(LocalDateTime.now());
            note.setChangedAt(LocalDateTime.now());
            note.setProjectId(projectService.getDefaultBotProjectForUser(userId).getId());


            note.getTags().add("telegram");

            // сохраняем пока без файлов
//            Note savedNote = noteService.saveMixedNote(note, userId);
//            note.setId(savedNote.getId());

            // Сохранение файлов

            List<NoteFile> files = noteFiles.stream().map(data -> {
                NoteFile file = new NoteFile();
//                file.setId(UUID.randomUUID());
                String tempVar= (String) data.get("serverFilePath");
                file.setServerFilePath(tempVar);
//                file.setFilePath((String) data.get("filePath"));
                file.setOriginalName((String) data.get("originalName"));
                file.setFileType((String) data.get("fileType"));
                file.setCreatedAt(LocalDateTime.now());
                return file;
            }).toList();
//            note.setFiles(files);

            // Сохранение аудио
            List<NoteAudio> audios = audioFiles.stream().map(data -> {
                NoteAudio audio = new NoteAudio();
//                audio.setId(UUID.randomUUID());
                audio.setUrl((String) data.get("url"));
                audio.setCreatedAt(LocalDateTime.now());
                audio.setAudioType((String) data.get("type"));//
                audio.setAudioFilePath((String) data.get("serverFilePath"));//
                audio.setSize(new BigDecimal(data.get("size").toString()));//
                audio.setCreatedAt(LocalDateTime.now());
                return audio;
            }).toList();
//            note.setAudios(audios);
            NoteAudio noteAudio = new NoteAudio();

            Note savedNote= noteService.saveMixedNote(note, userId, links);
            if (savedNote == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Ошибка при сохранении заметки без вложений.");
            }
            if (!files.isEmpty()) {
                savedNote = noteService.attachFilesToNote(savedNote.getId(), files);
                if (savedNote == null) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Заметка сохранена, но произошла ошибка при сохранении файлов.");
                }
            }
            // Шаг 3. Прикрепляем аудиофайлы, если они переданы
            if (!audios.isEmpty()) {
                savedNote = noteService.attachAudiosToNote(savedNote.getId(), audios);
                if (savedNote == null) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Заметка сохранена, файлы прикреплены, но произошла ошибка при сохранении аудиофайлов.");
                }
            }
            // Формируем итоговый ответ
            String responseMessage = "Заметка успешно создана с id: " + savedNote.getId() +
                    ". Файлы: " + files.size() + ", аудио: " + audios.size() + ".";
            return ResponseEntity.ok(responseMessage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при создании заметки: " + e.getMessage());
        }
    }

//    @PostMapping("/{noteId}/files")
//    public ResponseEntity<Note> uploadFilesToNote(@PathVariable UUID noteId, @RequestParam(value = "files", required = false)  List<MultipartFile> files) {
//
//        if (files == null || files.isEmpty()) {
//            System.out.println("Нет файлов для загрузки, удаляем все существующие файлы.");
//            Note note = noteService.getNoteById(noteId, null);
//
//            note.getFiles().clear();
//            noteFileRepository.deleteAll(note.getFiles()); // Удаляем файлы через репозиторий
//            return ResponseEntity.ok(noteService.saveNote(note,null));
//        }
//
//        Note updatedNote = noteService.addFilesToNote(noteId, files);
//        System.out.println("файлы добавлены "+ files.size());
//        return ResponseEntity.ok(updatedNote);
//    }

//    @PostMapping("/{noteId}/audios")
//    public ResponseEntity<Note> uploadAudiosToNote(
//            @PathVariable UUID noteId,
//            @RequestParam(value = "audios", required = false) List<MultipartFile> audios) {
//
//        if (audios == null || audios.isEmpty()) {
//            System.out.println("Запрос отсутствует, возвращаем заметку без изменений.");
//            Note note = noteService.getNoteById(noteId, null);
//            note.getAudios().clear();
//            noteAudioRepository.deleteAll(note.getAudios());
//            return ResponseEntity.ok(noteService.saveNote(note,null));
//        }
//
//        Note note = noteService.getNoteById(noteId, null);
//
//        Note updatedNote = noteService.addAudiosToNote(noteId, audios);
//        System.out.println("аудиофайлы добавлены "+ audios.size());
//        return ResponseEntity.ok(updatedNote);
//    }

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
        Path filePath = Paths.get(audioStoragePath).resolve(filename).normalize();
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
        Path filePath = Paths.get(fileStoragePath).resolve(filename).normalize();
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
//                    System.out.println(openGraphDataMap);

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

