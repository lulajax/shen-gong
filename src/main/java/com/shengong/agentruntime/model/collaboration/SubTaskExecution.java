package com.shengong.agentruntime.model.collaboration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 子任务执行记录
 * 用于记录子任务的执行历史和状态变化
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubTaskExecution {
    /**
     * 子任务ID
     */
    private String taskId;

    /**
     * Agent名称
     */
    private String agentName;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 执行状态
     */
    private String status;

    /**
     * 执行耗时(毫秒)
     */
    private Long latencyMs;

    /**
     * 重试次数
     */
    private int retryCount;

    /**
     * 错误信息(如果失败)
     */
    private String errorMessage;
}
