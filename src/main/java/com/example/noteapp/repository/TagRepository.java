package com.example.noteapp.repository;

import com.example.noteapp.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByName(String name);

    // Поиск всех тегов пользователя
    @Query("SELECT t FROM Tag t WHERE t.userId = :userId")
    List<Tag> findAllByUserId(@Param("userId") UUID userId);

    // Поиск тега по имени и userId
    @Query("SELECT t FROM Tag t WHERE t.name = :name AND t.userId = :userId")
    Optional<Tag> findByNameAndUserId(@Param("name") String name, @Param("userId") UUID userId);

    // Поиск тега по ID и userId
    @Query("SELECT t FROM Tag t WHERE t.id = :id AND t.userId = :userId")
    Optional<Tag> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}