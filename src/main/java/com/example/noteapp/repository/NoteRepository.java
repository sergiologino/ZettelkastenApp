package com.example.noteapp.repository;

import com.example.noteapp.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NoteRepository extends JpaRepository<Note, UUID> {
    List<Note> findAllByProjectId(UUID projectId);
}
