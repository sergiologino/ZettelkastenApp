package com.example.noteapp.service;

import com.example.noteapp.model.Project;
import com.example.noteapp.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Project getProjectById(UUID id) {
        return projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Project not found"));
    }
    @Transactional
    public Project saveProject(Project project) {
        return projectRepository.save(project);
    }

    public void deleteProjectById(UUID id) {
        projectRepository.deleteById(id);
    }
}
