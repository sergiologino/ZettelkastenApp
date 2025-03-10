package ru.altacod.noteapp.repository;


import ru.altacod.noteapp.model.NoteFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NoteFileRepository extends JpaRepository<NoteFile, UUID> {
}
