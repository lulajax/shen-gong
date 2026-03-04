package com.shengong.agentruntime.model.runtime;

import java.util.Map;

/**
 * 角色代理请求协议，承载用户输入、上下文和结构化参数。
 */
public record RoleRequest(
        String role,
        String userId,
        String sessionId,
        String inputText,
        Map<String, Object> context,
        Map<String, Object> payload
) {
}
