package com.example.noteapp.controller;

import com.example.noteapp.model.Project;
import com.example.noteapp.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(summary = "Получить список всех проектов", description = "Возвращает список всех проектов.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список проектов успешно возвращен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Project.class))),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    @GetMapping
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
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
        return projectService.getProjectById(id);
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
    public void deleteProjectById(@PathVariable UUID id) {
        projectService.deleteProjectById(id);
    }
}
