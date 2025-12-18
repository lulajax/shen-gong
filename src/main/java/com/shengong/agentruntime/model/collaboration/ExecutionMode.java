package com.shengong.agentruntime.model.collaboration;

/**
 * 多Agent执行模式
 *
 * @author 神工团队
 * @since 1.0.0
 */
public enum ExecutionMode {
    /**
     * 串行执行: Agent按顺序执行,前一个的输出作为下一个的输入
     */
    SEQUENTIAL,

    /**
     * 并行执行: 多个Agent同时处理任务的不同部分,最后聚合结果
     */
    PARALLEL,

    /**
     * 层级执行: 主Agent分解任务并分配给子Agent,然后整合结果
     */
    HIERARCHICAL
}
