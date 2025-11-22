package com.shengong.agentruntime.core.param;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Agent 参数注解
 * 用于在 DTO 字段上描述参数元数据，支持自动校验和 Prompt 生成
 *
 * @author 神工团队
 * @since 1.1.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentParam {

    /**
     * 是否必填
     */
    boolean required() default true;

    /**
     * 参数描述
     * 用于生成提取参数的 System Prompt
     */
    String description() default "";

    /**
     * 示例值
     * 用于生成 Few-shot Prompt 示例
     */
    String example() default "";

    /**
     * 默认值
     * 当参数缺失且非必填时使用
     */
    String defaultValue() default "";
}
