package com.example.noteapp.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ProjectDTO {
    private UUID id;
    private String name;
    private String description;
    private String color;

    private List<NoteDTO> notes;

    // Конструкторы, геттеры и сеттеры
}
