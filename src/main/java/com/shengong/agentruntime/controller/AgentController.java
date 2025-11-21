package com.shengong.agentruntime.controller;

import com.shengong.agentruntime.entity.TaskExecutionEntity;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import com.shengong.agentruntime.service.RouterAgentService;
import com.shengong.agentruntime.service.TaskExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Agent API 控制器
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
@Tag(name = "Agent API", description = "Agent 任务处理接口")
public class AgentController {

    private final RouterAgentService routerAgentService;
    private final TaskExecutionService taskExecutionService;

    /**
     * 处理 Agent 任务
     */
    @PostMapping("/handle")
    @Operation(summary = "处理任务", description = "提交任务给 Agent 处理")
    public ResponseEntity<AgentResult> handleTask(@RequestBody AgentTask task) {
        log.info("Received task: type={}, domain={}, taskId={}",
                task.getTaskType(), task.getDomain(), task.getTaskId());

        AgentResult result = routerAgentService.route(task);

        return ResponseEntity.ok(result);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查 Agent 服务状态")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok(" is running");
    }

    /**
     * 查询任务执行记录
     */
    @GetMapping("/task/{taskId}")
    @Operation(summary = "查询任务", description = "根据任务ID查询执行记录")
    public ResponseEntity<TaskExecutionEntity> getTask(@PathVariable String taskId) {
        log.info("Query task: taskId={}", taskId);

        Optional<TaskExecutionEntity> task = taskExecutionService.findByTaskId(taskId);

        return task.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 根据 traceId 查询所有相关任务
     */
    @GetMapping("/trace/{traceId}")
    @Operation(summary = "查询调用链", description = "根据traceId查询所有相关任务")
    public ResponseEntity<List<TaskExecutionEntity>> getTraceTask(@PathVariable String traceId) {
        log.info("Query trace: traceId={}", traceId);

        List<TaskExecutionEntity> tasks = taskExecutionService.findByTraceId(traceId);

        return ResponseEntity.ok(tasks);
    }
}
