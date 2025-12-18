package com.shengong.agentruntime.model.collaboration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 执行计划模型
 * 描述多Agent协同任务的执行计划
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Data
public class ExecutionPlan {
    /**
     * 计划唯一标识
     */
    private String planId = "plan-" + UUID.randomUUID().toString().substring(0, 8);

    /**
     * 执行模式
     */
    private ExecutionMode mode = ExecutionMode.SEQUENTIAL;

    /**
     * 子任务列表
     */
    private List<SubTask> subTasks = new ArrayList<>();

    /**
     * 为什么需要多Agent协同的原因
     */
    private String reason;

    /**
     * 计划创建时间
     */
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 预期执行时长(毫秒)
     */
    private Long estimatedDuration;

    /**
     * 验证计划合法性
     * 检查: 1) Agent是否存在 2) 依赖关系是否合法 3) 是否有循环依赖
     *
     * @return true if valid
     */
    @JsonIgnore
    public boolean isValid() {
        if (subTasks == null || subTasks.isEmpty()) {
            return false;
        }

        // 检查循环依赖
        return !hasCyclicDependency();
    }

    /**
     * 检测是否存在循环依赖
     *
     * @return true if cyclic dependency exists
     */
    @JsonIgnore
    private boolean hasCyclicDependency() {
        Map<String, List<String>> graph = new HashMap<>();
        Set<String> allTasks = new HashSet<>();

        // 构建依赖图
        for (SubTask task : subTasks) {
            allTasks.add(task.getTaskId());
            graph.put(task.getTaskId(), task.getDependencies());
        }

        // DFS检测环
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String taskId : allTasks) {
            if (hasCycleDFS(taskId, graph, visited, recursionStack)) {
                return true;
            }
        }

        return false;
    }

    /**
     * DFS检测环
     */
    @JsonIgnore
    private boolean hasCycleDFS(String taskId, Map<String, List<String>> graph,
                                Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(taskId)) {
            return true; // 发现环
        }

        if (visited.contains(taskId)) {
            return false; // 已访问过,无需再次检查
        }

        visited.add(taskId);
        recursionStack.add(taskId);

        List<String> dependencies = graph.getOrDefault(taskId, new ArrayList<>());
        for (String dep : dependencies) {
            if (hasCycleDFS(dep, graph, visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(taskId);
        return false;
    }

    /**
     * 获取任务数量
     */
    @JsonIgnore
    public int getTaskCount() {
        return subTasks != null ? subTasks.size() : 0;
    }
}
