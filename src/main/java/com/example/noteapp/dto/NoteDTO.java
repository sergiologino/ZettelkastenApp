package com.example.noteapp.dto;



public class NoteDTO {

    private String content;

    private String url;

    private String audioFilePath;

    private String recognizedText;

    private String annotation;

    private boolean aiSummary;

    public NoteDTO() {}


    public NoteDTO(String content, String url, String audioFilePath, String recognizedText, String annotation, boolean aiSummary) {
        this.content = content;
        this.url = url;
        this.audioFilePath = audioFilePath;
        this.recognizedText = recognizedText;
        this.annotation = annotation;
        this.aiSummary = aiSummary;
    }

    public String getContent() {
        return content;
    }

    public String getUrl() {
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

    public void setContent(String content) {
        this.content = content;
    }

    public void setUrl(String url) {
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
}
