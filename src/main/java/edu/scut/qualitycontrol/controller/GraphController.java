package edu.scut.qualitycontrol.controller;

import edu.scut.qualitycontrol.model.dto.DefectDto;
import edu.scut.qualitycontrol.model.dto.FactorDto;
import edu.scut.qualitycontrol.model.dto.GraphDataDto;
import edu.scut.qualitycontrol.model.dto.RelationshipDto;
import edu.scut.qualitycontrol.model.entity.DefectType;
import edu.scut.qualitycontrol.model.entity.InfluencingFactor;
import edu.scut.qualitycontrol.service.GraphManagerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 用于管理知识图谱节点的 RESTful API 控制器。
 * 暴露了对影响因素和缺陷类型节点的增删改查操作。
 */
@RestController
@RequestMapping("/api/graph") // 为所有接口定义一个统一的基础路径
public class GraphController {

    private final GraphManagerService graphManagerService;

    // 通过构造函数注入 GraphManagerService
    public GraphController(GraphManagerService graphManagerService) {
        this.graphManagerService = graphManagerService;
    }

    // --- 节点查询 (Read) ---

    /**
     * 根据名称查找单个节点（可能是影响因素或缺陷类型）。
     * @param name 要查询的节点名称
     * @return 如果找到节点则返回节点信息，否则返回 404 Not Found。
     */
    @GetMapping("/node")
    public ResponseEntity<?> findNodeByName(@RequestParam String name) {
        Optional<?> nodeOpt = graphManagerService.findNodeByName(name);
        return nodeOpt.map(ResponseEntity::ok) // 如果存在，返回 200 OK 和节点数据
                .orElse(ResponseEntity.notFound().build()); // 如果不存在，返回 404
    }

    /**
     * 获取所有节点，可以按标签（类型）过滤。
     * @param label 可选参数，可以是 "影响因素" 或 "缺陷类型"
     * @return 节点列表
     */
    /**
     * 获取整个知识图谱的数据（所有节点和所有关系）。
     * @return 包含节点和关系列表的 GraphDataDto 对象
     */
    @GetMapping("/nodes")
    public ResponseEntity<GraphDataDto> getFullGraph() {
        GraphDataDto graphData = graphManagerService.getFullGraphData();
        return ResponseEntity.ok(graphData);
    }

    // --- 关系查询 (Read) ---

    /**
     * 查找导致指定缺陷类型的所有直接影响因素。
     * @param defectName 缺陷类型的名称
     * @return 影响因素列表
     */
    @GetMapping("/causes")
    public ResponseEntity<List<InfluencingFactor>> findCausesForDefect(@RequestParam String defectName) {
        List<InfluencingFactor> causes = graphManagerService.findAllCausesForDefect(defectName);
        return ResponseEntity.ok(causes);
    }

    /**
     * 查找导致指定缺陷类型的所有【因果路径】。
     * @param defectName 缺陷类型的名称
     * @return 一个路径列表，每个路径本身是一个按顺序排列的节点列表。
     */
    @GetMapping("/causal-paths")
    public ResponseEntity<List<List<InfluencingFactor>>> findCausalPathsForDefect(@RequestParam String defectName) {
        List<List<InfluencingFactor>> paths = graphManagerService.findAllCausalPathsForDefect(defectName);
        return ResponseEntity.ok(paths);
    }

    /**
     * 查找由某个影响因素直接导致的所有缺陷类型。
     * @param factorName 影响因素的名称
     * @return 缺陷类型列表
     */
    @GetMapping("/defects")
        public ResponseEntity<List<DefectType>> findDefectsCausedBy(@RequestParam String factorName) {
        List<DefectType> defects = graphManagerService.findAllDefectsCausedBy(factorName);
        return ResponseEntity.ok(defects);
    }

    // --- 节点创建与更新 (Create/Update) ---

    /**
     * 创建或更新一个影响因素节点。
     * 使用 POST 请求，并在请求体中传入节点数据。
     */
    @PostMapping("/factor")
    public ResponseEntity<InfluencingFactor> createOrUpdateFactor(@RequestBody FactorDto factorDto) {
        InfluencingFactor factor = graphManagerService.createOrUpdateFactor(
                factorDto.getName(),
                factorDto.getStandard(),
                factorDto.getDescription()
        );
        return ResponseEntity.ok(factor);
    }

    /**
     * 创建或更新一个缺陷类型节点。
     * 使用 POST 请求，并在请求体中传入节点数据。
     */
    @PostMapping("/defect")
    public ResponseEntity<DefectType> createOrUpdateDefect(@RequestBody DefectDto defectDto) {
        DefectType defect = graphManagerService.createOrUpdateDefect(
                defectDto.getName(),
                defectDto.getTypicalManifestations()
        );
        return ResponseEntity.ok(defect);
    }

    // --- 关系创建 (Create) ---

    /**
     * 在两个已存在的节点之间创建“导致”关系。
     */
    @PostMapping("/relationship")
    public ResponseEntity<String> createRelationship(@RequestBody RelationshipDto relationshipDto) {
        boolean success = graphManagerService.createRelationship(
                relationshipDto.getStartNodeName(),
                relationshipDto.getEndNodeName()
        );
        if (success) {
            return ResponseEntity.ok("'" + relationshipDto.getStartNodeName() + " -> "
                    + relationshipDto.getEndNodeName() + "' 关系创建成功。");
        } else {
            return ResponseEntity.badRequest().body("'" + relationshipDto.getStartNodeName() + " -> "
                    + relationshipDto.getEndNodeName() + "' 关系创建失败，请检查节点是否存在。");
        }
    }

    // --- 删除操作 (Delete) ---

    /**
     * 根据名称删除一个节点及其所有关联关系。
     * @param name 要删除的节点名称
     */
    @DeleteMapping("/node")
    public ResponseEntity<String> deleteNodeByName(@RequestParam String name) {
        boolean success = graphManagerService.deleteNodeByName(name);
        if (success) {
            return ResponseEntity.ok("节点 '" + name + "' 已成功删除。");
        } else {
            return ResponseEntity.status(404).body("节点 '" + name + "' 未找到。");
        }
    }

    /**
     * 删除两个节点之间的关系。
     */
    @DeleteMapping("/relationship")
    public ResponseEntity<String> deleteRelationship(@RequestBody RelationshipDto relationshipDto) {
        boolean success = graphManagerService.deleteRelationship(
                relationshipDto.getStartNodeName(),
                relationshipDto.getEndNodeName()
        );
        if (success) {
            return ResponseEntity.ok("'" + relationshipDto.getStartNodeName() + " -> "
                    + relationshipDto.getEndNodeName() + "' 关系已成功断开。");
        } else {
            return ResponseEntity.badRequest().body("'" + relationshipDto.getStartNodeName() + " -> "
                    + relationshipDto.getEndNodeName() + "' 关系断开失败，请检查节点或关系是否存在。");
        }
    }
}