package com.shengong.agentruntime.service.runtime.capability;

import com.shengong.agentruntime.core.capability.CapabilityRuntime;
import com.shengong.agentruntime.model.runtime.CapabilityResult;
import com.shengong.agentruntime.model.spec.CapabilitySpec;

import java.util.Map;

/**
 * 能力执行策略接口，用于屏蔽本地执行与远程 A2A 执行差异。
 */
public interface CapabilityExecutor {

    boolean supports(CapabilitySpec spec);

    CapabilityResult execute(CapabilitySpec spec,
                             Map<String, Object> args,
                             Map<String, Object> context,
                             int depth,
                             CapabilityRuntime runtime);
}
