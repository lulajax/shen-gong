package com.shengong.agentruntime.model.collaboration;

import com.shengong.agentruntime.model.AgentResult;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多Agent协同上下文
 * 管理Agent间的数据传递和共享状态
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Data
public class CollaborationContext {
    /**
     * 父任务ID
     */
    private String parentTaskId;

    /**
     * 用户原始输入
     */
    private String originalInput;

    /**
     * 任务结果存储: taskId -> AgentResult
     * 使用ConcurrentHashMap保证线程安全(为并行执行做准备)
     */
    private Map<String, AgentResult> taskResults = new ConcurrentHashMap<>();

    /**
     * 共享数据存储: 任意键值对
     * 用于Agent间共享临时数据
     */
    private Map<String, Object> sharedData = new ConcurrentHashMap<>();

    /**
     * 执行历史记录
     */
    private List<SubTaskExecution> executionHistory = new ArrayList<>();

    /**
     * 上下文创建时间
     */
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 根据路径提取数据
     * 支持JSONPath风格的路径,例如: "task-1.data.metrics.gmv"
     *
     * @param path 数据路径
     * @return 提取的数据,如果不存在返回null
     */
    public Object extractData(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        if (parts.length == 0) {
            return null;
        }

        // 第一部分是taskId
        String taskId = parts[0];

        // 如果只有taskId,返回整个结果
        if (parts.length == 1) {
            return taskResults.get(taskId);
        }

        // 获取对应的任务结果
        AgentResult result = taskResults.get(taskId);
        if (result == null) {
            return null;
        }

        // 从结果中逐层提取数据
        Object current = null;

        // 第二部分通常是 "data", "summary", "status" 等
        if (parts.length > 1) {
            String field = parts[1];
            switch (field) {
                case "data":
                    current = result.getData();
                    break;
                case "summary":
                    return result.getSummary();
                case "status":
                    return result.getStatus();
                case "debug":
                    current = result.getDebug();
                    break;
                default:
                    // 尝试从data中获取
                    current = result.getData();
                    if (current instanceof Map) {
                        current = ((Map<?, ?>) current).get(field);
                    } else {
                        return null;
                    }
            }
        }

        // 继续提取嵌套字段
        for (int i = 2; i < parts.length; i++) {
            if (current == null) {
                return null;
            }

            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(parts[i]);
            } else {
                return null; // 无法继续提取
            }
        }

        return current;
    }

    /**
     * 保存任务结果
     *
     * @param taskId 任务ID
     * @param result 执行结果
     */
    public void putTaskResult(String taskId, AgentResult result) {
        this.taskResults.put(taskId, result);
    }

    /**
     * 获取任务结果
     *
     * @param taskId 任务ID
     * @return 执行结果
     */
    public AgentResult getTaskResult(String taskId) {
        return this.taskResults.get(taskId);
    }

    /**
     * 添加执行历史记录
     *
     * @param execution 执行记录
     */
    public void addExecutionHistory(SubTaskExecution execution) {
        this.executionHistory.add(execution);
    }

    /**
     * 设置共享数据
     *
     * @param key   键
     * @param value 值
     */
    public void putSharedData(String key, Object value) {
        this.sharedData.put(key, value);
    }

    /**
     * 获取共享数据
     *
     * @param key 键
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T getSharedData(String key) {
        return (T) this.sharedData.get(key);
    }

    /**
     * 检查任务结果是否存在
     *
     * @param taskId 任务ID
     * @return true if exists
     */
    public boolean hasTaskResult(String taskId) {
        return this.taskResults.containsKey(taskId);
    }

    /**
     * 获取已完成的任务数量
     *
     * @return 已完成的任务数
     */
    public int getCompletedTaskCount() {
        return taskResults.size();
    }

    /**
     * 获取执行历史数量
     *
     * @return 历史记录数
     */
    public int getExecutionHistoryCount() {
        return executionHistory.size();
    }
}
