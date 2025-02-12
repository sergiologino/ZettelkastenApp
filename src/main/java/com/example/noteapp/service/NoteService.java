package com.example.noteapp.service;

import com.example.noteapp.dto.NoteDTO;
import com.example.noteapp.integration.IntegrationException;
import com.example.noteapp.integration.IntegrationService;
import com.example.noteapp.mapper.NoteConverter;
import com.example.noteapp.model.*;
import com.example.noteapp.repository.NoteRepository;
import com.example.noteapp.repository.OpenGraphDataRepository;
import com.example.noteapp.repository.UserRepository;
import com.example.noteapp.utils.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.time.format.DateTimeFormatter;



@Service
public class NoteService {




//    private final NoteConverter noteConverter;
    @Value("${file.storage-path}")
    private String fileStoragePath;

    @Value("${audio.storage-path}")
    private String audioStoragePath;

    @Value("${open-graph-data.enabled}")
    private boolean openGraphDataEnabled;


    private final NoteRepository noteRepository;
    private final NoteConverter noteConverter;

    private final TagService tagService;
    private final IntegrationService integrationService;
    private final TelegramService telegramService;
    private final ProjectService projectService;

    private final OpenGraphDataRepository openGraphDataRepository;
    private final String filePath = "${file.storage-path}";
    private final String audioFilePath = "${audio.storage-path}";
    private final UserRepository userRepository;


    public NoteService(NoteRepository noteRepository, NoteConverter noteConverter, TagService tagService, IntegrationService integrationService, TelegramService telegramService, ProjectService projectService, OpenGraphDataRepository openGraphDataRepository, UserRepository userRepository) {

        this.noteRepository = noteRepository;
        this.noteConverter = noteConverter;
        this.tagService = tagService;
        this.integrationService = integrationService;
        this.telegramService = telegramService;
        this.projectService = projectService;
       // this.noteConverter = noteConverter;
        this.openGraphDataRepository = openGraphDataRepository;
//        this.noteConverter = noteConverter;
        this.userRepository = userRepository;
    }

    public UUID getCurrentUserId() {
        return userRepository.findByUsername(SecurityUtils.getCurrentUserId()).getId();
    }

    public List<Note> getAllNotes() {
        UUID userId = getCurrentUserId();
        return noteRepository.findAllbyUserId(userId);
    }

    public Note getNoteById(UUID id, HttpServletRequest request) {
        UUID userId = getCurrentUserId();
        Note note = noteRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Note not found with id: " + id));

        // Формируем полный URL для аудиофайлов
        note.getAudios().forEach(audio -> {
            audio.setUrl(generateFullAudioUrl(request, audio.getAudioFilePath()));
            System.out.println("current audiofileName: "+audio.getAudioFileName());
            System.out.println("current filePath: "+audio.getAudioFilePath());
            System.out.println("set URL for File: "+audio.getUrl());
        });

        // Формируем полный URL для файлов
        note.getFiles().forEach(file -> {
            file.setUrl(generateFullFileUrl(request, file.getFilePath()));
            System.out.println("current fileName: "+file.getFileName());
            System.out.println("current filePath: "+file.getFilePath());
            System.out.println("set URL for File: "+file.getUrl());
        });

        return note;
    }

    // Сохранить новую заметку
    public Note saveNote(Note note) {
        UUID userId = getCurrentUserId();
        User user =userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        note.setUser(user);
        return noteRepository.save(note);
    }

    // Удалить заметку по ID
    @Transactional
    public void deleteNoteById(UUID id) {
        UUID userId = getCurrentUserId();
        noteRepository.deleteByIdAndUserId(id, userId);
    }


