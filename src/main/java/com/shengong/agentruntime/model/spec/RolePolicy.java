package com.shengong.agentruntime.model.spec;

import java.util.List;
import java.util.Map;

/**
 * 角色策略定义，描述角色可用/禁用能力与数据范围。
 */
public record RolePolicy(
        String roleKey,
        List<String> allowedCapabilities,
        List<String> deniedCapabilities,
        Map<String, Object> dataScope
) {
}
