package com.example.noteapp.dto;


import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NoteDTO {


    private UUID id;

    private String content;

    private List<String> url;

    private String audioFilePath;

    private String recognizedText;

    private String annotation;

    private boolean aiSummary;

    private UUID projectId;

    private List<String> tags;

    private String filePath;

    private String fileType;

    private boolean analyze;

    private String neuralNetwork;

    private Long x;

    private Long y;




    public static class OpenGraphData {
        private String title;
        private String description;
        private String image;
        private String url;

        // Геттеры и сеттеры
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }



    public NoteDTO() {}

    public NoteDTO(String content, List<String> url, String audioFilePath, String recognizedText, String annotation, boolean aiSummary, UUID projectId, List<String> tags, String filePath, String fileType, boolean analyze, String neuralNetwork, Long x, Long y) {
        this.id = UUID.randomUUID();
        this.content = content;
        this.url = url;
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
        this.x = x;
        this.y = y;
    }

    public Map<String, OpenGraphData> getOpenGraphData() { return openGraphData; }

    public void setOpenGraphData(Map<String, OpenGraphData> openGraphData) { this.openGraphData = openGraphData; }

    private Map<String, OpenGraphData> openGraphData; // Данные Open Graph

    public List<String> getUrls() {
        return url;

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
        return url;
    }

    public String getAudioFilePath() {
        return audioFilePath;
    }

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
        this.url = url;
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
