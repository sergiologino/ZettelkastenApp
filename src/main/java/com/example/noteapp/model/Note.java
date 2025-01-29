package com.example.noteapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name="title", nullable = true)
    private String title;

    @NotNull(message = "Текст заметки обязателен.")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = true)
    private String audioFilePath;

    @Column(nullable = true)
    private String recognizedText;

    @Column(nullable = true)
    private String annotation;

    @Column(nullable = true)
    private boolean aiSummary;

    @JsonIgnore
    @NotNull(message = "Проект обязателен.")
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "note_tags",
            joinColumns = @JoinColumn(name = "note_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags=new ArrayList<>();

    @Column(nullable = true)
    private String filePath; // Путь к загруженному файлу

    @Column(nullable = true)
    private String fileType; // Тип файла (image, pdf, doc, xls, txt, csv)

    @Column(nullable = true)
    private String neuralNetwork; // Нейросеть, используемая для анализа


    @Column(name = "should_analyze", nullable = false)
    private boolean analyze = false; // Новый флаг для анализа

    @Column(name="position_x", nullable = true)
    private Long positionX;

    @Column(name="position_y", nullable = true)
    private Long positionY;

    @Column(name="width", nullable = true)
    private Integer width;

    @Column(name = "height", nullable = true)
    private Integer height;

    @Column(name="created_at", nullable = true)
    private LocalDateTime createdAt;

    @Column(name="changed_at", nullable = true)
    private LocalDateTime changedAt;

    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<OpenGraphData> openGraphData = new ArrayList<>();;

    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference // Указывает, что это основная связь
    private List<NoteFile> files = new ArrayList<>();

    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference // Указывает, что это основная связь
    private List<NoteAudio> audios = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;



    // constructors
    public Note() {
    }

    //Setters and Getters

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTitle() {return title;}

    public void setTitle(String title) {this.title = title;}

    public LocalDateTime getCreatedAt() {return createdAt;    }

    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;    }

    public LocalDateTime getChangedAt() {return changedAt;    }

    public void setChangedAt(LocalDateTime changedAt) {this.changedAt = changedAt;    }

    public Integer getWidth() {return width;}

    public Integer getHeight() {return height;}

    public void setWidth(Integer width) {
        this.width = width;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public List<OpenGraphData> getOpenGraphData() {
        return openGraphData;
    }

    public void setOpenGraphData(List<OpenGraphData> openGraphData) {
        this.openGraphData = openGraphData;
    }

    public boolean isAnalyze() { return analyze; }

    public void setAnalyze(boolean analyze) { this.analyze = analyze; }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public Long getPositionX() { return positionX; }

    public Long getPositionY() { return positionY; }

    public void setPositionX(Long positionX) { this.positionX = positionX; }

    public void setPositionY(Long positionY) { this.positionY = positionY; }


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAudioFilePath() {
        return audioFilePath;
    }

    public void setAudioFilePath(String audioFilePath) {
        this.audioFilePath = audioFilePath;
    }

    public String getRecognizedText() {
        return recognizedText;
    }

    public void setRecognizedText(String recognizedText) {
        this.recognizedText = recognizedText;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public boolean isAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(boolean aiSummary) {
        this.aiSummary = aiSummary;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getNeuralNetwork() {
        return neuralNetwork;
    }

    public void setNeuralNetwork(String neuralNetwork) {
        this.neuralNetwork = neuralNetwork;
    }

    public List<NoteFile> getFiles() {
        return files;
    }

    public void setFiles(List<NoteFile> files) {
        this.files = files;
    }

    public List<NoteAudio> getAudios() {
        return audios;
    }

    public void setAudios(List<NoteAudio> audios) {
        this.audios = audios;
    }
}
