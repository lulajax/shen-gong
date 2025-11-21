package com.shengong.agentruntime.core.tool;

import com.shengong.agentruntime.model.ToolResult;
import java.util.Map;

/**
 * Tool 接口
 * 所有工具必须实现此接口
 *
 * @author 神工团队
 * @since 1.0.0
 */
public interface Tool {

    /**
     * Tool 名称 (唯一标识)
     *
     * @return Tool 名称
     */
    String name();

    /**
     * Tool 描述 (用于 LLM 理解工具功能)
     *
     * @return 工具描述
     */
    String description();

    /**
     * 参数 Schema (JSON Schema 格式, 可选)
     *
     * @return 参数定义
     */
    default String parametersSchema() {
        return "{}";
    }

    /**
     * 执行工具
     *
     * @param arguments 参数
     * @return 执行结果
     */
    ToolResult invoke(Map<String, Object> arguments);

    /**
     * Tool 类别
     *
     * @return 类别 (database, http, cache, mcp, scraper等)
     */
    default String category() {
        return "general";
    }

    /**
     * 是否异步执行
     *
     * @return true 异步, false 同步
     */
    default boolean isAsync() {
        return false;
    }
}
