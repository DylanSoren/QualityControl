package edu.scut.qualitycontrol.repository;

import edu.scut.qualitycontrol.model.dto.CausalPathNode;
import edu.scut.qualitycontrol.model.entity.InfluencingFactor;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InfluencingFactorRepository extends Neo4jRepository<InfluencingFactor, Long> {
    Optional<InfluencingFactor> findByName(String name);

    // 自定义查询，用于查找直接导致某个缺陷的所有影响因素 (反向查询)
    // 等同于 database_manager.py 中的 find_all_causes_for_defect
    @Query("MATCH (f:影响因素)-[:导致]->(d:缺陷类型 {name: $defectName}) RETURN f")
    List<InfluencingFactor> findAllCausesForDefect(@Param("defectName") String defectName);

    /**
     * 新增方法：
     * 查找所有导致指定缺陷的因果路径。
     * @return 一个列表，其中每个元素都是一条路径上的 InfluencingFactor 节点列表。
     */
    @Query("MATCH p = (f:影响因素)-[:导致*1..]->(d:缺陷类型 {name: $defectName}) " +  // 1. 查找路径：从“影响因素”(f)开始，经过1..N个“导致”关系，到达指定的“缺陷类型”(d)
            "WHERE NOT (:影响因素)-[:导致]->(f) " +  // 2. 过滤根因：只保留那些起始节点(f)不被任何其他“影响因素”所导致的路径。
            "WITH COLLECT(p) AS paths " +  // 3. 收集路径：将所有通过筛选的根因路径(p)收集到一个列表(paths)中。
            "UNWIND range(0, size(paths) - 1) AS pathId " +  // 4. 生成路径ID：为每条路径创建一个临时的、从0开始的唯一数字ID(pathId)。
            "WITH paths[pathId] AS p, pathId " +  // 5. 关联路径与ID：使用pathId作为索引，从paths列表中取出对应的路径(p)。
            "UNWIND nodes(p)[0..-1] AS factorNode " +  // 6. 展开节点：获取路径(p)中的影响因素节点列表（排除最后的缺陷节点），并将其展开为多行。
            "RETURN pathId, factorNode AS factor")  // 7. 返回结果：返回路径ID和对应的影响因素节点，以便在Java中组装成最终结构。
    List<CausalPathNode> findCausalPathNodesForDefect(@Param("defectName") String defectName);
}