    public Note addTagsToNote(UUID noteId, List<String> tagNames, boolean isAutoGenerated) {
        Note note = noteRepository.findById(noteId).orElseThrow(() -> new RuntimeException("Note not found"));

        try {
            // Вызываем интеграционный сервис для анализа содержимого
            List<String> autoTags = integrationService.analyzeNoteContent(note.getContent());

            // Присваиваем автоматически сгенерированные теги
            for (String tagName : autoTags) {
                Tag tag = tagService.createTag(tagName, true);
                if (!note.getTags().contains(tag)) { // Избегаем дублирования тегов
                    note.getTags().add(tag);
                }
            }
        } catch (IntegrationException e) {
            // Логируем ошибку и оставляем заметку без изменений
            System.err.println("Ошибка интеграции: " + e.getMessage());
        }

        return noteRepository.save(note);
    }

    public Note moveNoteToProject(UUID noteId, Project project) {
        UUID userId = getCurrentUserId();
        Note note = noteRepository.findByIdAndUserId(noteId, userId).orElseThrow(() -> new RuntimeException("Note not found"));
        note.setProject(project);
        return noteRepository.save(note);
    }

    public Note analyzeAndAssignTags(UUID noteId, String chatId) {
        UUID userId = getCurrentUserId();
        Note note = noteRepository.findByIdAndUserId(noteId, userId).orElseThrow(() -> new RuntimeException("Note not found"));

        // Вызываем интеграционный сервис для анализа содержимого
        List<String> autoTags = integrationService.analyzeNoteContent(note);

        // Присваиваем автоматически сгенерированные теги
        for (String tagName : autoTags) {
            Tag tag = tagService.createTag(tagName, true);
            if (!note.getTags().contains(tag)) { // Избегаем дублирования тегов
                // Реальный текст аннотации
                note.getTags().add(tag);
            }
            note.setAnnotation("Результат анализа");
            String message = "Заметка обработана!\n" +
                    "Ссылка: /api/notes/" + noteId + "\n" +
                    "Аннотация: " + note.getAnnotation() + "\n" +
                    "Теги: " + String.join(", ", autoTags);
            telegramService.sendMessage(chatId, message);
        }
        return noteRepository.save(note);
    }

    private String detectFileType(String fileName) {
        if (fileName.endsWith(".pdf")) {
            return "pdf";
        } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
            return "doc";
        } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
            return "xls";
        } else if (fileName.endsWith(".txt")) {
            return "txt";
        } else if (fileName.endsWith(".csv")) {
            return "csv";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
            return "image";
        }
        return "unknown";
    }

//    public Note addFileToNote(UUID noteId, MultipartFile file, String neuralNetwork) {
//        UUID userId = getCurrentUserId();
//        Note note = noteRepository.findByIdAndUserId(noteId, userId).orElseThrow(() -> new RuntimeException("Note not found"));
//
//        try {
//            // Определяем директорию для сохранения файлов
//            String uploadDir = "uploads/";
//            Path uploadPath = Paths.get(uploadDir);
//
//            if (!Files.exists(uploadPath)) {
//                Files.createDirectories(uploadPath);
//            }
//            // Генерируем имя файла
//            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
//            String filePath = uploadPath.resolve(originalFileName).toString();
//            Files.copy(file.getInputStream(), Paths.get(filePath));
//
//            // Обновляем информацию в заметке
//            note.setFilePath(filePath);
//            note.setFileType(detectFileType(originalFileName));
//            if (neuralNetwork != null) {
//                note.setNeuralNetwork(neuralNetwork);
//            }
//            return noteRepository.save(note);
//        } catch (Exception e) {
//            throw new RuntimeException("Ошибка при загрузке файла: " + e.getMessage(), e);
//        }
//    }


