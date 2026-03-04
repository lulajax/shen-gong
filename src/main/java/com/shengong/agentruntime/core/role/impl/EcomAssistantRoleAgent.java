package com.shengong.agentruntime.core.role.impl;

import com.shengong.agentruntime.service.runtime.orchestration.CapabilityOrchestrator;
import org.springframework.stereotype.Component;

/**
 * 电商小助理角色代理，实现单角色门面入口。
 */
@Component
public class EcomAssistantRoleAgent extends AbstractRoleAgent {

    public EcomAssistantRoleAgent(CapabilityOrchestrator orchestrator) {
        super(orchestrator);
    }

    @Override
    public String roleKey() {
        return "ecom_assistant";
    }
}
