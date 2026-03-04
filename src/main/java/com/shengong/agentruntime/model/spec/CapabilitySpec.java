package com.shengong.agentruntime.model.spec;

import java.util.List;
import java.util.Map;

/**
 * 能力规格定义，包含提示词、执行方式、工具白名单和输入输出 Schema。
 */
public record CapabilitySpec(
        String capabilityKey,
        String description,
        String systemPrompt,
        String executionType,
        List<String> allowedTools,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        Map<String, Object> config
) {
}
