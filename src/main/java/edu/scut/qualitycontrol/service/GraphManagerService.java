package edu.scut.qualitycontrol.service;

import edu.scut.qualitycontrol.model.dto.CausalPathNode;
import edu.scut.qualitycontrol.model.dto.GraphDataDto;
import edu.scut.qualitycontrol.model.dto.LinkDto;
import edu.scut.qualitycontrol.model.entity.DefectType;
import edu.scut.qualitycontrol.model.entity.InfluencingFactor;
import edu.scut.qualitycontrol.repository.DefectTypeRepository;
import edu.scut.qualitycontrol.repository.InfluencingFactorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional // 建议在服务层开启事务
public class GraphManagerService {

    private final InfluencingFactorRepository influencingFactorRepository;
    private final DefectTypeRepository defectRepository;

    public GraphManagerService(InfluencingFactorRepository influencingFactorRepository, DefectTypeRepository defectRepository) {
        this.influencingFactorRepository = influencingFactorRepository;
        this.defectRepository = defectRepository;
        log.info("数据库连接成功，GraphManagerService 已初始化。");
    }

    // --- 增：创建和连接节点 ---
    public DefectType createOrUpdateDefect(String name, String manifestations) {
        return defectRepository.findByName(name)
                .map(defect -> { // 更新
                    defect.setTypicalManifestations(manifestations);
                    log.info("已更新缺陷类型节点：" + name);
                    return defectRepository.save(defect);
                })
                .orElseGet(() -> { // 创建
                    DefectType newDefect = new DefectType(name, manifestations);
                    log.info("已创建新的缺陷类型节点：" + name);
                    return defectRepository.save(newDefect);
                });
    }

    public InfluencingFactor createOrUpdateFactor(String name, String standard, String description) {
        return influencingFactorRepository.findByName(name)
                .map(factor -> { // 更新
                    if (standard != null) factor.setStandard(standard);
                    if (description != null) factor.setDescription(description);
                    log.info("已更新影响因素节点：" + name);
                    return influencingFactorRepository.save(factor);
                })
                .orElseGet(() -> { // 创建
                    InfluencingFactor newFactor = new InfluencingFactor(name, standard, description);
                    log.info("已创建新的影响因素节点：" + name);
                    return influencingFactorRepository.save(newFactor);
                });
    }

    public boolean createRelationship(String startNodeName, String endNodeName) {
        Optional<InfluencingFactor> startNodeOpt = influencingFactorRepository.findByName(startNodeName);
        if (startNodeOpt.isEmpty()) {
            log.error("错误：起始节点 '影响因素' '" + startNodeName + "' 不存在。");
            return false;
        }
        InfluencingFactor startNode = startNodeOpt.get();

        // 尝试将结束节点作为影响因素查找
        Optional<InfluencingFactor> endFactorOpt = influencingFactorRepository.findByName(endNodeName);
        if (endFactorOpt.isPresent()) {
            InfluencingFactor endNode = endFactorOpt.get();
            if (startNode.getLeadsToFactor().stream().anyMatch(f -> f.getId().equals(endNode.getId()))) {
                 log.info("关系已存在: (" + startNodeName + ") -[导致]-> (" + endNodeName + ")");
            } else {
                 startNode.getLeadsToFactor().add(endNode);
                 influencingFactorRepository.save(startNode);
                 log.info("已创建新关系: (" + startNodeName + ") -[导致]-> (" + endNodeName + ")");
            }
            return true;
        }

        // 尝试将结束节点作为缺陷类型查找
        Optional<DefectType> endDefectOpt = defectRepository.findByName(endNodeName);
        if (endDefectOpt.isPresent()) {
            DefectType endNode = endDefectOpt.get();
             if (startNode.getLeadsToDefect().stream().anyMatch(d -> d.getId().equals(endNode.getId()))) {
                log.info("关系已存在: (" + startNodeName + ") -[导致]-> (" + endNodeName + ")");
            } else {
                startNode.getLeadsToDefect().add(endNode);
                influencingFactorRepository.save(startNode);
                log.info("已创建新关系: (" + startNodeName + ") -[导致]-> (" + endNodeName + ")");
            }
            return true;
        }

        log.error("错误：结束节点 '{}' 不存在。", endNodeName);
        return false;
    }

    // --- 查：查询节点和关系 ---
    public List<?> findAllNodes(String label) {
        if ("缺陷类型".equals(label)) {
            return defectRepository.findAll();
        } else if ("影响因素".equals(label)) {
            return influencingFactorRepository.findAll();
        } else {
            List<Object> allNodes = new ArrayList<>();
            allNodes.addAll(defectRepository.findAll());
            allNodes.addAll(influencingFactorRepository.findAll());
            return allNodes;
        }
    }

