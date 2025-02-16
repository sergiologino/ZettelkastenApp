package com.example.noteapp.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NoteFileRepository extends JpaRepository<NoteFile, UUID> {
}