package com.shengong.agentruntime.service.runtime.registry;

import com.shengong.agentruntime.core.capability.CapabilityAgent;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 能力注册表，管理所有本地能力实现类的发现和查找。
 * 自动收集 Spring 容器中所有 {@link CapabilityAgent} 实现并按 capabilityKey 索引。
 */
@Service
public class CapabilityRegistry {

    private final Map<String, CapabilityAgent> capabilityMap;

    public CapabilityRegistry(List<CapabilityAgent> capabilityAgents) {
        Map<String, CapabilityAgent> index = new LinkedHashMap<>();
        for (CapabilityAgent capabilityAgent : capabilityAgents) {
            index.put(capabilityAgent.capabilityKey(), capabilityAgent);
        }
        this.capabilityMap = index;
    }

    public Optional<CapabilityAgent> get(String capabilityKey) {
        return Optional.ofNullable(capabilityMap.get(capabilityKey));
    }

    public Collection<CapabilityAgent> all() {
        return Collections.unmodifiableCollection(capabilityMap.values());
    }
}
