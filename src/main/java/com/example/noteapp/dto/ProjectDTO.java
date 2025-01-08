package com.example.noteapp.dto;


import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;

    private List<NoteDTO> notes;

    // Конструкторы, геттеры и сеттеры
}
