package com.shengong.agentruntime.collaboration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengong.agentruntime.entity.MultiAgentExecutionEntity;
import com.shengong.agentruntime.model.collaboration.*;
import com.shengong.agentruntime.repository.MultiAgentExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多Agent协同服务(门面)
 * 整合TaskOrchestrator, ExecutionCoordinator, ResultAggregator
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiAgentCollaborationService {

    private final TaskOrchestrator taskOrchestrator;
    private final ExecutionCoordinator executionCoordinator;
    private final ResultAggregator resultAggregator;
    private final MultiAgentExecutionRepository multiAgentExecutionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行多Agent协同任务
     *
     * @param multiAgentTask 多Agent任务
     * @return 执行结果
     */
    public MultiAgentResult execute(MultiAgentTask multiAgentTask) {
        log.info("开始执行多Agent协同任务: taskId={}, mode={}",
                multiAgentTask.getTaskId(), multiAgentTask.getExecutionMode());

        LocalDateTime startTime = LocalDateTime.now();
        String parentTaskId = multiAgentTask.getTaskId();

        try {
            // 1. 获取或生成执行计划
            ExecutionPlan plan = multiAgentTask.getExecutionPlan();
            if (plan == null) {
                log.warn("执行计划为空,尝试重新分解任务");
                String originalInput = multiAgentTask.getPayloadValue("originalInput");
                plan = taskOrchestrator.decompose(originalInput, multiAgentTask.getContext());
                multiAgentTask.setExecutionPlan(plan);
            }

            // 2. 初始化协同上下文
            CollaborationContext context = multiAgentTask.getCollaborationContext();
            if (context == null) {
                context = new CollaborationContext();
                context.setParentTaskId(parentTaskId);
                context.setOriginalInput(multiAgentTask.getPayloadValue("originalInput"));
                multiAgentTask.setCollaborationContext(context);
            }

            // 3. 保存多Agent任务开始记录
            MultiAgentExecutionEntity executionEntity = saveExecutionStart(multiAgentTask, plan);

            // 4. 执行子任务
            List<SubTaskResult> subTaskResults = executionCoordinator.execute(plan, context);

            // 5. 聚合结果
            String originalInput = context.getOriginalInput();
            MultiAgentResult result = resultAggregator.aggregate(subTaskResults, originalInput);

            // 6. 设置执行计划信息
            result.setExecutionPlan(plan);

            // 7. 计算总执行时长
            long totalLatency = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
            result.setTotalLatencyMs(totalLatency);

            // 8. 更新多Agent任务完成记录
            updateExecutionResult(executionEntity, result, totalLatency);

            log.info("多Agent协同任务执行完成: taskId={}, status={}, latency={}ms, success={}, failure={}",
                    parentTaskId, result.getStatus(), totalLatency,
                    result.getSuccessCount(), result.getFailureCount());

            return result;

        } catch (Exception e) {
            log.error("多Agent协同任务执行失败: taskId={}, error={}",
                    parentTaskId, e.getMessage(), e);

            // 创建错误结果
            MultiAgentResult errorResult = MultiAgentResult.error(
                    "多Agent任务执行失败: " + e.getMessage(),
                    null
            );

            long totalLatency = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
            errorResult.setTotalLatencyMs(totalLatency);

            // 尝试更新数据库记录
            try {
                MultiAgentExecutionEntity entity = multiAgentExecutionRepository
                        .findByParentTaskId(parentTaskId)
                        .orElse(null);
                if (entity != null) {
                    updateExecutionResult(entity, errorResult, totalLatency);
                }
            } catch (Exception ex) {
                log.error("更新执行记录失败: {}", ex.getMessage());
            }

            return errorResult;
        }
    }

    /**
     * 保存多Agent任务开始记录
     *
     * @param task 多Agent任务
     * @param plan 执行计划
     * @return 执行实体
     */
    private MultiAgentExecutionEntity saveExecutionStart(MultiAgentTask task, ExecutionPlan plan) {
        try {
            MultiAgentExecutionEntity entity = MultiAgentExecutionEntity.builder()
                    .parentTaskId(task.getTaskId())
                    .executionMode(plan.getMode().name())
                    .status("running")
                    .userId(task.getUserId())
                    .successCount(0)
                    .failureCount(0)
                    .build();

            // 序列化执行计划为JSON
            try {
                Map<String, Object> planJson = objectMapper.convertValue(plan, Map.class);
                entity.setExecutionPlanJson(planJson);
            } catch (Exception e) {
                log.warn("序列化执行计划失败: {}", e.getMessage());
            }

            return multiAgentExecutionRepository.save(entity);
        } catch (Exception e) {
            log.error("保存多Agent任务开始记录失败: {}", e.getMessage(), e);
            // 返回一个临时实体,不影响任务执行
            return new MultiAgentExecutionEntity();
        }
    }

    /**
     * 更新多Agent任务完成记录
     *
     * @param entity       执行实体
     * @param result       执行结果
     * @param totalLatency 总执行时长
     */
    private void updateExecutionResult(MultiAgentExecutionEntity entity,
                                       MultiAgentResult result,
                                       long totalLatency) {
        try {
            entity.setStatus(result.getStatus());
            entity.setSuccessCount(result.getSuccessCount());
            entity.setFailureCount(result.getFailureCount());
            entity.setTotalLatencyMs((int) totalLatency);
            entity.setCompletedAt(LocalDateTime.now());

            multiAgentExecutionRepository.save(entity);
            log.debug("更新多Agent任务记录成功: parentTaskId={}", entity.getParentTaskId());
        } catch (Exception e) {
            log.error("更新多Agent任务记录失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 预览执行计划(不执行)
     *
     * @param userInput 用户输入
     * @param context   上下文
     * @return 执行计划
     */
    public ExecutionPlan previewPlan(String userInput, Map<String, Object> context) {
        try {
            return taskOrchestrator.decompose(userInput, context);
        } catch (Exception e) {
            log.error("生成执行计划失败: {}", e.getMessage(), e);

            // 返回错误计划
            ExecutionPlan errorPlan = new ExecutionPlan();
            errorPlan.setReason("生成执行计划失败: " + e.getMessage());
            return errorPlan;
        }
    }

    /**
     * 判断是否需要多Agent协同
     *
     * @param userInput 用户输入
     * @param context   上下文
     * @return true if needs collaboration
     */
    public boolean needsCollaboration(String userInput, Map<String, Object> context) {
        return taskOrchestrator.needsCollaboration(userInput, context);
    }
}
