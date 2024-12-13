package com.example.noteapp.dto;

import java.util.List;
import java.util.UUID;

public class ProjectDTO {
    private UUID id;
    private String name;
    private String description;
    private String color;

    private List<NoteDTO> notes;

    // Конструкторы, геттеры и сеттеры
}
