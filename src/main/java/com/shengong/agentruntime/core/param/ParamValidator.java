package com.shengong.agentruntime.core.param;

import com.shengong.agentruntime.core.agent.Agent;
import com.shengong.agentruntime.model.AgentTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数验证器
 * 验证 Agent 执行前的参数完整性
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
public class ParamValidator {

    /**
     * 验证参数并返回缺失的必填参数列表
     *
     * @param task  Agent 任务
     * @param agent Agent 实例
     * @return 缺失的必填参数列表，如果为空表示验证通过
     */
    public List<String> validateAndCollect(AgentTask task, Agent agent) {
        List<String> requiredParams = agent.requiredParams();
        if (requiredParams == null || requiredParams.isEmpty()) {
            log.debug("Agent {} has no required parameters", agent.name());
            return List.of();
        }

        List<String> missingParams = new ArrayList<>();

        for (String param : requiredParams) {
            if (!task.hasParam(param)) {
                missingParams.add(param);
                log.debug("Missing required parameter: {}", param);
            }
        }

        if (missingParams.isEmpty()) {
            log.debug("All required parameters present for Agent {}", agent.name());
        } else {
            log.warn("Agent {} missing required parameters: {}", agent.name(), missingParams);
        }

        return missingParams;
    }

    /**
     * 填充可选参数的默认值
     *
     * @param task  Agent 任务
     * @param agent Agent 实例
     */
    public void fillOptionalDefaults(AgentTask task, Agent agent) {
        List<String> optionalParams = agent.optionalParams();
        if (optionalParams == null || optionalParams.isEmpty()) {
            return;
        }

        // 注意：这里只是标记有可选参数，具体默认值由 Agent 自己通过 getParam(key, defaultValue) 处理
        log.debug("Agent {} has optional parameters: {}", agent.name(), optionalParams);
    }

    /**
     * 生成参数提示文本（用于 LLM 提取）
     *
     * @param agent Agent 实例
     * @return 参数提示文本
     */
    public String generateParamPrompt(Agent agent) {
        List<String> requiredParams = agent.requiredParams();
        List<String> optionalParams = agent.optionalParams();

        if ((requiredParams == null || requiredParams.isEmpty()) &&
            (optionalParams == null || optionalParams.isEmpty())) {
            return "";
        }

        StringBuilder prompt = new StringBuilder();

        if (requiredParams != null && !requiredParams.isEmpty()) {
            prompt.append("必填参数: ");
            for (String param : requiredParams) {
                prompt.append(param);
                String desc = agent.paramDescriptions().get(param);
                if (desc != null && !desc.isEmpty()) {
                    prompt.append(" (").append(desc).append(")");
                }
                prompt.append(", ");
            }
            prompt.setLength(prompt.length() - 2); // 移除最后的 ", "
            prompt.append("\n");
        }

        if (optionalParams != null && !optionalParams.isEmpty()) {
            prompt.append("可选参数: ");
            for (String param : optionalParams) {
                prompt.append(param);
                String desc = agent.paramDescriptions().get(param);
                if (desc != null && !desc.isEmpty()) {
                    prompt.append(" (").append(desc).append(")");
                }
                prompt.append(", ");
            }
            prompt.setLength(prompt.length() - 2);
        }

        return prompt.toString();
    }
}
