package edu.scut.qualitycontrol.model.dto;

public class LinkDto {
    private String source;
    private String target;

    public LinkDto(String source, String target) {
        this.source = source;
        this.target = target;
    }

    // Getters and Setters
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
}