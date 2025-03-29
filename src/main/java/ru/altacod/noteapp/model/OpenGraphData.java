package ru.altacod.noteapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "open_graph_data", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"note_id", "url"})})
public class OpenGraphData {

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name="url", nullable = false)
    private String url;

    @Setter
    private String title;
    @Setter
    private String description;
    @Setter
    private String image;

    @Column(name="created_at",nullable = true)
    private LocalDateTime createdAt;

    @Column(name = "user_id", nullable = false)
    private UUID userId;


    @Setter
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    // Геттеры и сеттеры


    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {return createdAt;}

    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}

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