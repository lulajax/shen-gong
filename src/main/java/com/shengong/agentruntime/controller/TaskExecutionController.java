package com.shengong.agentruntime.controller;

import com.shengong.agentruntime.entity.TaskExecutionEntity;
import com.shengong.agentruntime.service.TaskExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务执行记录控制器
 * 提供任务执行历史查询、统计等功能
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Task Execution API", description = "任务执行记录查询接口")
public class TaskExecutionController {

    private final TaskExecutionService taskExecutionService;

    /**
     * 查询最近的任务记录
     */
    @GetMapping("/recent")
    @Operation(summary = "最近任务", description = "查询最近10条任务执行记录")
    public ResponseEntity<List<TaskExecutionEntity>> getRecentTasks() {
        log.info("Query recent tasks");

        List<TaskExecutionEntity> tasks = taskExecutionService.findRecentTasks();

        return ResponseEntity.ok(tasks);
    }

    /**
     * 根据用户ID分页查询
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "用户任务历史", description = "分页查询用户的任务执行记录")
    public ResponseEntity<Page<TaskExecutionEntity>> getUserTasks(
            @PathVariable String userId,
            @Parameter(description = "页码(从0开始)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页记录数") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "startedAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        log.info("Query tasks for user: userId={}, page={}, size={}", userId, page, size);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<TaskExecutionEntity> tasks = taskExecutionService.findByUserId(userId, pageRequest);

        return ResponseEntity.ok(tasks);
    }

    /**
     * 根据时间范围查询
     */
    @GetMapping("/range")
    @Operation(summary = "时间范围查询", description = "查询指定时间范围内的任务记录")
    public ResponseEntity<List<TaskExecutionEntity>> getTasksByTimeRange(
            @Parameter(description = "开始时间 (格式: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间 (格式: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("Query tasks by time range: {} to {}", startTime, endTime);

        List<TaskExecutionEntity> tasks = taskExecutionService.findByTimeRange(startTime, endTime);

        return ResponseEntity.ok(tasks);
    }

    /**
     * 统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "执行统计", description = "获取任务执行统计信息")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @Parameter(description = "开始时间 (可选)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间 (可选)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("Query statistics: {} to {}", startTime, endTime);

        Map<String, Object> statistics = new HashMap<>();

        // Agent 执行统计
        statistics.put("agentStatistics", taskExecutionService.getAgentStatistics());

        // 失败任务数量
        statistics.put("failedTaskCount", taskExecutionService.countFailedTasks());

        // 时间范围内的总执行次数
        if (startTime != null && endTime != null) {
            long count = taskExecutionService.countByTimeRange(startTime, endTime);
            statistics.put("totalExecutions", count);
            statistics.put("timeRange", Map.of("start", startTime, "end", endTime));
        }

        return ResponseEntity.ok(statistics);
    }

    /**
     * 获取 Agent 执行统计
     */
    @GetMapping("/statistics/agents")
    @Operation(summary = "Agent统计", description = "获取各Agent的执行次数统计")
    public ResponseEntity<Map<String, Long>> getAgentStatistics() {
        log.info("Query agent statistics");

        Map<String, Long> statistics = taskExecutionService.getAgentStatistics();

        return ResponseEntity.ok(statistics);
    }

    /**
     * 删除指定任务记录
     */
    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除任务记录", description = "删除指定的任务执行记录")
    public ResponseEntity<Void> deleteTask(@PathVariable String taskId) {
        log.info("Delete task: taskId={}", taskId);

        taskExecutionService.deleteByTaskId(taskId);

        return ResponseEntity.noContent().build();
    }

    /**
     * 批量删除旧记录
     */
    @DeleteMapping("/cleanup")
    @Operation(summary = "清理旧记录", description = "删除指定时间之前的任务记录")
    public ResponseEntity<Map<String, Object>> cleanupOldRecords(
            @Parameter(description = "删除此时间之前的记录")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before) {

        log.info("Cleanup old records before: {}", before);

        int deletedCount = taskExecutionService.deleteOldRecords(before);

        Map<String, Object> response = new HashMap<>();
        response.put("deletedCount", deletedCount);
        response.put("before", before);

        return ResponseEntity.ok(response);
    }
}
