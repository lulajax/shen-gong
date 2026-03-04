package com.shengong.agentruntime.service.runtime.tool.adapter;

import com.shengong.agentruntime.model.ToolResult;
import com.shengong.agentruntime.model.spec.ToolSpec;
import java.util.Map;

/**
 * 工具适配器策略接口，按适配器类型执行具体工具调用。
 */
public interface ToolAdapter {

    String type();

    ToolResult execute(ToolSpec spec, Map<String, Object> args, Map<String, Object> context);
}
