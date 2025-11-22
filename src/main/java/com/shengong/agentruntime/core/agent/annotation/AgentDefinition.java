package com.shengong.agentruntime.core.agent.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Agent 定义注解
 * 用于自动注册 Agent，替代 AgentType 枚举
 *
 * @author 神工团队
 * @since 1.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component // 自动标记为 Spring Bean
public @interface AgentDefinition {

    /**
     * Agent 名称 (唯一标识)
     */
    String name();

    /**
     * 支持的业务领域列表
     */
    String[] domains();

    /**
     * 任务类型
     */
    String taskType();

    /**
     * Agent 描述
     */
    String description() default "";
}
