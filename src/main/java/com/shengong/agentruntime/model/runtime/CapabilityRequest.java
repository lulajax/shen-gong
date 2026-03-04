package com.shengong.agentruntime.model.runtime;

import java.util.Map;

/**
 * 能力调用请求协议，包含能力标识、参数、上下文与调用深度。
 */
public record CapabilityRequest(
        String capability,
        Map<String, Object> args,
        Map<String, Object> context,
        int depth
) {
}
