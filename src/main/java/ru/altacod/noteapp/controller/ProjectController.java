package ru.altacod.noteapp.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import ru.altacod.noteapp.dto.ProjectDTO;
import ru.altacod.noteapp.mapper.NoteConverter;
import ru.altacod.noteapp.model.Project;
import ru.altacod.noteapp.repository.UserRepository;
import ru.altacod.noteapp.service.NoteService;
import ru.altacod.noteapp.service.ProjectService;
import ru.altacod.noteapp.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;



@RestController
@RequestMapping(value = "/api/projects", produces = "application/json")
// @CrossOrigin(origins = {
//         "http://localhost:3000",
//         "https://sergiologino-note-app-new-design-eaa6.twc1.net",
//         "https://altanote.ru"
// })
public class ProjectController {

    @PostConstruct
    public void init() {
        System.out.println("✅ ProjectController зарегистрирован в Spring");
    }

    private final ProjectService projectService;
    private final UserRepository userRepository;

    public ProjectController(ProjectService projectService, NoteService noteService, NoteConverter noteConverter, UserRepository userRepository) {
        this.projectService = projectService;
        this.userRepository = userRepository;
    }

    @ApiResponse(responseCode = "200", description = "Список проектов успешно возвращен",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectDTO.class)))
    @GetMapping
    public List<ProjectDTO> getAllProjects() {
        List<Project> allProjects = projectService.getAllProjects();
        return allProjects.stream()
                .map(projectService::convertToDto)
                .collect(Collectors.toList());

    }

    @Operation(summary = "Получить проект по ID", description = "Возвращает проект по указанному идентификатору.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Проект успешно возвращен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Project.class))),
            @ApiResponse(responseCode = "404", description = "Проект не найден"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    @GetMapping("/{id}")
    public Project getProjectById(@PathVariable UUID id) {
        UUID userId = userRepository.findByUsername(SecurityUtils.getCurrentUserId()).getId();
        Project responseProject = projectService.getProjectById(id, userId);
        return responseProject;
    }



    @Operation(summary = "Создать новый проект", description = "Создает новый проект с указанным названием и описанием.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Проект успешно создан",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Project.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody Project project) {

        System.out.println(" Вызван createProject с данными: " + project.getName());
        Project createdProject = projectService.saveProject(project);
        return ResponseEntity.ok(createdProject);
    }

    @Operation(summary = "Обновить проект", description = "Обновляет данные существующего проекта.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Проект успешно обновлен"),
            @ApiResponse(responseCode = "404", description = "Проект не найден"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(@PathVariable UUID id, @RequestBody ProjectDTO projectDTO) {
        try {
            projectService.updateProject(id, projectDTO);
            return ResponseEntity.ok("Проект успешно обновлён.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при обновлении проекта: " + e.getMessage());
        }
    }

    @Operation(summary = "Удалить проект", description = "Удаляет проект по указанному идентификатору.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Проект успешно удален"),
            @ApiResponse(responseCode = "404", description = "Проект не найден"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteProjectById(@PathVariable UUID id) {
        projectService.deleteProjectById(id);
        UUID userId = userRepository.findByUsername(SecurityUtils.getCurrentUserId()).getId();
        Project project = projectService.getProjectById(id, userId);
             //   .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Проект не найден"));

        // Удаляем связанные заметки TODO реализовать эндпойнт
        //noteService.deleteNote(project.getNotes());

        // Удаляем проект
        projectService.deleteProjectById(project.getId());

        return ResponseEntity.noContent().build();
    }
    // получение проектов пользака для бота телеги
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ProjectDTO>> getUserProjects(@PathVariable UUID userId) {
        List<Project> projects = projectService.getAllProjectsForUser(userId);
        List<ProjectDTO> projectDTOs = projects.stream().map(projectService::convertToDto).collect(Collectors.toList());
        return ResponseEntity.ok(projectDTOs);
    }

}
