package com.example.noteapp.controller;

import com.example.noteapp.dto.NoteDTO;
import com.example.noteapp.dto.ProjectDTO;
import com.example.noteapp.mapper.NoteConverter;
import com.example.noteapp.model.Note;
import com.example.noteapp.model.Project;
import com.example.noteapp.service.NoteService;
import com.example.noteapp.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final NoteService noteService;
    private final NoteConverter noteConverter;

    public ProjectController(ProjectService projectService, NoteService noteService, NoteConverter noteConverter) {
        this.projectService = projectService;
        this.noteService = noteService;
        this.noteConverter = noteConverter;
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
        Project responseProject = projectService.getProjectById(id);
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
    public Project createProject(@RequestBody Project project) {
        return projectService.saveProject(project);
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
        Project project = projectService.getProjectById(id);
             //   .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Проект не найден"));

        // Удаляем связанные заметки TODO реализовать эндпойнт
        //noteService.deleteNote(project.getNotes());

        // Удаляем проект
        projectService.deleteProjectById(project.getId());

        return ResponseEntity.noContent().build();
    }

}
