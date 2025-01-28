package com.example.noteapp.repository;

import com.example.noteapp.model.Note;
import com.example.noteapp.model.NoteAudio;
import com.example.noteapp.model.NoteFile;
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


    @Query("SELECT n FROM Note n JOIN n.tags t WHERE t.name IN :tagNames GROUP BY n HAVING COUNT(t.id) = :tagCount")
    List<Note> findAllByTags(@Param("tagNames") List<String> tagNames, @Param("tagCount") long tagCount);

    @Query("SELECT DISTINCT t.name FROM Tag t")
    List<String> findAllUniqueTags();

    @Query("SELECT n FROM Note n LEFT JOIN FETCH n.files LEFT JOIN FETCH n.audios WHERE n.project.id = :projectId")
    List<Note> findAllByProjectIdWithFilesAndAudios(@Param("projectId") UUID projectId);

    @Query("SELECT a FROM NoteAudio a WHERE a.note.id = :noteId")
    List<NoteAudio> findAudiosByNoteId(@Param("noteId") UUID noteId);

    @Query("SELECT f FROM NoteFile f WHERE f.note.id = :noteId")
    List<NoteFile> findFilesByNoteId(@Param("noteId") UUID noteId);

    @Query("SELECT DISTINCT n FROM Note n JOIN n.tags t WHERE t.name IN :tagNames")
    List<Note> findNotesByTags(@Param("tagNames") List<String> tagNames);

    @Query("SELECT n FROM Note n JOIN n.user u WHERE u.id = :userId")
    List<Note> findAllbyUserId(@Param("userId") UUID currentUserId);
}
