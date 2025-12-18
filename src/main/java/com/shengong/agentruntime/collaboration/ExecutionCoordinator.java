package com.shengong.agentruntime.collaboration;

import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import com.shengong.agentruntime.model.collaboration.*;
import com.shengong.agentruntime.service.RouterAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 执行协调器
 * 根据执行计划协调多个Agent的执行
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionCoordinator {

    private final RouterAgentService routerAgentService;

    /**
     * 执行整个执行计划
     *
     * @param plan    执行计划
     * @param context 协同上下文
     * @return 执行结果列表
     */
    public List<SubTaskResult> execute(ExecutionPlan plan, CollaborationContext context) {
        if (plan == null || plan.getSubTasks() == null || plan.getSubTasks().isEmpty()) {
            log.warn("执行计划为空或无子任务");
            return new ArrayList<>();
        }

        log.info("开始执行多Agent任务: planId={}, mode={}, taskCount={}",
                plan.getPlanId(), plan.getMode(), plan.getTaskCount());

        // 根据执行模式选择执行策略
        switch (plan.getMode()) {
            case SEQUENTIAL:
                return executeSequential(plan.getSubTasks(), context);
            case PARALLEL:
                log.warn("并行执行模式暂未实现,降级为串行执行");
                return executeSequential(plan.getSubTasks(), context);
            case HIERARCHICAL:
                log.warn("层级执行模式暂未实现,降级为串行执行");
                return executeSequential(plan.getSubTasks(), context);
            default:
                log.error("未知的执行模式: {}", plan.getMode());
                return executeSequential(plan.getSubTasks(), context);
        }
    }

    /**
     * 串行执行策略
     * 按照依赖关系顺序执行子任务
     *
     * @param tasks   子任务列表
     * @param context 协同上下文
     * @return 执行结果列表
     */
    private List<SubTaskResult> executeSequential(List<SubTask> tasks, CollaborationContext context) {
        List<SubTaskResult> results = new ArrayList<>();

        // 拓扑排序,按依赖关系排序
        List<SubTask> sortedTasks = sortByDependencies(tasks);

        // 为任务分配执行顺序
        for (int i = 0; i < sortedTasks.size(); i++) {
            sortedTasks.get(i).setOrder(i + 1);
        }

        log.info("串行执行开始,任务总数: {}", sortedTasks.size());

        // 顺序执行每个子任务
        for (SubTask task : sortedTasks) {
            SubTaskResult result = executeSubTask(task, context);
            results.add(result);

            // 如果是必须任务且失败,则终止后续执行
            if (!task.isOptional() && result.isFailure()) {
                log.warn("必须任务执行失败,终止后续执行: taskId={}", task.getTaskId());

                // 将剩余任务标记为跳过
                for (SubTask remainingTask : sortedTasks) {
                    if (!results.stream().anyMatch(r -> r.getTaskId().equals(remainingTask.getTaskId()))) {
                        SubTaskResult skipped = new SubTaskResult();
                        skipped.setTaskId(remainingTask.getTaskId());
                        skipped.setAgentName(remainingTask.getAgentName());
                        skipped.setDescription("由于前置任务失败而跳过");
                        skipped.setResult(AgentResult.error("前置任务失败,跳过执行"));
                        results.add(skipped);
                    }
                }
                break;
            }
        }

        log.info("串行执行完成,成功: {}, 失败: {}",
                results.stream().filter(SubTaskResult::isSuccess).count(),
                results.stream().filter(SubTaskResult::isFailure).count());

        return results;
    }

    /**
     * 执行单个子任务
     *
     * @param task    子任务
     * @param context 协同上下文
     * @return 执行结果
     */
    private SubTaskResult executeSubTask(SubTask task, CollaborationContext context) {
        log.info("执行子任务开始: taskId={}, agent={}, type={}",
                task.getTaskId(), task.getAgentName(), task.getTaskType());

        LocalDateTime startTime = LocalDateTime.now();
        task.setStatus(SubTaskStatus.RUNNING);

        try {
            // 执行子任务(带重试)
            AgentResult result = executeWithRetry(task, context);

            // 记录执行时间
            long latencyMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();

            // 更新任务状态
            task.setStatus(result.isSuccess() ? SubTaskStatus.COMPLETED : SubTaskStatus.FAILED);
            task.setResult(result);

            // 保存结果到上下文
            context.putTaskResult(task.getTaskId(), result);

            // 记录执行历史
            SubTaskExecution execution = new SubTaskExecution(
                    task.getTaskId(),
                    task.getAgentName(),
                    startTime,
                    LocalDateTime.now(),
                    task.getStatus().name(),
                    latencyMs,
                    task.getRetryCount(),
                    result.isError() ? result.getSummary() : null
            );
            context.addExecutionHistory(execution);

            log.info("子任务执行完成: taskId={}, status={}, latency={}ms, retries={}",
                    task.getTaskId(), task.getStatus(), latencyMs, task.getRetryCount());

            // 创建结果对象
            SubTaskResult subTaskResult = new SubTaskResult(task, result);
            subTaskResult.setLatencyMs(latencyMs);

            return subTaskResult;

        } catch (Exception e) {
            log.error("子任务执行异常: taskId={}, error={}", task.getTaskId(), e.getMessage(), e);

            task.setStatus(SubTaskStatus.FAILED);
            AgentResult errorResult = AgentResult.error("子任务执行异常: " + e.getMessage());
            task.setResult(errorResult);

            // 保存错误结果到上下文
            context.putTaskResult(task.getTaskId(), errorResult);

            // 记录执行历史
            long latencyMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
            SubTaskExecution execution = new SubTaskExecution(
                    task.getTaskId(),
                    task.getAgentName(),
                    startTime,
                    LocalDateTime.now(),
                    SubTaskStatus.FAILED.name(),
                    latencyMs,
                    task.getRetryCount(),
                    e.getMessage()
            );
            context.addExecutionHistory(execution);

            return new SubTaskResult(task, errorResult);
        }
    }

    /**
     * 带重试的任务执行
     *
     * @param task    子任务
     * @param context 协同上下文
     * @return 执行结果
     */
    private AgentResult executeWithRetry(SubTask task, CollaborationContext context) {
        int maxRetries = task.getMaxRetries();

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                // 提取参数
                Map<String, Object> params = extractParams(task, context);

                // 创建AgentTask
                AgentTask agentTask = createAgentTask(task, params, context);

                // 执行Agent
                AgentResult result = routerAgentService.route(agentTask);

                // 如果成功或已达最大重试次数,返回结果
                if (result.isSuccess() || attempt == maxRetries) {
                    task.setRetryCount(attempt);
                    return result;
                }

                // 失败且未达最大重试次数,记录日志并重试
                log.warn("子任务执行失败,准备重试: taskId={}, attempt={}/{}, error={}",
                        task.getTaskId(), attempt + 1, maxRetries, result.getSummary());

                // 指数退避
                if (attempt < maxRetries) {
                    Thread.sleep(1000L * (attempt + 1));
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("重试被中断: taskId={}", task.getTaskId());
                return AgentResult.error("重试被中断");
            } catch (Exception e) {
                log.error("子任务执行异常: taskId={}, attempt={}/{}, error={}",
                        task.getTaskId(), attempt + 1, maxRetries, e.getMessage());

                if (attempt == maxRetries) {
                    return AgentResult.error("执行失败(已达最大重试次数): " + e.getMessage());
                }
            }
        }

        return AgentResult.error("执行失败");
    }

    /**
     * 从上下文中提取参数
     *
     * @param task    子任务
     * @param context 协同上下文
     * @return 参数Map
     */
    private Map<String, Object> extractParams(SubTask task, CollaborationContext context) {
        Map<String, Object> params = new HashMap<>();

        // 1. 添加静态参数
        if (task.getStaticParams() != null) {
            params.putAll(task.getStaticParams());
        }

        // 2. 从上下文中提取映射的参数
        if (task.getDataMappings() != null) {
            for (Map.Entry<String, String> mapping : task.getDataMappings().entrySet()) {
                String paramName = mapping.getKey();
                String sourcePath = mapping.getValue();

                Object value = context.extractData(sourcePath);
                if (value != null) {
                    params.put(paramName, value);
                } else {
                    log.warn("无法从上下文提取数据: paramName={}, sourcePath={}",
                            paramName, sourcePath);
                }
            }
        }

        return params;
    }

    /**
     * 创建AgentTask
     *
     * @param task    子任务
     * @param params  参数
     * @param context 协同上下文
     * @return AgentTask
     */
    private AgentTask createAgentTask(SubTask task, Map<String, Object> params,
                                      CollaborationContext context) {
        AgentTask agentTask = new AgentTask();
        agentTask.setTaskType(task.getTaskType());
        agentTask.setDomain(task.getDomain());

        // 设置payload
        params.forEach(agentTask::putPayload);

        // 添加原始用户输入
        if (context.getOriginalInput() != null) {
            agentTask.putPayload("originalInput", context.getOriginalInput());
        }

        // 设置上下文信息
        agentTask.putContext("parentTaskId", context.getParentTaskId());
        agentTask.putContext("subTaskId", task.getTaskId());
        agentTask.putContext("subTaskDescription", task.getDescription());
        agentTask.putContext("isSubTask", true);

        return agentTask;
    }

    /**
     * 拓扑排序 - 按依赖关系排序任务
     *
     * @param tasks 任务列表
     * @return 排序后的任务列表
     */
    private List<SubTask> sortByDependencies(List<SubTask> tasks) {
        // 构建依赖图和入度map
        Map<String, SubTask> taskMap = tasks.stream()
                .collect(Collectors.toMap(SubTask::getTaskId, t -> t));

        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> graph = new HashMap<>();

        // 初始化
        for (SubTask task : tasks) {
            String taskId = task.getTaskId();
            inDegree.put(taskId, task.getDependencies().size());

            // 构建依赖图(反向图)
            for (String dep : task.getDependencies()) {
                graph.computeIfAbsent(dep, k -> new ArrayList<>()).add(taskId);
            }
        }

        // 拓扑排序(Kahn算法)
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<SubTask> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String taskId = queue.poll();
            sorted.add(taskMap.get(taskId));

            // 处理依赖于当前任务的任务
            for (String next : graph.getOrDefault(taskId, Collections.emptyList())) {
                int newInDegree = inDegree.get(next) - 1;
                inDegree.put(next, newInDegree);
                if (newInDegree == 0) {
                    queue.offer(next);
                }
            }
        }

        // 检查是否所有任务都被排序(检测循环依赖)
        if (sorted.size() != tasks.size()) {
            log.error("存在循环依赖,未能完全排序任务");
            // 返回原始列表
            return tasks;
        }

        return sorted;
    }
}
