package ru.altacod.noteapp.repository;

import ru.altacod.noteapp.model.Note;
import ru.altacod.noteapp.model.NoteAudio;
import ru.altacod.noteapp.model.NoteFile;
import ru.altacod.noteapp.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoteRepository extends JpaRepository<Note, UUID> {
    List<Note> findAllByProjectId(UUID projectId);

    @Query("SELECT n.tags FROM Note n WHERE n.id = :noteId")
    List<Tag> findTagsByNoteId(@Param("noteId") UUID noteId);


    @Query("SELECT n FROM Note n JOIN n.tags t WHERE t.name IN :tagNames GROUP BY n HAVING COUNT(t.id) = :tagCount")
    List<Note> findAllByTags(@Param("tagNames") List<String> tagNames, @Param("tagCount") long tagCount);

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

    // Получение заметки по ID и userId
    @Query("SELECT n FROM Note n WHERE n.id = :noteId AND n.user.id = :userId")
    Optional<Note> findByIdAndUserId(@Param("noteId") UUID noteId, @Param("userId") UUID userId);

    // Удаление заметки по ID и userId
    @Modifying
    @Query("DELETE FROM Note n WHERE n.id = :noteId AND n.user.id = :userId")
    void deleteByIdAndUserId(@Param("noteId") UUID noteId, @Param("userId") UUID userId);

    // Получение всех заметок для определенного проекта с фильтрацией по userId
    @Query("SELECT n FROM Note n WHERE n.project.id = :projectId AND n.user.id = :userId")
    List<Note> findAllByProjectIdAndUserId(@Param("projectId") UUID projectId, @Param("userId") UUID userId);

    // Получение тегов заметки
    @Query("SELECT n.tags FROM Note n WHERE n.id = :noteId AND n.user.id = :userId")
    List<Tag> findTagsByNoteIdAndUserId(@Param("noteId") UUID noteId, @Param("userId") UUID userId);

    // Получение аудиофайлов заметки
    @Query("SELECT a FROM NoteAudio a WHERE a.note.id = :noteId AND a.note.user.id = :userId")
    List<NoteAudio> findAudiosByNoteIdAndUserId(@Param("noteId") UUID noteId, @Param("userId") UUID userId);

    // Получение файлов заметки
    @Query("SELECT f FROM NoteFile f WHERE f.note.id = :noteId AND f.note.user.id = :userId")
    List<NoteFile> findFilesByNoteIdAndUserId(@Param("noteId") UUID noteId, @Param("userId") UUID userId);

    // Получение заметок с определенными тегами (фильтрация по userId)
    @Query("SELECT DISTINCT n FROM Note n JOIN n.tags t WHERE t.name IN :tagNames AND n.user.id = :userId")
    List<Note> findNotesByTagsAndUserId(@Param("tagNames") List<String> tagNames, @Param("userId") UUID userId);

    // Получение всех уникальных тегов
    @Query("SELECT DISTINCT t.name FROM Tag t Where t.userId =:userId")
    List<String> findAllUniqueTags(@Param("userId") UUID userId);

    @Query(value = """
        SELECT DISTINCT n.*
        FROM notes n
        LEFT JOIN open_graph_data og ON og.note_id = n.id
        LEFT JOIN note_files nf ON nf.note_id = n.id
        LEFT JOIN note_audios na ON nf.note_id = n.id
        WHERE
            to_tsvector('russian', n.content) @@ plainto_tsquery(:query)
            OR LOWER(og.url) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(nf.original_name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(na.original_name) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY n.changed_at DESC
        LIMIT 50
    """, nativeQuery = true)
    List<Note> searchNotes(@Param("query") String query);
}

