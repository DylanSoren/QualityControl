package edu.scut.qualitycontrol.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.scut.qualitycontrol.model.entity.DefectType;
import edu.scut.qualitycontrol.model.entity.InfluencingFactor;
import edu.scut.qualitycontrol.repository.DefectTypeRepository;
import edu.scut.qualitycontrol.repository.InfluencingFactorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DatabaseInitializationService {

    // 将所有依赖通过构造函数注入
    private final Neo4jClient neo4jClient;
    private final InfluencingFactorRepository factorRepository;
    private final DefectTypeRepository defectRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public DatabaseInitializationService(Neo4jClient neo4jClient,
                                     InfluencingFactorRepository factorRepository,
                                     DefectTypeRepository defectRepository) {
        this.neo4jClient = neo4jClient;
        this.factorRepository = factorRepository;
        this.defectRepository = defectRepository;
    }

    public void initializeDatabase() throws Exception {
        // 1. 清理数据库
        log.info("--- 开始清理数据库 ---");
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
        log.info("--- 清理数据库完成 ---");

        // 2. 从 JSON 加载数据并初始化图谱
        log.info("--- 开始初始化知识图谱 ---");
        
        InputStream inputStream = new ClassPathResource("initialData.json").getInputStream();
        List<JsonNode> data = mapper.readValue(inputStream, new TypeReference<>() {});

        for (JsonNode item : data) {
            // ... (内部逻辑与之前完全相同) ...
            JsonNode startNodeJson = item.get("start_node");
            Map<String, String> startProps = mapper.convertValue(startNodeJson.get("properties"), new TypeReference<>() {});

            InfluencingFactor startNode = factorRepository.findByName(startProps.get("name"))
                    .orElseGet(() -> factorRepository.save(new InfluencingFactor(startProps.get("name"), null, null)));

            if (startProps.containsKey("standard")) {
                startNode.setStandard(startProps.get("standard"));
            }
            if (startProps.containsKey("description")) {
                startNode.setDescription(startProps.get("description"));
            }
            factorRepository.save(startNode);

            JsonNode endNodeJson = item.get("end_node");
            String endLabel = endNodeJson.get("label").asText();
            Map<String, String> endProps = mapper.convertValue(endNodeJson.get("properties"), new TypeReference<>() {});
            String endNodeName = endProps.get("name");

            if ("影响因素".equals(endLabel)) {
                InfluencingFactor endNode = factorRepository.findByName(endNodeName)
                        .orElseGet(() -> factorRepository.save(new InfluencingFactor(endNodeName, null, null)));
                startNode.getLeadsToFactor().add(endNode);
            } else {
                DefectType endNode = defectRepository.findByName(endNodeName)
                        .orElseGet(() -> {
                            String manifestations = endProps.get("typical_manifestations");
                            return defectRepository.save(new DefectType(endNodeName, manifestations));
                        });
                if (endProps.containsKey("typical_manifestations") && endNode.getTypicalManifestations() == null) {
                    endNode.setTypicalManifestations(endProps.get("typical_manifestations"));
                    defectRepository.save(endNode);
                }
                startNode.getLeadsToDefect().add(endNode);
            }
            factorRepository.save(startNode);

            log.info("已连接: ({}) -[导致]-> ({})", startNode.getName(), endNodeName);
        }
        log.info("--- 知识图谱初始化完成！ ---");
    }
}