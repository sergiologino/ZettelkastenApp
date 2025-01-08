package com.example.noteapp.repository;

import com.example.noteapp.model.OpenGraphData;
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

//    @Query("SELECT og FROM OpenGraphData og WHERE og.note.id = :noteId AND og.url IN :urls")
//    List<OpenGraphData> findByNoteIdAndUrls(@Param("noteId") UUID noteId, @Param("urls") List<String> urls);

    @Query("SELECT og FROM OpenGraphData og WHERE og.note.id = :noteId")
    List<OpenGraphData> findByNoteId(@Param("noteId") UUID noteId);

    @Query("SELECT og FROM OpenGraphData og WHERE og.url = :url AND og.note.id = :noteId")
    Optional<OpenGraphData> findByUrlAndNoteId(@Param("url") String url, @Param("noteId") UUID noteId);


}
