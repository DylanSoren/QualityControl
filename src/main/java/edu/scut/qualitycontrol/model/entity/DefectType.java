package edu.scut.qualitycontrol.model.entity;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("缺陷类型") // @Node 的值对应 Neo4j 中的标签 (Label)
public class DefectType {

    @Id
    @GeneratedValue // 标记为主键，并由数据库自动生成
    private Long id;

    @Property("name") // @Property 的值对应数据库中的属性名
    private String name;

    @Property("typical_manifestations")
    private String typicalManifestations;

    // --- 构造函数, Getters 和 Setters ---
    public DefectType(String name, String typicalManifestations) {
        this.name = name;
        this.typicalManifestations = typicalManifestations;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypicalManifestations() {
        return typicalManifestations;
    }

    public void setTypicalManifestations(String typicalManifestations) {
        this.typicalManifestations = typicalManifestations;
    }
}