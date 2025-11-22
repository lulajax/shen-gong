package com.shengong.agentruntime.core.agent;

import com.shengong.agentruntime.core.agent.annotation.AgentDefinition;
import com.shengong.agentruntime.core.param.AgentParam;
import com.shengong.agentruntime.core.param.ParamBinder;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Agent 抽象基类
 * 提供了基于注解的元数据读取和参数绑定功能
 *
 * @param <T> 参数对象类型
 * @author 神工团队
 * @since 1.1.0
 */
@Slf4j
public abstract class AbstractAgent<T> implements Agent {

    private final Class<T> paramType;
    private final AgentDefinition definition;

    @Autowired
    protected ParamBinder paramBinder;

    protected AbstractAgent(Class<T> paramType) {
        this.paramType = paramType;
        this.definition = this.getClass().getAnnotation(AgentDefinition.class);
        if (this.definition == null) {
            throw new IllegalStateException("Agent class " + this.getClass().getName() +
                                         " must be annotated with @AgentDefinition");
        }
    }

    /**
     * 执行业务逻辑
     *
     * @param task   任务上下文
     * @param params 已绑定并验证的参数对象
     * @return 执行结果
     */
    protected abstract AgentResult execute(AgentTask task, T params);

    @Override
    public AgentResult handle(AgentTask task) {
        try {
            // 1. 绑定参数
            T params = paramBinder.bindAndValidate(task.getPayload(), paramType);

            // 2. 执行业务
            return execute(task, params);

        } catch (IllegalArgumentException e) {
            log.warn("Agent {} 参数验证失败: {}", name(), e.getMessage());
            return AgentResult.error("参数验证失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("Agent {} 执行异常: {}", name(), e.getMessage(), e);
            return AgentResult.error("系统内部错误: " + e.getMessage());
        }
    }

    // --- 元数据方法实现 ---

    @Override
    public String name() {
        return definition.name();
    }

    @Override
    public List<String> domains() {
        return Arrays.asList(definition.domains());
    }

    @Override
    public String taskType() {
        return definition.taskType();
    }

    @Override
    public boolean supports(String taskType, String domain) {
        return this.taskType().equals(taskType) && this.domains().contains(domain);
    }

    @Override
    public String description() {
        return definition.description().isEmpty() ? Agent.super.description() : definition.description();
    }

    // --- 兼容旧版参数验证系统的实现 ---

    @Override
    public List<String> requiredParams() {
        List<String> required = new ArrayList<>();
        for (Field field : paramType.getDeclaredFields()) {
            AgentParam meta = field.getAnnotation(AgentParam.class);
            if (meta != null && meta.required()) {
                required.add(field.getName());
            }
        }
        return required;
    }

    @Override
    public List<String> optionalParams() {
        List<String> optional = new ArrayList<>();
        for (Field field : paramType.getDeclaredFields()) {
            AgentParam meta = field.getAnnotation(AgentParam.class);
            if (meta != null && !meta.required()) {
                optional.add(field.getName());
            }
        }
        return optional;
    }

    @Override
    public Map<String, String> paramDescriptions() {
        Map<String, String> descs = new HashMap<>();
        for (Field field : paramType.getDeclaredFields()) {
            AgentParam meta = field.getAnnotation(AgentParam.class);
            if (meta != null) {
                descs.put(field.getName(), meta.description());
            }
        }
        return descs;
    }

    @Override
    public Class<?> getParamType() {
        return paramType;
    }
}
