package com.shengong.agentruntime.model.collaboration;

/**
 * 子任务执行状态
 *
 * @author 神工团队
 * @since 1.0.0
 */
public enum SubTaskStatus {
    /**
     * 待执行
     */
    PENDING,

    /**
     * 执行中
     */
    RUNNING,

    /**
     * 执行成功
     */
    COMPLETED,

    /**
     * 执行失败
     */
    FAILED
}
