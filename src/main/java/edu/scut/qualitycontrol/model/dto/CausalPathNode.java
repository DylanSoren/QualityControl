package edu.scut.qualitycontrol.model.dto;

import edu.scut.qualitycontrol.model.entity.InfluencingFactor;

// 一条路径上的影响因素节点
public class CausalPathNode {
    private Long pathId;
    private InfluencingFactor factor;

    // Getters and Setters
    public Long getPathId() {
        return pathId;
    }

    public void setPathId(Long pathId) {
        this.pathId = pathId;
    }

    public InfluencingFactor getFactor() {
        return factor;
    }

    public void setFactor(InfluencingFactor factor) {
        this.factor = factor;
    }
}