package com.shengong.agentruntime.service;

import com.shengong.agentruntime.core.agent.Agent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 注册中心
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Service
public class AgentRegistry {

    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    private final Map<String, Set<Agent>> domainAgents = new ConcurrentHashMap<>();

    /**
     * 注册 Agent
     */
    public void register(Agent agent) {
        String name = agent.name();
        if (agents.containsKey(name)) {
            log.warn("Agent {} already registered, will be replaced", name);
        }

        agents.put(name, agent);

        // 按域注册
        for (String domain : agent.domains()) {
            domainAgents.computeIfAbsent(domain, k -> new HashSet<>()).add(agent);
        }

        log.info("Registered agent: {} (domains: {})", name, agent.domains());
    }

    /**
     * 根据名称获取 Agent
     */
    public Optional<Agent> getAgent(String name) {
        return Optional.ofNullable(agents.get(name));
    }

    /**
     * 查找支持指定任务的 Agent
     */
    public Optional<Agent> findAgent(String taskType, String domain) {
        Set<Agent> candidates = domainAgents.get(domain);
        if (candidates == null || candidates.isEmpty()) {
            log.debug("No agents found for domain: {}", domain);
            return Optional.empty();
        }

        return candidates.stream()
                .filter(agent -> agent.supports(taskType, domain))
                .findFirst();
    }

    /**
     * 获取所有 Agent
     */
    public Collection<Agent> getAllAgents() {
        return Collections.unmodifiableCollection(agents.values());
    }

    /**
     * 获取指定域的所有 Agent
     */
    public Set<Agent> getAgentsByDomain(String domain) {
        return domainAgents.getOrDefault(domain, Collections.emptySet());
    }

    /**
     * 获取注册数量
     */
    public int getCount() {
        return agents.size();
    }
}
