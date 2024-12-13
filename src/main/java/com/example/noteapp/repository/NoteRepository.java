package com.example.noteapp.repository;

import com.example.noteapp.model.Note;
import com.example.noteapp.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NoteRepository extends JpaRepository<Note, UUID> {
    List<Note> findAllByProjectId(UUID projectId);

    @Query("SELECT n.tags FROM Note n WHERE n.id = :noteId")
    List<Tag> findTagsByNoteId(@Param("noteId") UUID noteId);


}
