package com.shengong.agentruntime.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 任务模型
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Data
public class AgentTask {

    /**
     * 任务唯一标识
     */
    private String taskId = UUID.randomUUID().toString();

    /**
     * 任务类型 (analysis, analysis_report, anomaly_detection, scrape_and_analyze等)
     */
    private String taskType;

    /**
     * 业务域 (generic, live, order, data等)
     */
    private String domain;

    /**
     * 任务载荷数据
     */
    private Map<String, Object> payload = new HashMap<>();

    /**
     * 上下文信息
     */
    private Map<String, Object> context = new HashMap<>();

    /**
     * 跟踪ID (用于分布式追踪)
     */
    private String traceId = UUID.randomUUID().toString();

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 地区语言
     */
    private String locale = "zh-CN";

    /**
     * 时区
     */
    private String timezone = "Asia/Shanghai";

    /**
     * 创建时间
     */
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 优先级 (1-10, 数字越大优先级越高)
     */
    private Integer priority = 5;

    /**
     * 超时时间(毫秒)
     */
    private Long timeout = 300000L; // 默认5分钟

    /**
     * 获取 payload 中的值
     */
    @SuppressWarnings("unchecked")
    public <T> T getPayloadValue(String key) {
        return (T) payload.get(key);
    }

    /**
     * 获取 context 中的值
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextValue(String key) {
        return (T) context.get(key);
    }

    /**
     * 设置 payload 值
     */
    public AgentTask putPayload(String key, Object value) {
        this.payload.put(key, value);
        return this;
    }

    /**
     * 设置 context 值
     */
    public AgentTask putContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }
}
