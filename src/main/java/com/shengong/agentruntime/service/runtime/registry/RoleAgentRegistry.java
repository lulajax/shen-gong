package com.shengong.agentruntime.service.runtime.registry;

import com.shengong.agentruntime.core.role.RoleAgent;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 角色 Agent 注册表，管理所有角色实现类的发现和查找。
 * 自动收集 Spring 容器中所有 {@link RoleAgent} 实现并按 roleKey 索引。
 */
@Service
public class RoleAgentRegistry {

    private final Map<String, RoleAgent> roleAgentMap;

    public RoleAgentRegistry(List<RoleAgent> roleAgents) {
        Map<String, RoleAgent> index = new LinkedHashMap<>();
        for (RoleAgent roleAgent : roleAgents) {
            index.put(roleAgent.roleKey(), roleAgent);
        }
        this.roleAgentMap = index;
    }

    public Optional<RoleAgent> find(String roleKey) {
        return Optional.ofNullable(roleAgentMap.get(roleKey));
    }
}
