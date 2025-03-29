package ru.altacod.noteapp.repository;

import ru.altacod.noteapp.model.OpenGraphData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OpenGraphDataRepository extends JpaRepository<OpenGraphData, UUID> {

    @Query("SELECT og.url FROM OpenGraphData og WHERE og.note.id = :noteId")
    List<String> findUrlsByNoteId(@Param("noteId") UUID noteId);

    @Query("SELECT og FROM OpenGraphData og WHERE og.url = :url")
    Optional<OpenGraphData> findByUrl(@Param("url") String url);

    @Query("SELECT og FROM OpenGraphData og WHERE og.note.id = :noteId AND og.url IN :urls")
    List<OpenGraphData> findByNoteIdAndUrls(@Param("noteId") UUID noteId, @Param("urls") List<String> urls);

    @Query("SELECT og FROM OpenGraphData og WHERE og.note.id = :noteId")
    List<OpenGraphData> findByNoteId(@Param("noteId") UUID noteId);

    @Query("SELECT og FROM OpenGraphData og WHERE og.url = :url AND og.note.id = :noteId")
    Optional<OpenGraphData> findByUrlAndNoteId(@Param("url") String url, @Param("noteId") UUID noteId);

    // Проверка наличия данных OpenGraph для заметки
    boolean existsByNoteIdAndUrl(UUID noteId, String url);

    // Получение всех URL OpenGraph для заметки
    @Query("SELECT og.url FROM OpenGraphData og WHERE og.note.id = :noteId AND og.note.user.id = :userId")
    List<String> findUrlsByNoteIdAndUserId(@Param("noteId") UUID noteId, @Param("userId") UUID userId);

    // Поиск данных OpenGraph по URL
    @Query("SELECT og FROM OpenGraphData og WHERE og.url = :url AND og.note.user.id = :userId")
    Optional<OpenGraphData> findByUrlAndUserId(@Param("url") String url, @Param("userId") UUID userId);

    // Поиск данных OpenGraph для заметки по URL
    @Query("SELECT og FROM OpenGraphData og WHERE og.note.id = :noteId AND og.url IN :urls AND og.note.user.id = :userId")
    List<OpenGraphData> findByNoteIdAndUrlsAndUserId(@Param("noteId") UUID noteId, @Param("urls") List<String> urls, @Param("userId") UUID userId);
}
