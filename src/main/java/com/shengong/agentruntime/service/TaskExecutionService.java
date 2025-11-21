package com.shengong.agentruntime.service;

import com.shengong.agentruntime.entity.TaskExecutionEntity;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import com.shengong.agentruntime.repository.TaskExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 任务执行记录服务
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutionService {

    private final TaskExecutionRepository taskExecutionRepository;

    /**
     * 保存任务开始执行的记录
     */
    @Transactional
    public TaskExecutionEntity saveTaskStart(AgentTask task, String agentName) {
        TaskExecutionEntity entity = TaskExecutionEntity.builder()
                .taskId(task.getTaskId())
                .taskType(task.getTaskType())
                .domain(task.getDomain())
                .agentName(agentName)
                .traceId(task.getTraceId())
                .status("running")
                .payload(task.getPayload())
                .userId(task.getUserId())
                .startedAt(LocalDateTime.now())
                .build();

        return taskExecutionRepository.save(entity);
    }

    /**
     * 更新任务执行结果
     */
    @Transactional
    public TaskExecutionEntity updateTaskResult(String taskId, AgentResult result) {
        Optional<TaskExecutionEntity> optEntity = taskExecutionRepository.findByTaskId(taskId);
        if (optEntity.isEmpty()) {
            log.warn("Task not found: {}", taskId);
            return null;
        }

        TaskExecutionEntity entity = optEntity.get();
        LocalDateTime completedAt = LocalDateTime.now();

        entity.setStatus(result.getStatus());
        entity.setSummary(result.getSummary());
        entity.setResult(result.getData());
        entity.setCompletedAt(completedAt);

        // 计算延迟
        if (entity.getStartedAt() != null) {
            Duration duration = Duration.between(entity.getStartedAt(), completedAt);
            entity.setLatencyMs((int) duration.toMillis());
        }

        // 如果有错误,保存错误信息
        if ("error".equals(result.getStatus()) && result.getData() != null) {
            Object errorObj = result.getData().get("error");
            if (errorObj != null) {
                entity.setErrorMessage(errorObj.toString());
            }
        }

        return taskExecutionRepository.save(entity);
    }

    /**
     * 保存完整的任务执行记录 (一步完成)
     */
    @Transactional
    public TaskExecutionEntity saveTaskExecution(AgentTask task, String agentName, AgentResult result) {
        LocalDateTime startedAt = LocalDateTime.now();
        LocalDateTime completedAt = LocalDateTime.now();

        TaskExecutionEntity entity = TaskExecutionEntity.builder()
                .taskId(task.getTaskId())
                .taskType(task.getTaskType())
                .domain(task.getDomain())
                .agentName(agentName)
                .traceId(task.getTraceId())
                .status(result.getStatus())
                .summary(result.getSummary())
                .payload(task.getPayload())
                .result(result.getData())
                .userId(task.getUserId())
                .startedAt(startedAt)
                .completedAt(completedAt)
                .latencyMs((int) Duration.between(startedAt, completedAt).toMillis())
                .build();

        // 如果有错误,保存错误信息
        if ("error".equals(result.getStatus()) && result.getData() != null) {
            Object errorObj = result.getData().get("error");
            if (errorObj != null) {
                entity.setErrorMessage(errorObj.toString());
            }
        }

        return taskExecutionRepository.save(entity);
    }

    /**
     * 根据 taskId 查询
     */
    public Optional<TaskExecutionEntity> findByTaskId(String taskId) {
        return taskExecutionRepository.findByTaskId(taskId);
    }

    /**
     * 根据 traceId 查询所有相关任务
     */
    public List<TaskExecutionEntity> findByTraceId(String traceId) {
        return taskExecutionRepository.findByTraceId(traceId);
    }

    /**
     * 根据用户 ID 分页查询
     */
    public Page<TaskExecutionEntity> findByUserId(String userId, Pageable pageable) {
        return taskExecutionRepository.findByUserId(userId, pageable);
    }

    /**
     * 查询最近的任务记录
     */
    public List<TaskExecutionEntity> findRecentTasks() {
        return taskExecutionRepository.findTop10ByOrderByStartedAtDesc();
    }

    /**
     * 根据时间范围查询
     */
    public List<TaskExecutionEntity> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return taskExecutionRepository.findByTimeRange(startTime, endTime);
    }

    /**
     * 统计时间范围内的执行次数
     */
    public long countByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return taskExecutionRepository.countByTimeRange(startTime, endTime);
    }

    /**
     * 统计失败任务数量
     */
    public long countFailedTasks() {
        return taskExecutionRepository.countByStatus("error");
    }

    /**
     * 获取 Agent 执行统计
     */
    public Map<String, Long> getAgentStatistics() {
        List<Object[]> results = taskExecutionRepository.countByAgentName();
        return results.stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
    }

    /**
     * 删除指定任务记录
     */
    @Transactional
    public void deleteByTaskId(String taskId) {
        taskExecutionRepository.findByTaskId(taskId)
                .ifPresent(taskExecutionRepository::delete);
    }

    /**
     * 批量删除旧记录
     */
    @Transactional
    public int deleteOldRecords(LocalDateTime before) {
        List<TaskExecutionEntity> oldRecords = taskExecutionRepository
                .findByTimeRange(LocalDateTime.MIN, before);

        taskExecutionRepository.deleteAll(oldRecords);
        return oldRecords.size();
    }
}
