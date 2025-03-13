package ru.altacod.noteapp.repository;

import ru.altacod.noteapp.model.NoteAudio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NoteAudioRepository extends JpaRepository<NoteAudio, UUID> {
}
