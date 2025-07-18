package ru.altacod.noteapp.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import ru.altacod.noteapp.model.OpenGraphData;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
public class NoteDTO {


    private UUID id;

    private String title;

    private String content;

    @JsonProperty("urls")
    private List<String> urls = new ArrayList<>();;

    private String audioFilePath;

    private String recognizedText;

    private String annotation;

    private boolean aiSummary;

    private UUID projectId;

    private List<String> tags= new ArrayList<>();;

    private String filePath;

    private String fileType;

    private boolean analyze;

    private String neuralNetwork;

    private Long x;

    private Long y;

    private Integer width;

    private Integer height;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime changedAt;

    private List<NoteFileDTO> files = new ArrayList<>();;

    private List<NoteAudioDTO> audios = new ArrayList<>();;

    private UUID userId;

    private List<String> matches;

    private String projectName;

    private String projectColor;

    private String formattedDate;

    private Map<String, OpenGraphData> openGraphData = new HashMap<>();; // Данные Open Graph

    public NoteDTO() {}

    public NoteDTO(String content, List<String> url, String audioFilePath, String recognizedText, String annotation, boolean aiSummary, UUID projectId, List<String> tags, String filePath, String fileType, boolean analyze, String neuralNetwork, Long x, Long y, Integer width, Integer height, List<NoteFileDTO> files, List<NoteAudioDTO> audios, List<String> urls,  Map<String, OpenGraphData> openGraphData) {
        this.id = UUID.randomUUID();
        this.content = content;
        this.urls = urls;
        this.audioFilePath = audioFilePath;
        this.recognizedText = recognizedText;
        this.annotation = annotation;
        this.aiSummary = aiSummary;
        this.projectId = projectId;
        this.tags = tags;
        this.filePath = filePath;
        this.fileType = fileType;
        this.analyze = analyze;
        this.neuralNetwork = neuralNetwork;
        this.files = new ArrayList<>();
        this.audios = new ArrayList<>();
        this.openGraphData = new HashMap<>();
        this.x = null;
        this.y = null;
        this.width = null;
        this.height = null;
        this.files = null;
        this.audios = null;
        this.openGraphData = null;

    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTitle() {return title;    }

    public void setTitle(String title) {this.title = title;}

    public Integer getWidth() {return width;    }

    public void setWidth(Integer width) {this.width = width;    }

    public Integer getHeight() {return height;    }

    public void setHeight(Integer height) {this.height = height;    }

    public LocalDateTime getCreatedAt() {return createdAt;    }

    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;    }

    public LocalDateTime getChangedAt() {return changedAt;}

    public void setChangedAt(LocalDateTime changedAt) {this.changedAt = changedAt;}

    public List<NoteFileDTO> getFiles() {
        return files;
    }

    public void setFiles(List<NoteFileDTO> files) {
        this.files = files;
    }

    public List<NoteAudioDTO> getAudios() {
        return audios;
    }

    public void setAudios(List<NoteAudioDTO> audios) {
        this.audios = audios;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public Map<String, OpenGraphData> getOpenGraphData() { return openGraphData; }

    public void setOpenGraphData(Map<String, OpenGraphData> openGraphData) { this.openGraphData = openGraphData; }



    public List<String> getUrls() {
        return urls;

    }


    public boolean isAnalyze() { return analyze; }

    public Long getX() {return x;}

    public Long getY() {return y;}

    public String getNeuralNetwork() { return neuralNetwork; }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getContent() {
        return content;
    }

    public List<String> getUrl() {
        return urls;
    }

    public String getAudioFilePath() { return audioFilePath; }

    public String getRecognizedText() {
        return recognizedText;
    }

    public String getAnnotation() {
        return annotation;
    }

    public boolean isAiSummary() {
        return aiSummary;
    }

    public String getFileType() { return fileType; }

    public String getFilePath() { return filePath; }



    public void setX(Long x) { this.x = x; }

    public void setY(Long y) { this.y = y; }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setUrl(List<String> url) {
        this.urls = url;
    }

    public void setAudioFilePath(String audioFilePath) {
        this.audioFilePath = audioFilePath;
    }

    public void setRecognizedText(String recognizedText) {
        this.recognizedText = recognizedText;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public void setAiSummary(boolean aiSummary) {
        this.aiSummary = aiSummary;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setFilePath(String filePath) { this.filePath = filePath; }

    public void setFileType(String fileType) { this.fileType = fileType; }

    public void setAnalyze(boolean analyze) { this.analyze = analyze; }

    public void setNeuralNetwork(String neuralNetwork) {
        this.neuralNetwork = neuralNetwork;
    }
}
