package edu.scut.qualitycontrol.repository;

import edu.scut.qualitycontrol.model.entity.DefectType;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DefectTypeRepository extends Neo4jRepository<DefectType, Long> {
    // Spring Data会根据方法名自动生成查询：通过 name 属性查找节点
    Optional<DefectType> findByName(String name);

    // --- 新增的模糊查询方法 ---
    /**
     * 根据名称模糊查找缺陷类型节点
     * @param name 查询关键词
     * @return 匹配的缺陷类型列表
     */
    @Query("MATCH (d:缺陷类型) WHERE d.name CONTAINS $name RETURN d")
    List<DefectType> findByNameContaining(@Param("name") String name);
}