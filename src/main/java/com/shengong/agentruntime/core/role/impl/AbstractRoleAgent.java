package com.shengong.agentruntime.core.role.impl;

import com.shengong.agentruntime.core.role.RoleAgent;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.runtime.RoleRequest;
import com.shengong.agentruntime.service.runtime.orchestration.CapabilityOrchestrator;

/**
 * 角色代理抽象基类，默认将请求委托给能力编排器处理。
 */
public abstract class AbstractRoleAgent implements RoleAgent {

    protected final CapabilityOrchestrator orchestrator;

    protected AbstractRoleAgent(CapabilityOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public AgentResult handle(RoleRequest request) {
        return orchestrator.executeRole(request);
    }
}
