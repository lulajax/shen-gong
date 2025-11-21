package com.shengong.agentruntime.service;

import com.shengong.agentruntime.core.agent.Agent;
import com.shengong.agentruntime.entity.TaskExecutionEntity;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Agent 路由服务
 * 负责将任务路由到合适的 Agent
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouterAgentService {

    private final AgentRegistry agentRegistry;
    private final TaskExecutionService taskExecutionService;

    /**
     * 路由并执行任务
     */
    public AgentResult route(AgentTask task) {
        String taskType = task.getTaskType();
        String domain = task.getDomain();

        log.info("Routing task: type={}, domain={}, traceId={}",
                taskType, domain, task.getTraceId());

        // 查找支持该任务的 Agent
        Agent agent = agentRegistry.findAgent(taskType, domain)
                .orElse(null);

        if (agent == null) {
            log.error("No agent found for task: type={}, domain={}", taskType, domain);
            AgentResult errorResult = AgentResult.error("No suitable agent found for task type: " + taskType + ", domain: " + domain);

            // 记录失败的路由尝试
            taskExecutionService.saveTaskExecution(task, "RouterAgent", errorResult);

            return errorResult;
        }

        // 记录任务开始执行
        TaskExecutionEntity executionRecord = taskExecutionService.saveTaskStart(task, agent.name());

        // 执行 Agent
        try {
            log.info("Executing agent: {}", agent.name());
            long startTime = System.currentTimeMillis();

            AgentResult result = agent.handle(task);

            long latency = System.currentTimeMillis() - startTime;
            result.setLatencyMs(latency);
            result.setAgentName(agent.name());
            result.addDebug("taskType", taskType);
            result.addDebug("domain", domain);
            result.addDebug("traceId", task.getTraceId());

            log.info("Agent execution completed: agent={}, status={}, latency={}ms",
                    agent.name(), result.getStatus(), latency);

            // 更新任务执行结果
            taskExecutionService.updateTaskResult(task.getTaskId(), result);

            return result;
        } catch (Exception e) {
            log.error("Agent execution failed: agent={}, error={}", agent.name(), e.getMessage(), e);
            AgentResult errorResult = AgentResult.error("Agent execution failed: " + e.getMessage());

            // 更新失败记录
            taskExecutionService.updateTaskResult(task.getTaskId(), errorResult);

            return errorResult;
        }
    }
}
