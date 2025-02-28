package com.example.noteapp.repository;

import com.example.noteapp.model.NoteAudio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NoteAudioRepository extends JpaRepository<NoteAudio, UUID> {
}
