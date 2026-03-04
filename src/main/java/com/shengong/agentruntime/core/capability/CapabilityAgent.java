package com.shengong.agentruntime.core.capability;

import com.shengong.agentruntime.model.runtime.CapabilityRequest;
import com.shengong.agentruntime.model.runtime.CapabilityResult;

/**
 * 能力代理接口，定义可被角色代理编排执行的能力单元。
 */
public interface CapabilityAgent {

    String capabilityKey();

    CapabilityResult execute(CapabilityRequest request, CapabilityRuntime runtime);
}
