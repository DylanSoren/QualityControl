package edu.scut.qualitycontrol.model.dto;

import java.util.List;

public class GraphDataDto {
    private List<?> nodes;
    private List<LinkDto> links;

    public GraphDataDto(List<?> nodes, List<LinkDto> links) {
        this.nodes = nodes;
        this.links = links;
    }

    // Getters and Setters
    public List<?> getNodes() { return nodes; }
    public void setNodes(List<?> nodes) { this.nodes = nodes; }
    public List<LinkDto> getLinks() { return links; }
    public void setLinks(List<LinkDto> links) { this.links = links; }
}