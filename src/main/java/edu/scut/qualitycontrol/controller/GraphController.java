package edu.scut.qualitycontrol.controller;

import dev.langchain4j.service.TokenStream;
import edu.scut.qualitycontrol.model.dto.DefectDto;
import edu.scut.qualitycontrol.model.dto.FactorDto;
import edu.scut.qualitycontrol.model.dto.GraphDataDto;
import edu.scut.qualitycontrol.model.dto.RelationshipDto;
import edu.scut.qualitycontrol.model.entity.DefectType;
import edu.scut.qualitycontrol.model.entity.InfluencingFactor;
import edu.scut.qualitycontrol.service.GraphManagerService;
import edu.scut.qualitycontrol.service.GraphNarratorService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 用于管理知识图谱节点的 RESTful API 控制器
 * 暴露了对影响因素和缺陷类型节点的增删改查操作
 */
@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphManagerService graphManagerService;

    // 注入 AI 服务
    private final GraphNarratorService narratorService;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // 通过构造函数注入 GraphManagerService
    public GraphController(GraphManagerService graphManagerService, GraphNarratorService narratorService) {
        this.graphManagerService = graphManagerService;
        this.narratorService = narratorService;
    }

    // agent智能品控

    /**
     * 接收缺陷名称，查询因果路径，并调用大模型生成通俗解释
     */
    @GetMapping("/narrate")
    public ResponseEntity<String> narrate(@RequestParam String defectType) {
        // 检查缺陷是否存在
        Optional<?> nodeOpt = graphManagerService.findNodeByName(defectType);

        // 如果找不到节点，或者找到的不是缺陷类型(而是影响因素)，直接返回提示
        if (nodeOpt.isEmpty() || !(nodeOpt.get() instanceof DefectType)) {
            return ResponseEntity.ok("系统提示：在知识库中未找到名为“" + defectType + "”的缺陷类型，无法进行因果分析。请检查输入名称是否正确。");
        }

        // 调用 GraphManagerService 查询所有因果路径
        List<List<InfluencingFactor>> paths = graphManagerService.findAllCausalPathsForDefect(defectType);

        if (paths.isEmpty()) {
            return ResponseEntity.ok("系统提示：虽然找到了缺陷“" + defectType + "”，但在库中暂时没有记录导致该缺陷的具体因果链路。");
        }

        // 数据格式化：将 List<List<InfluencingFactor>> 转换为 AI 能读懂的自然语言文本
        String formattedContext = formatPathsForAI(paths);

        // 调用 AI 生成文本
        String description = narratorService.analyzeDefectCauses(defectType, formattedContext);

        return ResponseEntity.ok(description);
    }

    /**
     * SSE 流式接口
     * 前端使用 EventSource 调用: /api/graph/narrate/stream?defectType=xxx
     */
    @GetMapping(value = "/narrate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNarrate(@RequestParam String defectType) {
        // 1. 建立长连接，超时设为 2 分钟
        SseEmitter emitter = new SseEmitter(120000L);

        executor.execute(() -> {
            try {
                // 查库
                Optional<?> nodeOpt = graphManagerService.findNodeByName(defectType);
                if (nodeOpt.isEmpty() || !(nodeOpt.get() instanceof DefectType)) {
                    emitter.send("系统提示：未找到缺陷类型“" + defectType + "”。");
                    emitter.complete();
                    return;
                }

                List<List<InfluencingFactor>> paths = graphManagerService.findAllCausalPathsForDefect(defectType);
                if (paths.isEmpty()) {
                    emitter.send("系统提示：未发现因果路径数据。");
                    emitter.complete();
                    return;
                }

                String formattedContext = formatPathsForAI(paths);

                // 调用 AI 并流式输出
                TokenStream tokenStream = narratorService.streamAnalyzeDefectCauses(defectType, formattedContext);

                // 发生错误
                tokenStream
                        .onNext(token -> {
                            try {
                                // 收到一个字(token)，发给前端
                                emitter.send(token);
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        })
                        .onComplete(token -> {
                            // 生成完毕
                            emitter.complete();
                        })
                        .onError(emitter::completeWithError)
                        .start();

            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 辅助方法：将路径对象列表转换为字符串描述
     */
    private String formatPathsForAI(List<List<InfluencingFactor>> paths) {
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (List<InfluencingFactor> path : paths) {
            sb.append("路径 ").append(index++).append("：");

            // 将路径中的节点用 "->" 连接，并附带标准信息
            String pathStr = path.stream()
                    .map(factor -> {
                        String info = factor.getName();
                        // 如果有标准或描述，拼接到名字后面，帮助AI理解
                        if (factor.getStandard() != null && !factor.getStandard().isEmpty()) {
                            info += "(标准:" + factor.getStandard() + ")";
                        }
                        if (factor.getDescription() != null && !factor.getDescription().isEmpty()) {
                            info += "(备注:" + factor.getDescription() + ")";
                        }
                        return info;
                    })
                    .collect(Collectors.joining(" -> "));

            sb.append(pathStr).append("\n");
        }
        return sb.toString();
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
     * 根据名称模糊搜索节点。
     * @param name 搜索关键词
     * @return 匹配的节点列表（包含影响因素和缺陷类型）
     */
    @GetMapping("/nodes/search")
    public ResponseEntity<List<Object>> findNodesByNameFuzzy(@RequestParam String name) {
        List<Object> nodes = graphManagerService.findNodesByNameFuzzy(name);
        return ResponseEntity.ok(nodes);
    }

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