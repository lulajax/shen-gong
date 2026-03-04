package com.shengong.agentruntime.model.spec;

import java.util.Map;

/**
 * 工具规格定义，描述适配器类型、参数 Schema、默认值和运行配置。
 */
public record ToolSpec(
        String toolKey,
        String adapterType,
        Map<String, Object> inputSchema,
        Map<String, Object> defaults,
        Map<String, Object> config
) {
}
