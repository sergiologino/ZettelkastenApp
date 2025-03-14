package ru.altacod.noteapp.repository;

import ru.altacod.noteapp.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    // Получение всех проектов пользователя
    @Query("SELECT p FROM Project p WHERE p.userId = :userId")
    List<Project> findAllByUserId(@Param("userId") UUID userId);

    // Получение проекта по ID и userId
    @Query("SELECT p FROM Project p WHERE p.id = :projectId AND p.userId = :userId")
    Optional<Project> findByIdAndUserId(@Param("projectId") UUID projectId, @Param("userId") UUID userId);

    // Удаление проекта по ID и userId
    @Query("DELETE FROM Project p WHERE p.id = :projectId AND p.userId = :userId")
    void deleteByIdAndUserId(@Param("projectId") UUID projectId, @Param("userId") UUID userId);

    @Query("SELECT p FROM Project p WHERE p.userId = :userId AND p.isDefault = true")
    Optional<Project> findDefaultProjectByUserId(@Param("userId") UUID userId);
}
