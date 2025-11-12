package edu.scut.qualitycontrol.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface GraphNarratorService {

    // 设定人设：PCB生产质量控制专家
    @SystemMessage(
        "你是一位经验丰富的PCB电路板生产品质控制专家。" +
        "你的任务是根据图数据库提供的【因果路径信息】，为一线生产人员解释导致某种【缺陷类型】的根本原因。" +
        "要求：" +
        "1. 语言风格：通俗易懂、专业但不要过于学术，适合车间工作人员阅读。" +
        "2. 逻辑清晰：如果有多个原因链条，请分点说明。" +
        "3. 结合标准：如果路径信息中包含“标准(standard)”或“描述(description)”，请务必在解释中提及，作为判断依据。" +
        "4. 总结建议：最后给出一句简短的排查建议。"
    )
    // 传入两个参数：缺陷名称 和 格式化后的路径文本
    @UserMessage(
        "当前检测到的缺陷类型是: {{defectName}}" +
        "数据库查询到的可能成因路径如下: {{pathsContext}}" +
        "请分析并生成报告。"
    )
    String analyzeDefectCauses(@V("defectName") String defectName, @V("pathsContext") String pathsContext);
}