package com.shengong.agentruntime.core.agent.impl;

import com.shengong.agentruntime.core.agent.Agent;
import com.shengong.agentruntime.core.agent.AgentType;
import com.shengong.agentruntime.llm.LlmClient;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * 异常检测 Agent
 * 检测订单数据中的异常模式
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnomalyDetectionAgent implements Agent {

    private static final AgentType AGENT_TYPE = AgentType.ANOMALY_DETECTION;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String name() {
        return AGENT_TYPE.getName();
    }

    @Override
    public List<String> domains() {
        return AGENT_TYPE.getDomains();
    }

    @Override
    public boolean supports(String taskType, String domain) {
        return AGENT_TYPE.supports(taskType, domain);
    }

    @Override
    @SuppressWarnings("unchecked")
    public AgentResult handle(AgentTask task) {
        log.info("AnomalyDetectionAgent handling task: {}", task.getTaskId());

        try {
            Map<String, Object> orders = task.getContextValue("orders");
            if (orders == null) {
                return AgentResult.error("Missing orders from previous step");
            }

            // 计算统计指标
            Map<String, Object> statistics = calculateStatistics(orders);

            // 使用规则 + LLM 检测异常
            List<Map<String, Object>> anomalies = detectAnomalies(statistics);

            String summary = String.format("检测到 %d 个异常模式", anomalies.size());

            return AgentResult.ok(summary, Map.of(
                    "anomalies", anomalies,
                    "statistics", statistics
            ));

        } catch (Exception e) {
            log.error("AnomalyDetectionAgent failed: {}", e.getMessage(), e);
            return AgentResult.error("Anomaly detection failed: " + e.getMessage());
        }
    }

    /**
     * 计算订单统计指标
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> calculateStatistics(Map<String, Object> ordersData) {
        Map<String, Object> stats = new HashMap<>();

        List<Map<String, Object>> orders = (List<Map<String, Object>>)
                ordersData.getOrDefault("orderList", List.of());

        // 基础统计
        stats.put("totalOrders", orders.size());

        // 退货率
        long refundCount = orders.stream()
                .filter(o -> "REFUNDED".equals(o.get("status")))
                .count();
        double refundRate = orders.isEmpty() ? 0 : (double) refundCount / orders.size();
        stats.put("refundRate", refundRate);
        stats.put("refundCount", refundCount);

        // 延迟发货数
        long delayedShipments = orders.stream()
                .filter(o -> Boolean.TRUE.equals(o.get("delayedShipment")))
                .count();
        stats.put("delayedShipments", delayedShipments);

        // 平均处理时间
        double avgProcessingTime = orders.stream()
                .mapToDouble(o -> ((Number) o.getOrDefault("processingTimeHours", 24)).doubleValue())
                .average()
                .orElse(0);
        stats.put("avgProcessingTimeHours", avgProcessingTime);

        return stats;
    }

    /**
     * 检测异常
     */
    private List<Map<String, Object>> detectAnomalies(Map<String, Object> statistics) {
        List<Map<String, Object>> anomalies = new ArrayList<>();

        // 规则1: 退货率过高
        double refundRate = ((Number) statistics.get("refundRate")).doubleValue();
        if (refundRate > 0.15) { // 15%
            anomalies.add(Map.of(
                    "type", "HIGH_REFUND_RATE",
                    "severity", "high",
                    "metric", "refundRate",
                    "value", refundRate,
                    "threshold", 0.15,
                    "description", String.format("退货率异常高: %.1f%%，正常范围应低于15%%", refundRate * 100)
            ));
        }

        // 规则2: 延迟发货过多
        long delayedShipments = ((Number) statistics.get("delayedShipments")).longValue();
        long totalOrders = ((Number) statistics.get("totalOrders")).longValue();
        double delayRate = totalOrders > 0 ? (double) delayedShipments / totalOrders : 0;
        if (delayRate > 0.10) { // 10%
            anomalies.add(Map.of(
                    "type", "HIGH_DELAY_RATE",
                    "severity", "medium",
                    "metric", "delayedShipments",
                    "value", delayedShipments,
                    "rate", delayRate,
                    "description", String.format("发货延迟率过高: %.1f%%，影响了 %d 个订单", delayRate * 100, delayedShipments)
            ));
        }

        // 规则3: 处理时间过长
        double avgProcessingTime = ((Number) statistics.get("avgProcessingTimeHours")).doubleValue();
        if (avgProcessingTime > 48) { // 48小时
            anomalies.add(Map.of(
                    "type", "SLOW_PROCESSING",
                    "severity", "medium",
                    "metric", "avgProcessingTimeHours",
                    "value", avgProcessingTime,
                    "threshold", 48,
                    "description", String.format("平均处理时间过长: %.1f 小时，正常应在 48 小时内", avgProcessingTime)
            ));
        }

        return anomalies;
    }

    @Override
    public String description() {
        return AGENT_TYPE.getDescription();
    }
}
