package com.shengong.agentruntime.model.collaboration;

import com.shengong.agentruntime.model.AgentResult;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 子任务模型
 * 表示多Agent协同中的一个子任务
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Data
public class SubTask {
    /**
     * 子任务唯一标识
     */
    private String taskId;

    /**
     * Agent名称
     */
    private String agentName;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 业务域
     */
    private String domain;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 依赖的任务ID列表
     * 例如: ["task-1", "task-2"] 表示当前任务依赖于task-1和task-2
     */
    private List<String> dependencies = new ArrayList<>();

    /**
     * 参数映射: 参数名 -> 数据路径
     * 例如: {"rawData": "task-1.data.liveMetrics"}
     * 表示从task-1的结果中提取data.liveMetrics作为rawData参数
     */
    private Map<String, String> dataMappings = new HashMap<>();

    /**
     * 静态参数
     * 在任务分解时直接指定的参数,不依赖其他任务的输出
     */
    private Map<String, Object> staticParams = new HashMap<>();

    /**
     * 执行状态
     */
    private SubTaskStatus status = SubTaskStatus.PENDING;

    /**
     * 执行结果
     */
    private AgentResult result;

    /**
     * 重试配置: 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 当前重试次数
     */
    private int retryCount = 0;

    /**
     * 是否可选任务
     * 可选任务失败不会导致整个协同任务失败
     */
    private boolean optional = false;

    /**
     * 任务执行顺序(用于记录)
     */
    private Integer order;
}
