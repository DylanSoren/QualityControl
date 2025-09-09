package edu.scut.qualitycontrol.repository;

import edu.scut.qualitycontrol.model.entity.DefectType;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import java.util.Optional;

public interface DefectTypeRepository extends Neo4jRepository<DefectType, Long> {
    // Spring Data会根据方法名自动生成查询：通过 name 属性查找节点
    Optional<DefectType> findByName(String name);
}