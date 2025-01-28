package com.example.noteapp.repository;

import com.example.noteapp.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("SELECT n FROM Project n WHERE n.userId = :userId")
    List<Project> findAllByUserId(@Param("userId") UUID currentUserId);

    @Query("SELECT n FROM Project n WHERE n.userId = :userId AND n.id =:id")
    Optional<Project> findByIdAndUserId(@Param("userId") UUID currentUserId, @Param("id") UUID id);

    @Query()
    Project saveProjectWithUserId(Project project, UUID currentUserId);
}

