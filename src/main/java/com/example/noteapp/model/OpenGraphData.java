package com.example.noteapp.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "open_graph_data", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"note_id", "url"})})
public class OpenGraphData {

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String url;

    @Setter
    private String title;
    @Setter
    private String description;
    @Setter
    private String image;

    @Setter
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    @JsonBackReference
    private Note note;

    // Геттеры и сеттеры
    public UUID getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(Object url) {
        this.url = url.toString();
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImage() {
        return image;
    }

    public Note getNote() {
        return note;
    }

}