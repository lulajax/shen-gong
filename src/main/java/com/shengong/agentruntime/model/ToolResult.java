package com.shengong.agentruntime.model;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * Tool 执行结果模型
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Data
public class ToolResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 结果数据
     */
    private Map<String, Object> data = new HashMap<>();

    /**
     * 错误信息
     */
    private String error;

    /**
     * 执行耗时(毫秒)
     */
    private Long latencyMs;

    /**
     * Tool 名称
     */
    private String toolName;

    /**
     * 创建成功结果
     */
    public static ToolResult success(Map<String, Object> data) {
        ToolResult result = new ToolResult();
        result.setSuccess(true);
        result.setData(data != null ? data : new HashMap<>());
        return result;
    }

    /**
     * 创建失败结果
     */
    public static ToolResult failure(String error) {
        ToolResult result = new ToolResult();
        result.setSuccess(false);
        result.setError(error);
        return result;
    }

    /**
     * 添加数据
     */
    public ToolResult addData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
}
