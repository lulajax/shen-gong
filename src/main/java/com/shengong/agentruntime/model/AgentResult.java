package com.shengong.agentruntime.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 执行结果模型
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Data
public class AgentResult {

    /**
     * 执行状态: ok, error, partial
     */
    private String status;

    /**
     * 结果摘要
     */
    private String summary;

    /**
     * 结果数据
     */
    private Map<String, Object> data = new HashMap<>();

    /**
     * 调试信息
     */
    private Map<String, Object> debug = new HashMap<>();

    /**
     * 错误列表
     */
    private List<String> errors = new ArrayList<>();

    /**
     * 执行耗时(毫秒)
     */
    private Long latencyMs;

    /**
     * Agent 名称
     */
    private String agentName;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt = LocalDateTime.now();

    /**
     * 创建成功结果
     */
    public static AgentResult ok(String summary, Map<String, Object> data) {
        AgentResult result = new AgentResult();
        result.setStatus("ok");
        result.setSummary(summary);
        result.setData(data != null ? data : new HashMap<>());
        return result;
    }

    /**
     * 创建成功结果(仅摘要)
     */
    public static AgentResult ok(String summary) {
        return ok(summary, new HashMap<>());
    }

    /**
     * 创建错误结果
     */
    public static AgentResult error(String errorMessage) {
        AgentResult result = new AgentResult();
        result.setStatus("error");
        result.setSummary(errorMessage);
        result.getErrors().add(errorMessage);
        return result;
    }

    /**
     * 创建部分成功结果
     */
    public static AgentResult partial(String summary, Map<String, Object> data, List<String> errors) {
        AgentResult result = new AgentResult();
        result.setStatus("partial");
        result.setSummary(summary);
        result.setData(data != null ? data : new HashMap<>());
        result.setErrors(errors != null ? errors : new ArrayList<>());
        return result;
    }

    /**
     * 添加数据
     */
    public AgentResult addData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    /**
     * 添加调试信息
     */
    public AgentResult addDebug(String key, Object value) {
        this.debug.put(key, value);
        return this;
    }

    /**
     * 添加错误
     */
    public AgentResult addError(String error) {
        this.errors.add(error);
        return this;
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return "ok".equals(status);
    }

    /**
     * 判断是否失败
     */
    public boolean isError() {
        return "error".equals(status);
    }
}
