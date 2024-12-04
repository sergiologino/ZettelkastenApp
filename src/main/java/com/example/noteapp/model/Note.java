package com.example.noteapp.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "notes")
public class Note {



    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String content;

    @Column(nullable = true)
    private String url;

    @Column(nullable = true)
    private String audioFilePath;

    @Column(nullable = true)
    private String recognizedText;

    @Column(nullable = true)
    private String annotation;

    @Column(nullable = true)
    private boolean aiSummary;

    @ManyToOne
    @JoinColumn(name = "project_id")
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



    public Note() {
    }


    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }



    // Геттеры и сеттеры
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

}
