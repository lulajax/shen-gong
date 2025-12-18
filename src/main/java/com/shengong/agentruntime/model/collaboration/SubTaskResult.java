package com.shengong.agentruntime.model.collaboration;

import com.shengong.agentruntime.model.AgentResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 子任务执行结果
 * 包含子任务的基本信息和执行结果
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubTaskResult {
    /**
     * 子任务ID
     */
    private String taskId;

    /**
     * Agent名称
     */
    private String agentName;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 业务域
     */
    private String domain;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 执行结果
     */
    private AgentResult result;

    /**
     * 执行耗时(毫秒)
     */
    private Long latencyMs;

    /**
     * 重试次数
     */
    private int retryCount;

    /**
     * 执行顺序
     */
    private Integer order;

    /**
     * 便捷构造函数
     */
    public SubTaskResult(SubTask task, AgentResult result) {
        this.taskId = task.getTaskId();
        this.agentName = task.getAgentName();
        this.taskType = task.getTaskType();
        this.domain = task.getDomain();
        this.description = task.getDescription();
        this.result = result;
        this.retryCount = task.getRetryCount();
        this.order = task.getOrder();
        this.latencyMs = result != null ? result.getLatencyMs() : null;
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return result != null && result.isSuccess();
    }

    /**
     * 判断是否失败
     */
    public boolean isFailure() {
        return result == null || result.isError();
    }
}
