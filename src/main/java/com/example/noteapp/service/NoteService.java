package com.example.noteapp.service;

import com.example.noteapp.dto.NoteDTO;
import com.example.noteapp.integration.IntegrationException;
import com.example.noteapp.integration.IntegrationService;
import com.example.noteapp.mapper.NoteConverter;
import com.example.noteapp.model.Note;
import com.example.noteapp.model.User;
import com.example.noteapp.model.NoteAudio;
import com.example.noteapp.model.NoteFile;
import com.example.noteapp.model.OpenGraphData;
import com.example.noteapp.model.Tag;
import com.example.noteapp.model.Project;
import com.example.noteapp.repository.NoteAudioRepository;
import com.example.noteapp.repository.NoteFileRepository;
import com.example.noteapp.repository.NoteRepository;
import com.example.noteapp.repository.OpenGraphDataRepository;
import com.example.noteapp.repository.UserRepository;
import com.example.noteapp.utils.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final NoteAudioRepository noteAudioRepository;
    private final NoteFileRepository noteFileRepository;


    public NoteService(NoteRepository noteRepository, NoteConverter noteConverter, TagService tagService, IntegrationService integrationService, TelegramService telegramService, ProjectService projectService, OpenGraphDataRepository openGraphDataRepository, UserRepository userRepository, NoteAudioRepository noteAudioRepository,
                       NoteFileRepository noteFileRepository) {

        this.noteRepository = noteRepository;
        this.noteConverter = noteConverter;
        this.tagService = tagService;
        this.integrationService = integrationService;
        this.telegramService = telegramService;
        this.projectService = projectService;
        this.openGraphDataRepository = openGraphDataRepository;
        this.userRepository = userRepository;
        this.noteAudioRepository = noteAudioRepository;
        this.noteFileRepository = noteFileRepository;
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
        if (request != null) {
            note.getAudios().forEach(audio -> {
                audio.setUrl(generateFullAudioUrl(request, audio.getAudioFilePath()));
            });
            note.getFiles().forEach(file -> {
                file.setUrl(generateFullFileUrl(request, file.getFilePath()));
            });
        } else {
            note.getAudios().forEach(audio -> {
                audio.setUrl("/api/notes/download/audio/" + audio.getAudioFileName());
            });
            note.getFiles().forEach(file -> {
                file.setUrl("/api/notes/download/file/" + file.getFileName());
            });
        }
        return note;
    }

    // Сохранить новую заметку
    @Transactional
    public Note saveNote(Note note, UUID userId) {
        if (userId == null) {
            userId = getCurrentUserId();
        }

        UUID finalUserId = userId;
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + finalUserId));

        note.setUser(user);

        // ✅ Если проект не указан, назначаем проект по умолчанию
        if (note.getProject() == null) {
            note.setProject(projectService.getDefaultBotProjectForUser(userId));
        }

        System.out.println("📌 Сохраняем заметку: " + note.getId());
        System.out.println("📌 Проект: " + (note.getProject() != null ? note.getProject().getId() : "NULL"));
        System.out.println("📌 Файлы до сохранения заметки: " + note.getFiles().size());

        // ✅ Сначала сохраняем заметку, чтобы у неё появился ID
        note = noteRepository.saveAndFlush(note);

        // ✅ Устанавливаем note_id в файлах, так как теперь у note есть ID
        if (!note.getFiles().isEmpty()) {
            for (NoteFile file : note.getFiles()) {
                file.setNote(note);
            }
            noteFileRepository.saveAll(note.getFiles()); // Сохраняем файлы
        }

        // ✅ Аналогично для аудиофайлов
        if (!note.getAudios().isEmpty()) {
            for (NoteAudio audio : note.getAudios()) {
                audio.setNote(note);
            }
            noteAudioRepository.saveAll(note.getAudios());
        }


        noteRepository.save(note);
        System.out.println("✅ Заметка сохранена: " + note.getId());
        return note;
    }


    @Transactional
    public Note saveMixedNote(NoteDTO noteDTO, UUID userId, List<String> links) {
        Note note = noteConverter.toEntity(noteDTO);

        if (note.getTitle() == null || note.getTitle().isEmpty()){
            note.setTitle("note id: " + note.getId().toString());
        }

        // Если проект не указан – назначаем проект по умолчанию
        if (note.getProject() == null) {
            note.setProject(projectService.getDefaultBotProjectForUser(userId));
        }

        // Обработка OpenGraph ссылок
        if (links != null && !links.isEmpty()) {
            Note finalNote = note;
            List<OpenGraphData> openGraphData = links.stream()
                    .map(link -> fetchOpenGraphData(link, finalNote))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            note.setOpenGraphData(openGraphData);
        }

        // Убираем вложения – они будут добавлены отдельно
        note.setFiles(new ArrayList<>());
        note.setAudios(new ArrayList<>());

        // Сохраняем заметку и получаем её ID
        note = noteRepository.save(note);
        System.out.println("✅ Заметка сохранена без вложений: " + note.getId());
        return note;
    }



    // Шаг 2. Прикрепление файлов (в новой транзакции)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Note attachFilesToNote(UUID noteId, List<NoteFile> files) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new EntityNotFoundException("Note not found with id: " + noteId));

        for (NoteFile file : files) {
//            file.setId(UUID.randomUUID());
            file.setNote(note);
        }
        // Добавляем файлы к заметке и сохраняем их
        note.getFiles().addAll(files);
        noteFileRepository.saveAll(files);
        note.setChangedAt(LocalDateTime.now());
        note = noteRepository.save(note);
        System.out.println("✅ Файлы успешно прикреплены к заметке: " + note.getId());
        return note;
    }

    // Шаг 3. Прикрепление аудиофайлов (также в новой транзакции)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Note attachAudiosToNote(UUID noteId, List<NoteAudio> audios) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new EntityNotFoundException("Note not found with id: " + noteId));

        for (NoteAudio audio : audios) {
//            audio.setId(UUID.randomUUID());
            audio.setNote(note);
        }
        // Добавляем аудиофайлы к заметке и сохраняем их
        note.getAudios().addAll(audios);
        noteAudioRepository.saveAll(audios);
        note.setChangedAt(LocalDateTime.now());
        note = noteRepository.save(note);
        System.out.println("✅ Аудиофайлы успешно прикреплены к заметке: " + note.getId());
        return note;
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
                    file.setNote(note); // Убедимся, что файлы привязаны
                }
            }
        }
        if (note.getAudios() != null) {
            for (NoteAudio audio : note.getAudios()) {
                if (audio.getNote() == null) {
                    audio.setNote(note); // Убедимся, что файлы привязаны
                }
            }
        }

        // Сохраняем заметку
        System.out.println("окончательная запись заметки: "+note);
        noteRepository.save(note);

        return note;
    }




    @Transactional
    public Note createNote(Note note, List<String> links, UUID userId) {

        if (userId==null){
            userId = getCurrentUserId();
        };

        UUID finalUserId = userId;
        User user =userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found with id: " + finalUserId));
        note.setUser(user);


//
        if (note.getProject() == null) {
            // Назначаем проект по умолчанию, если не указан
            Project defaultProject = projectService.getDefaultProjectForUser();
            note.setProject(defaultProject);
        }
        if (note.getContent() == null) {
            note.setContent("Проект по умолчанию, " + System.lineSeparator() + note.getContent());
        }


        // ✅ Сначала сохраняем заметку, чтобы Hibernate знал её ID
        if (note.getPositionX() == null) {
            note.setPositionX(100L);
        }
        if (note.getPositionY() == null) {
            note.setPositionY(100L);
        }
        note.setCreatedAt(LocalDateTime.now());
        note.setChangedAt(LocalDateTime.now());

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
        if (note.getFiles() != null) {
            for (NoteFile file : note.getFiles()) {
                file.setNote(savedNote); // ✅ Привязываем к заметке
                noteFileRepository.save(file);
            }
        }

        if (note.getAudios() != null) {
            for (NoteAudio audio : note.getAudios()) {
                audio.setNote(savedNote); // ✅ Привязываем к заметке
                noteAudioRepository.save(audio);
            }
        }
//        noteRepository.save(savedNote);
        return savedNote;


     }



    public OpenGraphData fetchOpenGraphData(String url, Note note) {
        try {
            // Проверяем валидность URL
            if (!isValidUrl(url)) {
                throw new IllegalArgumentException("Некорректный URL: " + url);
            }

            System.out.println("Загрузка OpenGraph данных для: " + url);

            // Устанавливаем SSL-свойства (если сайт использует самоподписанный сертификат)
            System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,TLSv1");
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");

            // Загружаем страницу с доп. заголовками
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Referer", "https://www.google.com/")
                    .ignoreHttpErrors(true) // Игнорируем ошибки HTTP (403, 500)
                    .timeout(10000) // Увеличиваем таймаут (10 секунд)
                    .get();

            // Извлекаем OpenGraph данные
            OpenGraphData ogData = new OpenGraphData();
            ogData.setUrl(url);
            ogData.setTitle(getMetaTagContent(document, "og:title"));
            if (ogData.getTitle().isEmpty()) {
                ogData.setTitle(ogData.getUrl().toString()); // Используем title, если OpenGraph теги отсутствуют
            }
            ogData.setDescription(getMetaTagContent(document, "og:description"));
            ogData.setImage(getMetaTagContent(document, "og:image"));
            ogData.setNote(note);
            ogData.setUserId(note.getUser().getId());

            System.out.println("Успешно загружены OpenGraph данные: " + ogData.getTitle());

            return ogData;
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке OpenGraph данных: " + e.getMessage());
            return null; // Возвращаем null при ошибке
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

    // Вспомогательный метод для извлечения содержимого метатегов
    private String getMetaTagContent(Document doc, String property) {
        Element metaTag = doc.select("meta[property=" + property + "]").first();
        return metaTag != null ? metaTag.attr("content") : "";
    }

    // Метод для проверки корректности URL
    private boolean isValidUrl(String url) {
        try {
            new URL(url); // Проверяем, можно ли создать объект URL
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (MalformedURLException e) {
            return false; // Если URL некорректен
        }
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
//                System.out.println("audio URL: " + audio.getUrl());

            });

            note.getFiles().forEach(file -> {
//                file.setUrl(generateFullFileUrl(request, file.getUrl()));
                file.setUrl("/api/notes/download/file/" + file.getFileName()); // Генерация ссылки для скачивания
//                System.out.println("file URL: " + file.getUrl());
            });
        }
        return foundedNotes;
    }

    public List<OpenGraphData> getOpenGraphDataForNote(UUID noteId) {

          return openGraphDataRepository.findByNoteId(noteId);
    }

    @Transactional
    public synchronized Note addFilesToNote(UUID noteId, List<MultipartFile> files) {
        Note note = noteRepository.findById(noteId).orElseThrow(() -> new RuntimeException("Note not found"));

        if (files == null || files.isEmpty()) {
            System.out.println("Нет файлов для загрузки, удаляем все существующие файлы.");
            List<NoteFile> filesToRemove = new ArrayList<>(note.getFiles());
            note.getFiles().clear();
            noteFileRepository.deleteAll(filesToRemove); // Явно удаляем файлы из базы
            return noteRepository.save(note);
        }

        String publicPath = "/files/files/";

        // Получаем уже существующие файлы у заметки
        Set<String> newFileNames = files.stream()
                .map(file -> StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename())))
                .collect(Collectors.toSet());

        List<NoteFile> filesToRemove = note.getFiles().stream()
                .filter(existingFile -> !newFileNames.contains(existingFile.getFileName()))
                .collect(Collectors.toList());

        // Удаляем файлы, которых нет в переданном списке
        note.getFiles().removeAll(filesToRemove);
        noteFileRepository.deleteAll(filesToRemove); // Удаляем файлы из базы


        for (MultipartFile file : files) {
            try {
                Path uploadPath = Paths.get("E:/uploaded/uploaded-files/");
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
                String uniqueFileName = noteId + "_" + originalFileName;
                Path filePath = uploadPath.resolve(uniqueFileName);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                NoteFile newNoteFile = new NoteFile();
                newNoteFile.setFileName(originalFileName);
                newNoteFile.setFilePath(uploadPath + "/" + uniqueFileName);
                newNoteFile.setUrl(publicPath + uniqueFileName);
                newNoteFile.setNote(note);
                newNoteFile.setUserId(note.getUser().getId());
                newNoteFile.setCreatedAt(LocalDateTime.now());

                noteFileRepository.save(newNoteFile);
                note.getFiles().add(newNoteFile);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка при загрузке файла: " + e.getMessage(), e);
            }
        }
        note.setChangedAt(LocalDateTime.now());
        return noteRepository.save(note);
    }

    @Transactional
    public synchronized Note addAudiosToNote(UUID noteId, List<MultipartFile> audios) {
        Note note = noteRepository.findById(noteId).orElseThrow(() -> new RuntimeException("Note not found"));

        if (audios == null || audios.isEmpty()) {
            System.out.println("Нет аудиофайлов для загрузки, удаляем все существующие аудиофайлы.");
            List<NoteAudio> audiosToRemove = new ArrayList<>(note.getAudios());
            note.getAudios().clear();
            noteAudioRepository.deleteAll(audiosToRemove); // Удаляем аудиофайлы
            return noteRepository.save(note);
        }

        String publicPath = "/files/audio/";

        // Получаем уже существующие аудиофайлы у заметки
        Set<String> newAudioNames = audios.stream()
                .map(audio -> StringUtils.cleanPath(Objects.requireNonNull(audio.getOriginalFilename())))
                .collect(Collectors.toSet());

        // Удаляем аудиофайлы
        List<NoteAudio> audiosToRemove = note.getAudios().stream()
                .filter(existingAudio -> !newAudioNames.contains(existingAudio.getAudioFileName()))
                .collect(Collectors.toList());

        note.getAudios().removeAll(audiosToRemove);
        noteAudioRepository.deleteAll(audiosToRemove); // Удаляем аудиофайлы


        for (MultipartFile audio : audios) {
            try {
                Path uploadPath = Paths.get("E:/uploaded/uploaded-audio/");
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(audio.getOriginalFilename()));
                String uniqueFileName = noteId + "_" + originalFileName;
                Path filePath = uploadPath.resolve(uniqueFileName);
                Files.copy(audio.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                NoteAudio newNoteAudio = new NoteAudio();
                newNoteAudio.setAudioFileName(originalFileName);
                newNoteAudio.setAudioFilePath(uploadPath + "/" + uniqueFileName);
                newNoteAudio.setNote(note);
                newNoteAudio.setUserId(note.getUser().getId());
                newNoteAudio.setCreatedAt(LocalDateTime.now());

                noteAudioRepository.save(newNoteAudio);
                note.getAudios().add(newNoteAudio);

            } catch (IOException e) {
                throw new RuntimeException("Ошибка при загрузке аудиофайла: " + e.getMessage(), e);
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
    public String downloadFileFromBot(String fileUrl, String storagePath, String fileName) {
        System.out.println("Begin processing downloadFileFromBot");
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
        UUID userId = getCurrentUserId();
        List<Tag> tagList = new ArrayList<>();
        for (String tagName : tags) {
            Tag tag = tagService.findOrCreateTag(tagName, false);
            tagList.add(tag);
        }
        return tagList;
    }
    public List<Tag> getTagsByNameForBot(List<String> tags, UUID userId) {
        List<Tag> tagList = new ArrayList<>();
        for (String tagName : tags) {
            Tag tag = tagService.findOrCreateTagForBot(tagName, false, userId);
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
    // Сохранить новую заметку - старый метод все в одной транзакции
//    @Transactional
//    public Note saveMixedNote(NoteDTO noteDTO, UUID userId, List<String>links, List<NoteFile> files, List<NoteAudio> audios) {
//
//
//        Note note= noteConverter.toEntity(noteDTO);
//
////        UUID finalUserId = userId;
////        User user = userRepository.findById(userId)
////                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + finalUserId));
////
////        note.setUser(user);
//
//        if(note.getTitle()==null || note.getTitle().equals("")){
//            note.setTitle("note id: "+note.getId().toString());
//        }
//
//        // ✅ Если проект не указан, назначаем проект по умолчанию
//        if (note.getProject() == null) {
//            note.setProject(projectService.getDefaultBotProjectForUser(userId));
//        }
//        noteRepository.save(note);
//
//        System.out.println("📌 Сохраняем заметку: " + note.getId());
//        System.out.println("📌 Проект: " + (note.getProject() != null ? note.getProject().getId() : "NULL"));
//        System.out.println("📌 Файлы до сохранения заметки: " + note.getFiles().size());
//
//        if (!links.isEmpty()) {
//            final Note localNote = note;
//            List<OpenGraphData> openGraphData = links.stream()
//                    .map(link -> fetchOpenGraphData(link, localNote)) // Метод возвращает OpenGraphData
//                    .filter(Objects::nonNull) // Фильтруем null-значения
//                    .collect(Collectors.toList()); // Используем collect вместо .toList(), если проект использует Java 8-16
//
//            note.setOpenGraphData(openGraphData);
//        }
//
//        StringBuilder fileContent= new StringBuilder(" files: ");
////         ✅ Устанавливаем note_id в файлах, так как теперь у note есть ID
//        if (!files.isEmpty()) {
//            for (NoteFile file : note.getFiles()) {
//                fileContent.append(" ").append(file.getFileName());
//                file.setId(UUID.randomUUID());
//                file.setNote(note);
//
//            }
//            note.setFiles(files);
//            note.setContent(note.getContent()+ fileContent.toString());
//            noteFileRepository.saveAll(note.getFiles()); // Сохраняем файлы
//        }
//
//        StringBuilder audioContent= new StringBuilder(" audios: ");
//        // ✅ Аналогично для аудиофайлов
//        if (!note.getAudios().isEmpty()) {
//            for (NoteAudio audio : note.getAudios()) {
//                audioContent.append(" ").append(audio.getAudioFileName());
//                audio.setId(UUID.randomUUID());
//                audio.setNote(note);
//            }
//            note.setAudios(audios);
//            note.setContent(note.getContent()+ audioContent.toString());
//            noteAudioRepository.saveAll(note.getAudios());
//
//        }
//
//        note = noteRepository.save(note);
//
//        System.out.println("✅ Заметка сохранена, вложений: " + note.getFiles().size());
////        noteRepository.save(note);
//        return note;
//    }
}
