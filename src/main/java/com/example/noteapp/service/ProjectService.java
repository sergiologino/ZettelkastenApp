package com.example.noteapp.service;

import com.example.noteapp.dto.ProjectDTO;
import com.example.noteapp.model.Project;
import com.example.noteapp.repository.ProjectRepository;
import com.example.noteapp.repository.UserRepository;
import com.example.noteapp.utils.SecurityUtils;
import com.nimbusds.jose.crypto.opts.UserAuthenticationRequired;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;



@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;



    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public UUID getCurrentUserId() {

        return userRepository.findByUsername(SecurityUtils.getCurrentUserId()).getId();
    }

    public List<Project> getAllProjects() {
        UUID userId = getCurrentUserId();
        System.out.println("Текущий пользователь: "+userId);
        List<Project> foundedProjects = new ArrayList<>();
        foundedProjects=projectRepository.findAllByUserId(userId);
        System.out.println("Нашлись проекты: "+foundedProjects);
        return foundedProjects;
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
        String currentUser = SecurityUtils.getCurrentUserId();
        if (currentUser == null) {
            throw new SecurityException("Не удалось определить текущего пользователя.");
        }
        UUID userId = userRepository.findByUsername(SecurityUtils.getCurrentUserId()).getId();
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
        dto.setDefault(project.isDefault());
        dto.setCreatedAt(project.getCreatedAt());
        // Убираем связанные заметки для облегчения ответа
        dto.setNotes(Collections.emptyList());
        dto.setPosition(project.getPosition());
        return dto;
    }
}
