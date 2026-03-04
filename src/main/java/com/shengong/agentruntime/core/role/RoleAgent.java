package com.shengong.agentruntime.core.role;

import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.runtime.RoleRequest;

/**
 * 角色代理门面接口，统一承接外部请求并返回最终结果。
 */
public interface RoleAgent {

    String roleKey();

    AgentResult handle(RoleRequest request);
}
