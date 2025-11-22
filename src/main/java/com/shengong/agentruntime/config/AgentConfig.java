package com.shengong.agentruntime.config;

import com.shengong.agentruntime.core.agent.Agent;
import com.shengong.agentruntime.core.tool.Tool;
import com.shengong.agentruntime.service.AgentRegistry;
import com.shengong.agentruntime.service.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

/**
 * Agent 自动配置
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AgentConfig {

    private final AgentRegistry agentRegistry;
    private final ToolRegistry toolRegistry;

    /**
     * 自动注册所有 Agent
     * 已被 AnnotationAgentRegistry 取代
     */
    // @Bean
    // public CommandLineRunner registerAgents(List<Agent> agents) {
    //     return args -> {
    //         log.info("Auto-registering {} agents", agents.size());
    //         agents.forEach(agentRegistry::register);
    //         log.info("Agent registration completed: total={}", agentRegistry.getCount());
    //     };
    // }

    /**
     * 自动注册所有 Tool
     */
    @Bean
    public CommandLineRunner registerTools(List<Tool> tools) {
        return args -> {
            log.info("Auto-registering {} tools", tools.size());
            tools.forEach(toolRegistry::register);
            log.info("Tool registration completed: total={}", toolRegistry.getCount());
        };
    }
}
