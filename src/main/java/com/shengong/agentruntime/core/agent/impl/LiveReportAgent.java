package com.shengong.agentruntime.core.agent.impl;

import com.shengong.agentruntime.core.agent.AbstractAgent;
import com.shengong.agentruntime.core.agent.annotation.AgentDefinition;
import com.shengong.agentruntime.core.param.AgentParam;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import lombok.Data;
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
@AgentDefinition(
    name = "LiveReportAgent",
    domains = {"live"},
    taskType = "live_report",
    description = "Generate structured report for live streaming performance"
)
public class LiveReportAgent extends AbstractAgent<LiveReportAgent.LiveReportParams> {

    public LiveReportAgent() {
        super(LiveReportParams.class);
    }

    @Data
    public static class LiveReportParams {
        @AgentParam(required = true, description = "分析结果，包含 findings、rootCauses、suggestions 等")
        private Map<String, Object> analysis;

        @AgentParam(required = true, description = "直播指标数据")
        private Map<String, Object> metrics;
    }

    @Override
    protected AgentResult execute(AgentTask task, LiveReportParams params) {
        log.info("LiveReportAgent handling task: {}", task.getTaskId());

        try {
            // 获取分析数据
            Map<String, Object> analysis = params.getAnalysis();
            Map<String, Object> metrics = params.getMetrics();

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
        // analysis 已经是 Map<String, Object> 类型，无需强制转换
        report.put("findings", analysis.getOrDefault("findings", List.of()));
        report.put("rootCauses", analysis.getOrDefault("rootCauses", List.of()));
        report.put("suggestions", analysis.getOrDefault("suggestions", List.of()));

        // 总结
        report.put("summary", analysis.getOrDefault("summary", ""));

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
}
