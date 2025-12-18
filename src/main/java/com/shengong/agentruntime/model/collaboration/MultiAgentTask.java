package com.shengong.agentruntime.model.collaboration;

import com.shengong.agentruntime.model.AgentTask;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 多Agent协同任务模型
 * 继承自AgentTask,增加多Agent协同相关的信息
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MultiAgentTask extends AgentTask {
    /**
     * 标识这是一个多Agent任务
     */
    private boolean collaborative = true;

    /**
     * 执行计划
     */
    private ExecutionPlan executionPlan;

    /**
     * 协同上下文
     */
    private CollaborationContext collaborationContext;

    /**
     * 执行模式
     */
    private ExecutionMode executionMode = ExecutionMode.SEQUENTIAL;

    /**
     * 是否是强制多Agent模式
     * 当用户通过 /send-multi 端点显式触发时为 true
     */
    private boolean forced = false;
}
