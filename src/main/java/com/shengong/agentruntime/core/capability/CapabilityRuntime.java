package com.shengong.agentruntime.core.capability;

import com.shengong.agentruntime.model.ToolResult;
import com.shengong.agentruntime.model.runtime.CapabilityResult;
import java.util.Map;

/**
 * 能力运行时网关，提供能力互调与工具调用入口。
 */
public interface CapabilityRuntime {

    CapabilityResult callCapability(String capabilityKey, Map<String, Object> args,
                                    Map<String, Object> context, int depth);

    ToolResult callTool(String toolKey, Map<String, Object> args, Map<String, Object> context);
}
