package com.shengong.agentruntime.service;

import com.shengong.agentruntime.core.agent.Agent;
import com.shengong.agentruntime.core.agent.annotation.AgentDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * 注解驱动的 Agent 注册中心
 * 自动扫描并注册带有 @AgentDefinition 的 Bean
 *
 * @author 神工团队
 * @since 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnnotationAgentRegistry implements BeanPostProcessor {

    private final AgentRegistry agentRegistry;

    @Override
    public Object postProcessAfterInitialization(@org.springframework.lang.NonNull Object bean, @org.springframework.lang.NonNull String beanName) throws BeansException {
        // 检查类上是否有 @AgentDefinition 注解
        if (bean.getClass().isAnnotationPresent(AgentDefinition.class)) {
            if (bean instanceof Agent) {
                Agent agent = (Agent) bean;
                log.info("Found annotated agent: {} (bean: {})", agent.name(), beanName);
                agentRegistry.register(agent);
            } else {
                log.warn("Bean {} annotated with @AgentDefinition but does not implement Agent interface", beanName);
            }
        }
        return bean;
    }
}

