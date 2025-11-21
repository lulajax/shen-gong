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
 * 直播报告生成 Agent
 * 整合分析结果生成结构化报告
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
public class LiveReportAgent implements Agent {

    private static final AgentType AGENT_TYPE = AgentType.LIVE_REPORT;

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
        log.info("LiveReportAgent handling task: {}", task.getTaskId());

        try {
            // 获取分析数据
            Map<String, Object> analysis = task.getContextValue("analysis");
            Map<String, Object> metrics = task.getContextValue("metrics");

            if (analysis == null || metrics == null) {
                return AgentResult.error("Missing analysis or metrics from previous steps");
            }

            // 生成报告
            Map<String, Object> report = generateReport(analysis, metrics, task);

            // 生成摘要
            String summary = generateSummary(metrics);

            return AgentResult.ok(summary, Map.of(
                    "report", report,
                    "reportType", "live_performance"
            ));

        } catch (Exception e) {
            log.error("LiveReportAgent failed: {}", e.getMessage(), e);
            return AgentResult.error("Report generation failed: " + e.getMessage());
        }
    }

    /**
     * 生成报告
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> generateReport(Map<String, Object> analysis,
                                                Map<String, Object> metrics,
                                                AgentTask task) {
        Map<String, Object> report = new HashMap<>();

        // 报告元数据
        report.put("reportType", "live_performance");
        report.put("timeRange", task.getPayloadValue("timeRange"));
        report.put("filters", task.getPayloadValue("filters"));

        // KPI 指标
        report.put("kpi", metrics);

        // 分析结果
        Map<String, Object> analysisMap = (Map<String, Object>) analysis;
        report.put("findings", analysisMap.getOrDefault("findings", List.of()));
        report.put("rootCauses", analysisMap.getOrDefault("rootCauses", List.of()));
        report.put("suggestions", analysisMap.getOrDefault("suggestions", List.of()));

        // 总结
        report.put("summary", analysisMap.getOrDefault("summary", ""));

        return report;
    }

    /**
     * 生成摘要
     */
    private String generateSummary(Map<String, Object> metrics) {
        double gmvChangeRate = ((Number) metrics.getOrDefault("gmvChangeRate", 0)).doubleValue();
        double gmv = ((Number) metrics.getOrDefault("gmv", 0)).doubleValue();

        String trend = gmvChangeRate >= 0 ? "上升" : "下降";
        int changePercent = (int) Math.abs(gmvChangeRate * 100);

        return String.format("直播间整体 GMV 为 %.2f，较前一日%s约 %d%%",
                gmv, trend, changePercent);
    }

    @Override
    public String description() {
        return AGENT_TYPE.getDescription();
    }
}
