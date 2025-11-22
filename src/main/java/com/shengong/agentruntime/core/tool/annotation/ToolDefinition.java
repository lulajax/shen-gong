package com.shengong.agentruntime.core.tool.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tool 定义注解
 * 用于自动注册 Tool，替代 ToolType 枚举
 *
 * @author 神工团队
 * @since 1.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component // 自动标记为 Spring Bean
public @interface ToolDefinition {

    /**
     * Tool 名称 (唯一标识)
     */
    String name();

    /**
     * Tool 描述 (用于 LLM 理解工具功能)
     */
    String description() default "";

    /**
     * Tool 类别
     */
    String category() default "general";
}
