package com.example.noteapp.service;

import com.example.noteapp.dto.ProjectDTO;
import com.example.noteapp.model.Project;
import com.example.noteapp.repository.ProjectRepository;
import com.example.noteapp.utils.SecurityUtils;
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
        return projectRepository.findAllByUserId(getCurrentUserId());
    }

    public Project getProjectById(UUID id) {
        return projectRepository.findByIdAndUserId(id, getCurrentUserId()).orElseThrow(() -> new RuntimeException("Project not found"));
    }
    @Transactional
    public Project saveProject(Project project) {
        UUID userId = SecurityUtils.getCurrentUserId();
        project.setUserId(userId);
        return projectRepository.save(project);
    }

    public void deleteProjectById(UUID id) {
        projectRepository.deleteById(id);
    }

    public ProjectDTO convertToDto(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setColor(project.getColor());
        // Удаляем связанные заметки для простого ответа
        dto.setNotes(Collections.emptyList());
        return dto;
    }
}
