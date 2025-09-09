package edu.scut.qualitycontrol.model.entity;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Node("影响因素")
public class InfluencingFactor {

    @Id @GeneratedValue
    private Long id;

    @Property("name")
    private String name;

    @Property("standard")
    private String standard;

    @Property("description")
    private String description;

    // @Relationship 定义了关系。type 是关系类型，direction 是关系方向。
    @Relationship(type = "导致", direction = Relationship.Direction.OUTGOING)
    private Set<InfluencingFactor> leadsToFactor = new HashSet<>();

    @Relationship(type = "导致", direction = Relationship.Direction.OUTGOING)
    private Set<DefectType> leadsToDefect = new HashSet<>();
    
    // --- 构造函数, Getters 和 Setters ---
    public InfluencingFactor(String name, String standard, String description) {
        this.name = name;
        this.standard = standard;
        this.description = description;
    }

    // Getters and Setters...
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStandard() { return standard; }
    public void setStandard(String standard) { this.standard = standard; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Set<InfluencingFactor> getLeadsToFactor() { return leadsToFactor; }
    public Set<DefectType> getLeadsToDefect() { return leadsToDefect; }
}