package com.shengong.agentruntime.core.agent.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengong.agentruntime.core.agent.AbstractAgent;
import com.shengong.agentruntime.core.agent.annotation.AgentDefinition;
import com.shengong.agentruntime.core.param.AgentParam;
import com.shengong.agentruntime.llm.LlmClient;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 根因分析 Agent
 * 使用 LLM 分析异常的根本原因
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
@AgentDefinition(
    name = "RootCauseAgent",
    domains = {"order"},
    taskType = "anomaly_detection",
    description = "Analyze root causes of order anomalies using LLM"
)
public class RootCauseAgent extends AbstractAgent<RootCauseAgent.RootCauseParams> {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RootCauseAgent(LlmClient llmClient) {
        super(RootCauseParams.class);
        this.llmClient = llmClient;
    }

    @Data
    public static class RootCauseParams {
        @AgentParam(required = true, description = "异常列表，来自 AnomalyDetectionAgent 的输出")
        private List<Map<String, Object>> anomalies;

        @AgentParam(required = false, description = "统计数据，包含各类指标")
        private Map<String, Object> statistics;
    }

    @Override
    protected AgentResult execute(AgentTask task, RootCauseParams params) {
        log.info("RootCauseAgent handling task: {}", task.getTaskId());

        try {
            List<Map<String, Object>> anomalies = params.getAnomalies();
            Map<String, Object> statistics = params.getStatistics();
            if (statistics == null) {
                statistics = Map.of();
            }

            if (anomalies == null || anomalies.isEmpty()) {
                return AgentResult.ok("No anomalies found, no root cause analysis needed", Map.of(
                        "rootCauses", List.of(),
                        "solutions", List.of()
                ));
            }

            // 使用 LLM 分析根因
            Map<String, Object> analysis = analyzeRootCauses(anomalies, statistics);

            String summary = String.format("识别了 %d 个可能的根本原因",
                    ((List<?>) analysis.get("rootCauses")).size());

            return AgentResult.ok(summary, Map.of(
                    "rootCauseAnalysis", analysis,
                    "anomalies", anomalies
            ));

        } catch (Exception e) {
            log.error("RootCauseAgent failed: {}", e.getMessage(), e);
            return AgentResult.error("Root cause analysis failed: " + e.getMessage());
        }
    }

    /**
     * 分析根本原因
     */
    private Map<String, Object> analyzeRootCauses(List<Map<String, Object>> anomalies,
                                                   Map<String, Object> statistics) {
        try {
            String systemPrompt = """
                你是一个订单异常诊断专家。请分析以下订单异常情况,找出根本原因并提供解决方案。

                请按以下 JSON 格式输出:
                {
                  "rootCauses": [
                    {"cause": "原因描述", "confidence": "high/medium/low", "evidence": "证据"}
                  ],
                  "solutions": [
                    {"solution": "解决方案", "priority": "high/medium/low", "impact": "预期影响"}
                  ]
                }
                """;

            String anomaliesJson = objectMapper.writeValueAsString(Map.of(
                    "anomalies", anomalies,
                    "statistics", statistics
            ));

            String userPrompt = "订单异常数据如下:\n" + anomaliesJson + "\n\n请进行根因分析。";

            log.debug("Calling LLM for root cause analysis...");
            String llmResponse = llmClient.chat(systemPrompt, userPrompt);

            // 解析响应
            return parseResponse(llmResponse);

        } catch (Exception e) {
            log.error("LLM analysis failed, using fallback: {}", e.getMessage());
            return getFallbackAnalysis(anomalies);
        }
    }

    /**
     * 解析 LLM 响应
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(String response) {
        try {
            String jsonContent = response;

            // 提取 JSON
            if (response.contains("```json")) {
                int start = response.indexOf("```json") + 7;
                int end = response.indexOf("```", start);
                if (end > start) {
                    jsonContent = response.substring(start, end).trim();
                }
            }

            return objectMapper.readValue(jsonContent, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse LLM response, using fallback");
            return Map.of(
                    "summary", response,
                    "rootCauses", List.of(),
                    "solutions", List.of()
            );
        }
    }

    /**
     * 获取兜底分析结果
     */
    private Map<String, Object> getFallbackAnalysis(List<Map<String, Object>> anomalies) {
        List<Map<String, Object>> rootCauses = new ArrayList<>();
        List<Map<String, Object>> solutions = new ArrayList<>();

        for (Map<String, Object> anomaly : anomalies) {
            String type = (String) anomaly.get("type");

            switch (type) {
                case "HIGH_REFUND_RATE" -> {
                    rootCauses.add(Map.of(
                            "cause", "商品质量问题或描述不符",
                            "confidence", "high",
                            "evidence", "退货率异常高"
                    ));
                    solutions.add(Map.of(
                            "solution", "加强商品质检和描述准确性",
                            "priority", "high",
                            "impact", "预计可降低退货率 5-10%"
                    ));
                }
                case "HIGH_DELAY_RATE" -> {
                    rootCauses.add(Map.of(
                            "cause", "仓储物流处理能力不足",
                            "confidence", "medium",
                            "evidence", "发货延迟率过高"
                    ));
                    solutions.add(Map.of(
                            "solution", "优化仓储流程或增加人力",
                            "priority", "high",
                            "impact", "提升物流效率 20%"
                    ));
                }
                case "SLOW_PROCESSING" -> {
                    rootCauses.add(Map.of(
                            "cause", "订单处理流程存在瓶颈",
                            "confidence", "medium",
                            "evidence", "平均处理时间过长"
                    ));
                    solutions.add(Map.of(
                            "solution", "自动化订单处理流程",
                            "priority", "medium",
                            "impact", "缩短处理时间 30%"
                    ));
                }
            }
        }

        return Map.of(
                "rootCauses", rootCauses,
                "solutions", solutions
        );
    }
}
