package com.shengong.agentruntime.core.param;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 参数绑定器
 * 负责将 Map 转换为强类型 DTO，并执行基于注解的校验
 *
 * @author 神工团队
 * @since 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParamBinder {

    private final ObjectMapper objectMapper;

    /**
     * 绑定并验证参数
     *
     * @param payload   原始参数 Map
     * @param paramType 目标 DTO 类型
     * @param <T>       DTO 类型泛型
     * @return 填充好数据的 DTO 对象
     * @throws IllegalArgumentException 当必填参数缺失时抛出
     */
    public <T> T bindAndValidate(Map<String, Object> payload, Class<T> paramType) {
        // 1. 基础转换
        T params;
        try {
            params = objectMapper.convertValue(payload, paramType);
        } catch (Exception e) {
            log.error("参数类型转换失败: {}", e.getMessage());
            throw new IllegalArgumentException("Parameter binding failed: " + e.getMessage());
        }

        // 2. 校验 @AgentParam 约束
        validateConstraints(params, paramType);

        return params;
    }

    private <T> void validateConstraints(T params, Class<T> paramType) {
        for (Field field : paramType.getDeclaredFields()) {
            field.setAccessible(true);
            AgentParam annotation = field.getAnnotation(AgentParam.class);

            if (annotation == null) {
                continue;
            }

            try {
                Object value = field.get(params);

                // 检查必填
                if (annotation.required() && (value == null || value.toString().trim().isEmpty())) {
                    // 尝试使用默认值
                    if (!annotation.defaultValue().isEmpty()) {
                        // 这里简单处理字符串类型，复杂类型可能需要更复杂的转换逻辑
                        if (field.getType().equals(String.class)) {
                            field.set(params, annotation.defaultValue());
                            log.debug("使用了默认值填充参数 {}: {}", field.getName(), annotation.defaultValue());
                            continue;
                        }
                    }
                    throw new IllegalArgumentException("Missing required parameter: " + field.getName());
                }

            } catch (IllegalAccessException e) {
                log.error("无法访问字段 {}: {}", field.getName(), e.getMessage());
                throw new RuntimeException("Parameter validation error", e);
            }
        }
    }

    /**
     * 根据参数类生成 LLM 提取 Prompt
     *
     * @param paramType 参数 DTO 类
     * @return 格式化的参数描述文本
     */
    public String generateExtractionPrompt(Class<?> paramType) {
        StringBuilder prompt = new StringBuilder();

        for (Field field : paramType.getDeclaredFields()) {
            AgentParam meta = field.getAnnotation(AgentParam.class);
            if (meta == null) continue;

            String typeName = field.getType().getSimpleName();
            String desc = meta.description();
            String example = meta.example().isEmpty() ? "" : " (例如: " + meta.example() + ")";
            String required = meta.required() ? "[必填]" : "[可选]";

            prompt.append(String.format("- %s %s (%s): %s%s\n",
                required, field.getName(), typeName, desc, example));
        }

        return prompt.toString();
    }
}