//    public Note addAudioToNote(UUID noteId, MultipartFile file) {
//        Note note = noteRepository.findById(noteId).orElseThrow(() -> new RuntimeException("Note not found"));
//
//        try {
//            // Определяем директорию для сохранения аудиофайлов
//            String uploadDir = "uploads/audio/";
//            Path uploadPath = Paths.get(uploadDir);
//
//            if (!Files.exists(uploadPath)) {
//                Files.createDirectories(uploadPath);
//            }
//
//            // Генерируем имя файла
//            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
//            String filePath = uploadPath.resolve(originalFileName).toString();
//            Files.copy(file.getInputStream(), Paths.get(filePath));
//
//            // Обновляем информацию в заметке
//            note.setAudioFilePath(filePath);
//            return noteRepository.save(note);
//        } catch (Exception e) {
//            throw new RuntimeException("Ошибка при загрузке аудиофайла: " + e.getMessage(), e);
//        }
//    }

    @Transactional
    public Note updateNote(Note note, List<String> links) {
        boolean useOpenGraph = openGraphDataEnabled;
        if (useOpenGraph) {
            if (links != null && !links.isEmpty()) {
                // Получаем текущую коллекцию OpenGraph данных для заметки
                List<OpenGraphData> existingData = openGraphDataRepository.findByNoteId(note.getId());

                // Выделяем данные для удаления: те, что не содержатся в новых ссылках
                List<OpenGraphData> dataToDelete = existingData.stream()
                        .filter(data -> !links.contains(data.getUrl()))
                        .collect(Collectors.toList());

                // Удаляем данные из базы
                if (!dataToDelete.isEmpty()) {
                    openGraphDataRepository.deleteAll(dataToDelete);
                }

                // Обновляем текущую коллекцию: удаляем записи, которых больше нет в новых ссылках
                existingData.removeIf(data -> !links.contains(data.getUrl()));

                // Добавляем новые OpenGraph данные
                List<String> existingUrls = existingData.stream()
                        .map(OpenGraphData::getUrl)
                        .collect(Collectors.toList());
                links.stream()
                        .filter(link -> !existingUrls.contains(link)) // Только новые ссылки
                        .map(link -> fetchOpenGraphData(link, note)) // Получаем OpenGraphData
                        .filter(Objects::nonNull) // Исключаем null
                        .forEach(existingData::add); // Добавляем в существующую коллекцию

                System.out.println("existingData содержит: " + existingData); // проверяем что получилось в existingData

                // Обновляем объект Note
                note.getOpenGraphData().clear();
                note.getOpenGraphData().addAll(existingData);
                // Сохраняем обновленные данные в базе
                openGraphDataRepository.saveAll(existingData);
            }
        }
        if (note.getFiles() != null) {
            for (NoteFile file : note.getFiles()) {
                if (file.getNote() == null) {
                    System.out.println("Файл " + file.getFileName() + " не привязан к заметке!");
                }
                file.setNote(note); // Убедимся, что файлы привязаны
            }
        }
        if (note.getAudios() != null) {
            for (NoteAudio audio : note.getAudios()) {
                audio.setNote(note); // Убедимся, что файлы привязаны
            }
        }

        // Сохраняем заметку
        System.out.println("окончательная запись заметки: "+note);
        noteRepository.save(note);

        return note;
    }




    @Transactional
    public Note createNote(Note note, List<String> links){

        UUID userId = getCurrentUserId();
//
        if (note.getProject() == null) {
            // Назначаем проект по умолчанию, если не указан
            Project defaultProject = projectService.getDefaultProjectForUser(userId);
            note.setProject(defaultProject);
        }
            note.setContent("Проект по умолчанию, " + System.lineSeparator() + note.getContent());
            User user=userRepository.findById(userId).orElseThrow();
            note.setUser(user);

        // ✅ Сначала сохраняем заметку, чтобы Hibernate знал её ID
        if (note.getPositionX() == null) {
            note.setPositionX(100L);
        }
        if (note.getPositionY() == null) {
            note.setPositionY(100L);
        }
//}
        Note savedNote = noteRepository.save(note);

//        noteRepository.save(note);
        // Обрабатываем ссылки и сохраняем Open Graph данные
        boolean useOpenGraph = openGraphDataEnabled;
        if (useOpenGraph) {
            if (links != null && !links.isEmpty()) {
                // Получаем текущую коллекцию
                List<OpenGraphData> existingData = openGraphDataRepository.findByNoteId(note.getId());

                // Удаляем существующие элементы, которые не соответствуют новым ссылкам
                existingData.removeIf(data -> !links.contains(data.getUrl()));

                // Добавляем новые OpenGraph данные
                List<String> existingUrls = existingData.stream()
                        .map(OpenGraphData::getUrl)
                        .collect(Collectors.toList());
                links.stream()
                        .filter(link -> !existingUrls.contains(link))
                        .map(link -> fetchOpenGraphData(link, note))
                        .filter(Objects::nonNull)
                        .forEach(existingData::add); // Добавляем в существующую коллекцию

                System.out.println("existingData содержит: " + existingData); // проверяем что получилось в existingData

                // Добавляем данные в объект Note
                savedNote.getOpenGraphData().addAll(existingData);

                // Сохраняем данные в базу через репозиторий
                openGraphDataRepository.saveAll(existingData);

            }


            System.out.println("OpenGraphData после обработки: " + note.getOpenGraphData());
//            return noteRepository.save(note);

        }
//        noteRepository.save(savedNote);
        return savedNote;


        //TODO временно, чтобы не отправлять на анализ, потом убрать, правильная есть в конце метода
        // Отправляем на анализ
//        if (note.isAnalyze()) {
//            try {
//                List<String> tags = integrationService.analyzeNoteContent(note);
//                // Присваиваем автоматически сгенерированные теги
//                for (String tagName : tags) {
//                    Tag tag = tagService.createTag(tagName, true);
//                    if (!note.getTags().contains(tag)) { // Избегаем дублирования тегов
//                        note.getTags().add(tag);
//                    }
//                }
//            } catch (Exception e) {
//                // Логируем ошибку, но не прерываем процесс
//                System.err.println("Ошибка при анализе заметки: " + e.getMessage());
//                noteRepository.save(note);
//
//
//            }
//        }
//        return note;
    }



    public OpenGraphData fetchOpenGraphData(String url, Note note) {

        try {
            // Проверяем, является ли URL корректным
            if (!isValidUrl(url)) {
                throw new IllegalArgumentException("Некорректный URL: " + url);
            }
            System.out.println("Загрузка OpenGraph данных для: " + url);

            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get();
            OpenGraphData ogData = new OpenGraphData();
            ogData.setUrl(url);
            ogData.setTitle(getMetaTagContent(document, "og:title"));
            ogData.setDescription(getMetaTagContent(document, "og:description"));
            ogData.setImage(getMetaTagContent(document, "og:image"));
            ogData.setNote(note);
            ogData.setUserId(note.getUser().getId());
            System.out.println("Успешно загружены OpenGraph данные: " + ogData.getTitle());
            return ogData;
        } catch (IOException e) {
            System.err.println("Ошибка при обработке Open Graph: " + url);
            e.printStackTrace(); // Добавлено для отладки
            return null;
        }
    }


    // получаем OpenGraph без привязки к заметке
    public OpenGraphData fetchOpenGraphDataClear(String url) {

        try {
            // Проверяем, является ли URL корректным
            if (!isValidUrl(url)) {
                throw new IllegalArgumentException("Некорректный URL: " + url);
            }
//            System.out.println("Загрузка OpenGraph данных для: " + url);

            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get();
            OpenGraphData ogData = new OpenGraphData();
            ogData.setUrl(url);
            ogData.setTitle(getMetaTagContent(document, "og:title"));
            ogData.setDescription(getMetaTagContent(document, "og:description"));
            ogData.setImage(getMetaTagContent(document, "og:image"));

//            System.out.println("Успешно получены OpenGraph данные: " + ogData.getTitle());
            return ogData;
        } catch (IOException e) {
            System.err.println("Ошибка при обработке Open Graph: " + url);
            e.printStackTrace(); // Добавлено для отладки
            return null;
        }
    }

    // Метод для проверки валидности URL
    private boolean isValidUrl(String url) {
        try {
            new URL(url); // Проверяем, можно ли создать объект URL
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (MalformedURLException e) {
            return false; // Если URL некорректен
        }
    }


    private String getMetaTagContent(Document document, String metaName) {
        return document.select("meta[property=" + metaName + "]").attr("content");
    }

    @Transactional
    public Note analyzeGroupNotes(List<UUID> noteIds, String chatId) {
        UUID userId = getCurrentUserId();
        List<Note> notes = noteRepository.findAllById(noteIds);

        if (notes.isEmpty()) {
            throw new RuntimeException("No notes found for provided IDs.");
        }

        // Собираем данные для анализа
        String combinedContent = notes.stream()
                .map(Note::getContent)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n"));

        // Отправляем на анализ
        String annotation = integrationService.analyzeGroupContent(combinedContent);
        List<String> autoTagNames = integrationService.extractTags(annotation);

        List<Tag> autoTags = autoTagNames.stream()
                .map(tagName -> tagService.createTag(tagName, true))
                .collect(Collectors.toList());

        // Создаем групповую заметку
        Note groupNote = new Note();
        groupNote.setContent("Групповая заметка: \n\n" + combinedContent);
        groupNote.setAnnotation(annotation);
        groupNote.setTags(autoTags);
        groupNote.setAiSummary(true);

        noteRepository.save(groupNote);

        // Отправляем результат в Telegram
        if(!chatId.isEmpty()){

                String message = "Групповая обработка завершена!\n" +
                        "Ссылка: /api/notes/" + groupNote.getId() + "\n" +
                        "Аннотация: " + annotation + "\n" +
                        "Теги: " + autoTags.stream().map(Tag::getName).collect(Collectors.joining(", "));
                telegramService.sendMessage(chatId, message);
        }

        return groupNote;
    }

    public Note analyzeProjectNotes(UUID projectId, String chatId) {
        List<Note> notes = noteRepository.findAllByProjectId(projectId);

        if (notes.isEmpty()) {
            throw new RuntimeException("No notes found for project ID: " + projectId);
        }

        // Собираем данные для анализа
        String combinedContent = notes.stream()
                .map(Note::getContent)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n"));

        // Отправляем на анализ
        String annotation = integrationService.analyzeGroupContent(combinedContent);
        List<String> autoTagNames  = integrationService.extractTags(annotation);

        List<Tag> autoTags = autoTagNames.stream()
                .map(tagName -> tagService.createTag(tagName, true))
                .collect(Collectors.toList());

        // Создаем групповую заметку
        Note projectGroupNote = new Note();
        projectGroupNote.setContent("Групповая заметка проекта: \n\n" + combinedContent);
        projectGroupNote.setAnnotation(annotation);
        projectGroupNote.setTags(autoTags);
        projectGroupNote.setAiSummary(true);

        noteRepository.save(projectGroupNote);

        // Отправляем результат в Telegram
        String message = "Обработка заметок проекта завершена!\n" +
                "Ссылка: /api/notes/" + projectGroupNote.getId() + "\n" +
                "Аннотация: " + annotation + "\n" +
                "Теги: " + autoTags.stream().map(Tag::getName).collect(Collectors.joining(", "));
        telegramService.sendMessage(chatId, message);

        return projectGroupNote;
    }

    public List<Note> getNotesByProjectId(UUID projectId, HttpServletRequest request) {
        List<Note> foundedNotes = noteRepository.findAllByProjectId(projectId);
//
        for (Note note : foundedNotes) {
            note.setAudios(noteRepository.findAudiosByNoteId(note.getId()));
            note.setFiles(noteRepository.findFilesByNoteId(note.getId()));


            note.getAudios().forEach(audio -> {
//                audio.setUrl(generateFullAudioUrl(request, audio.getAudioFilePath()));
                audio.setUrl("/api/notes/download/audio/" + audio.getAudioFileName());
                System.out.println("audio URL: " + audio.getUrl());

            });

            note.getFiles().forEach(file -> {
//                file.setUrl(generateFullFileUrl(request, file.getUrl()));
                file.setUrl("/api/notes/download/file/" + file.getFileName()); // Генерация ссылки для скачивания
                System.out.println("file URL: " + file.getUrl());
            });
        }
        return foundedNotes;
    }

    public List<OpenGraphData> getOpenGraphDataForNote(UUID noteId) {

          return openGraphDataRepository.findByNoteId(noteId);
    }

    @Transactional
    public Note addFilesToNote(UUID noteId, List<MultipartFile> files) {
        Note note = noteRepository.findById(noteId).orElseThrow(() -> new RuntimeException("Note not found"));

        if (files.isEmpty()) {
            System.out.println("Нет файлов для загрузки, удаляем все существующие файлы.");
            note.getFiles().clear();
            return noteRepository.save(note);
        }

        String publicPath = "/files/files/";

        // Получаем уже существующие файлы у заметки
        List<NoteFile> existingFiles = new ArrayList<>(note.getFiles());
        Set<String> newFileNames = files.stream()
                .map(file -> StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename())))
                .collect(Collectors.toSet());

        // Удаляем файлы, которых нет в переданном списке
        List<NoteFile> filesToRemove = existingFiles.stream()
                .filter(existingFile -> !newFileNames.contains(existingFile.getFileName()))
                .collect(Collectors.toList());

        if (!filesToRemove.isEmpty()) {
            System.out.println("Удаляем файлы: " + filesToRemove.stream().map(NoteFile::getFileName).toList());
            note.getFiles().removeAll(filesToRemove);
        }

        for (MultipartFile file : files) {
            try {
                // TODO временно дляч теста ставлю абсолютный путь
                String uploadDir = "E:/uploaded/uploaded-files/";
                Path uploadPath = Paths.get(uploadDir);

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // Уникальное имя файла
                String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
                String uniqueFileName = noteId + "_" + originalFileName;

                // Проверяем, есть ли уже файл с таким именем у этой заметки
                boolean fileExists = existingFiles.stream()
                        .anyMatch(existingFile -> existingFile.getFileName().equals(originalFileName));

                if (fileExists) {
                    System.out.println("Файл уже существует и не будет загружен повторно: " + originalFileName);
                    continue; // Пропускаем загрузку
                }


                Path filePath = uploadPath.resolve(uniqueFileName);
//                Files.copy(file.getInputStream(), filePath);

                System.out.println("Оригинальное имя файла: " + file.getOriginalFilename());
                System.out.println("Директория загрузки: " + uploadDir);
                System.out.println("Уникальное имя файла: " + uniqueFileName);



                Files.copy(file.getInputStream(), Paths.get(filePath.toUri()),StandardCopyOption.REPLACE_EXISTING);


                NoteFile newNoteFile = new NoteFile();
                newNoteFile.setFileName(originalFileName);
                newNoteFile.setFilePath(uploadPath + uniqueFileName);
                newNoteFile.setUrl(publicPath + uniqueFileName);
                newNoteFile.setNote(note);
                newNoteFile.setCreatedAt(LocalDateTime.now());
                newNoteFile.setUserId(note.getUser().getId());

                if (note.getFiles() == null) {
                    note.setFiles(new ArrayList<>());
                }

                note.getFiles().add(newNoteFile);

                noteRepository.save(note);



            } catch (IOException e) {
                throw new RuntimeException("Ошибка при загрузке файла: " + e.getMessage(), e);
            }
        }
        note.setChangedAt(LocalDateTime.now());
        return noteRepository.save(note);
    }

    @Transactional
    public Note addAudiosToNote(UUID noteId, List<MultipartFile> audios) {
        System.out.println("Добавляем аудио в заметку (addAudiosToNote)");
        Note note = noteRepository.findById(noteId).orElseThrow(() -> new RuntimeException("Note not found"));

        if (audios.isEmpty()) {
            System.out.println("Нет аудиофайлов для загрузки, удаляем все существующие аудиофайлы.");
            note.getAudios().clear();
            return noteRepository.save(note);
        }

        String publicPath = "/files/audio/";

        // Получаем уже существующие аудиофайлы у заметки
        List<NoteAudio> existingAudios = new ArrayList<>(note.getAudios());
        Set<String> newAudioNames = audios.stream()
                .map(audio -> StringUtils.cleanPath(Objects.requireNonNull(audio.getOriginalFilename())))
                .collect(Collectors.toSet());

        // Удаляем аудиофайлы, которых нет в переданном списке
        List<NoteAudio> audiosToRemove = existingAudios.stream()
                .filter(existingAudio -> !newAudioNames.contains(existingAudio.getAudioFileName()))
                .collect(Collectors.toList());

        if (!audiosToRemove.isEmpty()) {
            System.out.println("Удаляем аудиофайлы: " + audiosToRemove.stream().map(NoteAudio::getAudioFileName).toList());
            note.getAudios().removeAll(audiosToRemove);
        }

        for (MultipartFile audio : audios) {
            try {
                // TODO временно для теста ставлю абсолютный путь
                String uploadDir = "E:/uploaded/uploaded-audio/";
                //String uploadDir = audioStoragePath;
                Path uploadPath = Paths.get(uploadDir);

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(audio.getOriginalFilename()));

                // Проверяем, есть ли уже аудиофайл с таким именем у этой заметки
                boolean audioExists = existingAudios.stream()
                        .anyMatch(existingAudio -> existingAudio.getAudioFileName().equals(originalFileName));

                if (audioExists) {
                    System.out.println("Аудиофайл уже существует и не будет загружен повторно: " + originalFileName);
                    continue; // Пропускаем загрузку
                }
                // Извлекаем contentType и определяем расширение
                String contentType = audio.getContentType();
                String extension = (contentType != null && contentType.startsWith("audio/"))
                        ? contentType.substring("audio/".length())
                        : "unknown";

                //String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName  + "." + extension;
                String uniqueFileName = noteId+"_"+originalFileName; //  + "." + extension;

                Path filePath = uploadPath.resolve(uniqueFileName);

                Files.copy(audio.getInputStream(), Paths.get(filePath.toUri()), StandardCopyOption.REPLACE_EXISTING);


                NoteAudio newNoteAudioFile = new NoteAudio();
                newNoteAudioFile.setAudioFileName(uniqueFileName);
                newNoteAudioFile.setAudioFilePath(publicPath + uniqueFileName);
                newNoteAudioFile.setNote(note);
                newNoteAudioFile.setUserId(note.getUser().getId());
                newNoteAudioFile.setCreatedAt(LocalDateTime.now());

                if (note.getAudios() == null) {
                    note.setAudios(new ArrayList<>());
                }
                note.getAudios().add(newNoteAudioFile);

                noteRepository.save(note);


//
            } catch (IOException e) {
                throw new RuntimeException("Ошибка при загрузке файла: " + e.getMessage(), e);
            }
        }
        note.setChangedAt(LocalDateTime.now());
        return noteRepository.save(note);
    }

    @Transactional
    public Note updateNoteCoordinates(UUID noteId, Long x, Long y) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note not found"));
        note.setPositionX(x);
        note.setPositionY(y);
        return noteRepository.save(note);
    }

    public String downloadFile(String fileUrl, String storagePath, String fileName) {
        try {
            Path storageDirectory = Paths.get(storagePath);
            if (!Files.exists(storageDirectory)) {
                Files.createDirectories(storageDirectory);
            }
            Path destinationPath = storageDirectory.resolve(fileName);
            Files.copy(new URL(fileUrl).openStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
            return destinationPath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при загрузке файла: " + e.getMessage(), e);
        }
    }

    public List<Note> getNotesByTags(List<String> tags) {
        UUID userId = getCurrentUserId();
        List<Note> foundedNotes = noteRepository.findNotesByTagsAndUserId(tags, userId);
        System.out.println("Найденные заметки: " + foundedNotes);

        // Оставляем только заметки, содержащие все тэги
        List<Note> filteredNotes = foundedNotes.stream()
                .filter(note -> note.getTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.toSet())
                        .containsAll(tags))
                .collect(Collectors.toList());


        System.out.println("Отобранные заметки: "+filteredNotes);
        return filteredNotes;

    }

    public List<String> getAllUniqueTags() {
        UUID userId = getCurrentUserId();
        return noteRepository.findAllUniqueTags(userId);
    }

    public String generateFullAudioUrl(HttpServletRequest request, String relativePath) {
        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
        return baseUrl + relativePath;
    }

    public String generateFullFileUrl(HttpServletRequest request, String relativePath) {
        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
        return baseUrl + relativePath;
    }

    public List<NoteDTO> searchNotes(String query) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

        return noteRepository.findAll().stream()
                .map(note -> {
                    List<String> matches = new ArrayList<>();

                    if (note.getContent().toLowerCase().contains(query.toLowerCase())) {
                        matches.add("content");
                    }

                    boolean foundInUrls = note.getOpenGraphData().stream()
                            .anyMatch(og -> og.getUrl().toLowerCase().contains(query.toLowerCase()));

                    if (foundInUrls) {
                        matches.add("url");
                    }

                    boolean foundInFiles = note.getFiles().stream()
                            .anyMatch(file -> file.getFileName().toLowerCase().contains(query.toLowerCase()));

                    if (foundInFiles) {
                        matches.add("file");
                    }

                    NoteDTO noteDTO = noteConverter.toDTO(note);
                    noteDTO.setMatches(matches); // 👈 Добавляем список источников совпадений
                    noteDTO.setProjectName(note.getProject().getName()); // 👈 Добавляем название проекта
                    noteDTO.setProjectColor(note.getProject().getColor()); // 👈 Добавляем цвет проекта
                    noteDTO.setFormattedDate(note.getChangedAt().format(formatter)); // 👈 Форматируем дату

                    return noteDTO;
                })
                .filter(noteDTO -> !noteDTO.getMatches().isEmpty()) // Оставляем только заметки с совпадениями
                .collect(Collectors.toList());
    }

    public List<String> getUrlsByNoteId (Note note){
        return openGraphDataRepository.findUrlsByNoteId(note.getId());
    }

    public OpenGraphData getOpenGraphDataByUrl(String url) {
        System.out.println("Получен URL для поиска: " + url);
        return openGraphDataRepository.findByUrl(url).stream().findFirst()
                .orElseThrow(() -> new RuntimeException("OpenGraphData not found for URL: " + url));
    }

    public List<Tag> getTagsByNoteId(UUID noteId) { return noteRepository.findTagsByNoteId(noteId); }

    public List<Tag> getTagsByName(List<String> tags) {
        List<Tag> tagList = new ArrayList<>();
        for (String tagName : tags) {
            Tag tag = tagService.findOrCreateTag(tagName, false);
            tagList.add(tag);
        }
        return tagList;
    }

    public Map<String, OpenGraphData> processOpenGraphData(List<String> links) {
        Map<String, OpenGraphData> openGraphDataMap = new HashMap<>();

        for (String link : links) {
            try {
                Document document = Jsoup.connect(link).get();
                OpenGraphData ogData = new OpenGraphData();

                ogData.setTitle(getMetaTagContent(document, "og:title"));
                ogData.setDescription(getMetaTagContent(document, "og:description"));
                ogData.setImage(getMetaTagContent(document, "og:image"));
                ogData.setUrl(link);

                openGraphDataMap.put(link, ogData);
            } catch (IOException e) {
                // Логируем ошибку, если невозможно обработать ссылку
                System.err.println("Ошибка при обработке ссылки: " + link + " - " + e.getMessage());
            }
        }

        return openGraphDataMap;
    }
}
