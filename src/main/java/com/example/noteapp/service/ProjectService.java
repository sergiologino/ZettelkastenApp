package com.example.noteapp.service;

import com.example.noteapp.dto.ProjectDTO;
import com.example.noteapp.model.Project;
import com.example.noteapp.repository.ProjectRepository;
import com.example.noteapp.utils.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public UUID getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    public List<Project> getAllProjects() {
        UUID userId = getCurrentUserId();
        return projectRepository.findAllByUserId(userId);
    }

    // Получение проекта по ID
    public Project getProjectById(UUID id) {
        UUID userId = getCurrentUserId();
        return projectRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found or access denied."));
    }

    public Project getDefaultProjectForUser(UUID userId) {

        return projectRepository.findDefaultProjectByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Default project not found for user."));
    }

    @Transactional
    public Project saveProject(Project project) {
        UUID userId = SecurityUtils.getCurrentUserId();
        project.setUserId(userId);
        return projectRepository.save(project);
    }

    // Удаление проекта по ID
    @Transactional
    public void deleteProjectById(UUID id) {
        UUID userId = getCurrentUserId();
        projectRepository.deleteByIdAndUserId(id, userId);
    }

    // Преобразование объекта Project в DTO
    public ProjectDTO convertToDto(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setColor(project.getColor());
        // Убираем связанные заметки для облегчения ответа
        dto.setNotes(Collections.emptyList());
        return dto;
    }
}
