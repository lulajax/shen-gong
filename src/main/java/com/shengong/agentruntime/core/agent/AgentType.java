package com.shengong.agentruntime.core.agent;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Agent 类型枚举
 * 统一管理所有 Agent 的配置信息
 *
 * @author 神工团队
 * @since 1.0.0
 */
public enum AgentType {

    /**
     * 通用分析 Agent
     */
    GENERIC_ANALYSIS(
            "GenericAnalysisAgent",
            List.of("generic"),
            "analysis",
            "Generic analysis agent powered by LLM"
    ),

    /**
     * 订单数据获取 Agent
     */
    ORDER_DATA(
            "OrderDataAgent",
            List.of("order"),
            "anomaly_detection",
            "Fetch order data for anomaly detection"
    ),

    /**
     * 异常检测 Agent
     */
    ANOMALY_DETECTION(
            "AnomalyDetectionAgent",
            List.of("order"),
            "anomaly_detection",
            "Detect anomalies in order data using rules and LLM"
    ),

    /**
     * 根因分析 Agent
     */
    ROOT_CAUSE(
            "RootCauseAgent",
            List.of("order"),
            "anomaly_detection",
            "Analyze root causes of order anomalies using LLM"
    ),

    /**
     * 直播数据获取 Agent
     */
    LIVE_DATA_FETCH(
            "LiveDataFetchAgent",
            List.of("live"),
            "analysis_report",
            "Fetch live streaming data from external services"
    ),

    /**
     * 直播数据预处理 Agent
     */
    LIVE_DATA_PREP(
            "LiveDataPrepAgent",
            List.of("live"),
            "analysis_report",
            "Calculate key metrics from live streaming data"
    ),

    /**
     * 直播数据分析 Agent
     */
    LIVE_ANALYSIS(
            "LiveAnalysisAgent",
            List.of("live"),
            "analysis_report",
            "Analyze live streaming data and find insights using LLM"
    ),

    /**
     * 直播报告生成 Agent
     */
    LIVE_REPORT(
            "LiveReportAgent",
            List.of("live"),
            "analysis_report",
            "Generate structured live streaming performance report"
    );

    private final String name;
    private final List<String> domains;
    private final String taskType;
    private final String description;

    AgentType(String name, List<String> domains, String taskType, String description) {
        this.name = name;
        this.domains = domains;
        this.taskType = taskType;
        this.description = description;
    }

    /**
     * 获取 Agent 名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取支持的业务域列表
     */
    public List<String> getDomains() {
        return domains;
    }

    /**
     * 获取支持的任务类型
     */
    public String getTaskType() {
        return taskType;
    }

    /**
     * 获取 Agent 描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 检查是否支持指定的任务类型和业务域
     */
    public boolean supports(String taskType, String domain) {
        return this.taskType.equals(taskType) && this.domains.contains(domain);
    }

    /**
     * 根据名称查找 AgentType
     */
    public static Optional<AgentType> fromName(String name) {
        return Arrays.stream(values())
                .filter(type -> type.getName().equals(name))
                .findFirst();
    }

    /**
     * 根据任务类型和业务域查找 AgentType
     */
    public static Optional<AgentType> findByTaskAndDomain(String taskType, String domain) {
        return Arrays.stream(values())
                .filter(type -> type.supports(taskType, domain))
                .findFirst();
    }

    /**
     * 获取所有 Agent 名称列表
     */
    public static List<String> getAllNames() {
        return Arrays.stream(values())
                .map(AgentType::getName)
                .toList();
    }

    /**
     * 获取指定业务域的所有 AgentType
     */
    public static List<AgentType> findByDomain(String domain) {
        return Arrays.stream(values())
                .filter(type -> type.getDomains().contains(domain))
                .toList();
    }

    /**
     * 获取指定任务类型的所有 AgentType
     */
    public static List<AgentType> findByTaskType(String taskType) {
        return Arrays.stream(values())
                .filter(type -> type.getTaskType().equals(taskType))
                .toList();
    }

    @Override
    public String toString() {
        return String.format("AgentType{name='%s', domains=%s, taskType='%s', description='%s'}",
                name, domains, taskType, description);
    }
}