    public Optional<?> findNodeByName(String name) {
        Optional<InfluencingFactor> factor = influencingFactorRepository.findByName(name);
        if (factor.isPresent()) {
            return factor;
        }
        return defectRepository.findByName(name);
    }
    
    public List<DefectType> findAllDefectsCausedBy(String factorName) {
        return influencingFactorRepository.findByName(factorName)
                .map(factor -> new ArrayList<>(factor.getLeadsToDefect()))
                .orElse(new ArrayList<>());
    }
    
    public List<InfluencingFactor> findAllCausesForDefect(String defectName) {
        return influencingFactorRepository.findAllCausesForDefect(defectName);
    }

    /**
     * 获取所有的影响因素链。
     */
    public List<List<InfluencingFactor>> findAllCausalPathsForDefect(String defectName) {
        // 1. 从 Repository 获取扁平化的路径节点列表
        List<CausalPathNode> pathNodes = influencingFactorRepository.findCausalPathNodesForDefect(defectName);

        // 2. 使用 Stream API 按 pathId 进行分组
        Map<Long, List<CausalPathNode>> groupedByPathId = pathNodes.stream()
                .collect(Collectors.groupingBy(CausalPathNode::getPathId));

        // 3. 将分组后的 Map 转换为 List<List<InfluencingFactor>>
        // 我们只关心每个分组中的 factor 对象列表
        List<List<InfluencingFactor>> finalResult = groupedByPathId.values().stream()
                .map(nodeList -> nodeList.stream()
                        // 注意：这里可以根据需要进行排序，以保证链路顺序
                        // .sorted(Comparator.comparing(node -> ...)) // 如果需要排序
                        .map(CausalPathNode::getFactor)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        return finalResult;
    }

    // --- 删：删除节点和关系 ---
    public boolean deleteNodeByName(String name) {
        Optional<?> nodeOpt = findNodeByName(name);
        if (nodeOpt.isPresent()) {
            Object node = nodeOpt.get();
            if (node instanceof DefectType) {
                defectRepository.delete((DefectType) node);
            } else {
                influencingFactorRepository.delete((InfluencingFactor) node);
            }
            log.info("已删除节点 '" + name + "'。");
            return true;
        } else {
            log.error("未找到节点 '" + name + "'。");
            return false;
        }
    }
    
    public boolean deleteRelationship(String startName, String endName) {
        Optional<InfluencingFactor> startNodeOpt = influencingFactorRepository.findByName(startName);
        if (startNodeOpt.isEmpty()) {
             log.error("错误：无法找到起始节点。");
             return false;
        }
        InfluencingFactor startNode = startNodeOpt.get();
        
        boolean removed = startNode.getLeadsToFactor().removeIf(factor -> factor.getName().equals(endName));
        if (!removed) {
             removed = startNode.getLeadsToDefect().removeIf(defect -> defect.getName().equals(endName));
        }

        if (removed) {
            influencingFactorRepository.save(startNode);
            log.info("已断开关系: (" + startName + ") -[导致]-> (" + endName + ")");
            return true;
        }

        log.error("关系不存在或节点类型不匹配: (" + startName + ") -[导致]-> (" + endName + ")");
        return false;
    }

    /**
     * 获取完整的图谱数据，包含所有节点和所有关系
     * @return GraphDataDto
     */
    @Transactional(readOnly = true) // 使用只读事务，提高查询性能
    public GraphDataDto getFullGraphData() {
        // 1. 获取所有节点
        List<Object> allNodes = new ArrayList<>();
        List<InfluencingFactor> factors = influencingFactorRepository.findAll();
        allNodes.addAll(factors);
        allNodes.addAll(defectRepository.findAll());

        // 2. 遍历所有影响因素节点，构建关系列表
        List<LinkDto> allLinks = new ArrayList<>();
        for (InfluencingFactor factor : factors) {
            // 添加指向其他影响因素的关系
            if (factor.getLeadsToFactor() != null) {
                for (InfluencingFactor targetFactor : factor.getLeadsToFactor()) {
                    allLinks.add(new LinkDto(factor.getName(), targetFactor.getName()));
                }
            }
            // 添加指向缺陷类型的关系
            if (factor.getLeadsToDefect() != null) {
                for (DefectType targetDefect : factor.getLeadsToDefect()) {
                    allLinks.add(new LinkDto(factor.getName(), targetDefect.getName()));
                }
            }
        }

        // 3. 封装并返回
        return new GraphDataDto(allNodes, allLinks);
    }
}