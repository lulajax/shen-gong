package com.shengong.agentruntime.service.runtime.capability;

import com.shengong.agentruntime.core.capability.CapabilityRuntime;
import com.shengong.agentruntime.model.runtime.CapabilityRequest;
import com.shengong.agentruntime.model.runtime.CapabilityResult;
import com.shengong.agentruntime.model.spec.CapabilitySpec;
import com.shengong.agentruntime.service.runtime.registry.CapabilityRegistry;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 本地能力执行策略，实现对 Spring 容器内能力代理的调用。
 */
@Service
@Order(100)
public class LocalCapabilityExecutor implements CapabilityExecutor {

    private final CapabilityRegistry capabilityRegistry;

    public LocalCapabilityExecutor(CapabilityRegistry capabilityRegistry) {
        this.capabilityRegistry = capabilityRegistry;
    }

    @Override
    public boolean supports(CapabilitySpec spec) {
        if (spec == null) {
            return false;
        }
        if ("a2a".equalsIgnoreCase(spec.executionType())) {
            return false;
        }
        Map<String, Object> config = spec.config();
        if (config == null) {
            return true;
        }
        Object mode = config.get("mode");
        if (mode != null && "a2a".equalsIgnoreCase(String.valueOf(mode))) {
            return false;
        }
        return config.get("serverUrl") == null;
    }

    @Override
    public CapabilityResult execute(CapabilitySpec spec,
                                    Map<String, Object> args,
                                    Map<String, Object> context,
                                    int depth,
                                    CapabilityRuntime runtime) {
        var capability = capabilityRegistry.get(spec.capabilityKey()).orElse(null);
        if (capability == null) {
            return CapabilityResult.failure("CAPABILITY_NOT_FOUND",
                    "Capability bean not found: " + spec.capabilityKey());
        }

        CapabilityRequest request = new CapabilityRequest(
                spec.capabilityKey(),
                args != null ? args : Map.of(),
                context != null ? context : Map.of(),
                depth
        );
        CapabilityResult result = capability.execute(request, runtime);
        if (result == null) {
            return CapabilityResult.failure("CAPABILITY_EXECUTION_FAILED",
                    "Capability returned null result");
        }
        return result;
    }
}
