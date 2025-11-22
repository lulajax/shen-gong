package com.shengong.agentruntime.config;

import com.shengong.agentruntime.core.agent.Agent;
import com.shengong.agentruntime.core.agent.annotation.AgentDefinition;
import com.shengong.agentruntime.core.tool.Tool;
import com.shengong.agentruntime.core.tool.annotation.ToolDefinition;
import com.shengong.agentruntime.service.AgentRegistry;
import com.shengong.agentruntime.service.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public BeanPostProcessor agentAndToolPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(@org.springframework.lang.NonNull Object bean, @org.springframework.lang.NonNull String beanName) throws BeansException {
                // 自动注册 Agent
                if (bean.getClass().isAnnotationPresent(AgentDefinition.class)) {
                    if (bean instanceof Agent) {
                        Agent agent = (Agent) bean;
                        log.info("Found annotated agent: {} (bean: {})", agent.name(), beanName);
                        agentRegistry.register(agent);
                    } else {
                        log.warn("Bean {} annotated with @AgentDefinition but does not implement Agent interface", beanName);
                    }
                }

                // 自动注册 Tool
                if (bean.getClass().isAnnotationPresent(ToolDefinition.class)) {
                    if (bean instanceof Tool) {
                        Tool tool = (Tool) bean;
                        log.info("Found annotated tool: {} (bean: {})", tool.name(), beanName);
                        toolRegistry.register(tool);
                    } else {
                        log.warn("Bean {} annotated with @ToolDefinition but does not implement Tool interface", beanName);
                    }
                }

                return bean;
            }
        };
    }
}
