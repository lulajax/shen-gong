package com.shengong.agentruntime.core.agent.impl;

import com.shengong.agentruntime.core.agent.Agent;
import com.shengong.agentruntime.core.agent.AgentType;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 直播数据预处理 Agent
 * 计算直播关键指标 (GMV、在线人数、转化率等)
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
public class LiveDataPrepAgent implements Agent {

    private static final AgentType AGENT_TYPE = AgentType.LIVE_DATA_PREP;

    @Override
    public String name() {
        return AGENT_TYPE.getName();
    }
    
    @Override
    public String taskType() {
        return AGENT_TYPE.getTaskType();
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
    public List<String> requiredParams() {
        return List.of("rawData");
    }

    @Override
    public Map<String, String> paramDescriptions() {
        return Map.of(
            "rawData", "原始直播数据，通常来自 LiveDataFetchAgent 的输出"
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public AgentResult handle(AgentTask task) {
        log.info("LiveDataPrepAgent handling task: {}", task.getTaskId());

        try {
            // 统一参数验证
            AgentResult validationError = validateParams(task);
            if (validationError != null) {
                return validationError;
            }

            // 从上下文或 payload 中获取原始数据
            Map<String, Object> rawData = task.getParam("rawData");

            // 模拟计算指标
            Map<String, Object> metrics = calculateMetrics(rawData);

            return AgentResult.ok("Metrics calculated successfully", Map.of(
                    "metrics", metrics,
                    "rawDataSummary", Map.of(
                            "dataPoints", rawData.size(),
                            "timeRange", rawData.get("timeRange")
                    )
            ));

        } catch (Exception e) {
            log.error("LiveDataPrepAgent failed: {}", e.getMessage(), e);
            return AgentResult.error("Data preparation failed: " + e.getMessage());
        }
    }

    /**
     * 计算直播指标
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> calculateMetrics(Map<String, Object> rawData) {
        Map<String, Object> metrics = new HashMap<>();

        // 模拟从原始数据中提取和计算指标
        Map<String, Object> liveData = (Map<String, Object>) rawData.getOrDefault("liveMetrics", Map.of());

        // GMV 相关
        double gmv = ((Number) liveData.getOrDefault("totalGmv", 12345.67)).doubleValue();
        double previousGmv = ((Number) liveData.getOrDefault("previousGmv", 17000.0)).doubleValue();
        double gmvChangeRate = previousGmv > 0 ? (gmv - previousGmv) / previousGmv : 0;

        metrics.put("gmv", gmv);
        metrics.put("previousGmv", previousGmv);
        metrics.put("gmvChangeRate", gmvChangeRate);

        // 观众数据
        metrics.put("peakViewers", liveData.getOrDefault("peakViewers", 980));
        metrics.put("avgViewers", liveData.getOrDefault("avgViewers", 430));
        metrics.put("totalViewers", liveData.getOrDefault("totalViewers", 2500));

        // 互动数据
        metrics.put("giftCount", liveData.getOrDefault("giftCount", 230));
        metrics.put("commentCount", liveData.getOrDefault("commentCount", 1200));
        metrics.put("likeCount", liveData.getOrDefault("likeCount", 5600));

        // 转化率
        int orderCount = ((Number) liveData.getOrDefault("orderCount", 87)).intValue();
        int totalViewers = ((Number) metrics.get("totalViewers")).intValue();
        double conversionRate = totalViewers > 0 ? (double) orderCount / totalViewers : 0;
        metrics.put("conversionRate", conversionRate);
        metrics.put("orderCount", orderCount);

        return metrics;
    }

    @Override
    public String description() {
        return AGENT_TYPE.getDescription();
    }
}
