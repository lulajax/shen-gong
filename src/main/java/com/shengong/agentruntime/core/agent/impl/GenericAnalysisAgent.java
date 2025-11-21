package com.shengong.agentruntime.core.agent.impl;

import com.shengong.agentruntime.core.agent.Agent;
import com.shengong.agentruntime.core.agent.AgentType;
import com.shengong.agentruntime.llm.LlmClient;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/**
 * 通用分析 Agent
 * 支持通用领域的分析任务
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenericAnalysisAgent implements Agent {

    private static final AgentType AGENT_TYPE = AgentType.GENERIC_ANALYSIS;

    private final LlmClient llmClient;

    @Override
    public String name() {
        return AGENT_TYPE.getName();
    }
    
    @Override
    public String taskType() {
        return AGENT_TYPE.getTaskType();
    }

    @Override
    public List<String> domains() {
        return AGENT_TYPE.getDomains();
    }

    @Override
    public boolean supports(String taskType, String domain) {
        return AGENT_TYPE.supports(taskType, domain);
    }

    @Override
    public List<String> requiredParams() {
        return List.of("text");
    }

    @Override
    public Map<String, String> paramDescriptions() {
        return Map.of(
            "text", "用户输入的文本内容，需要进行分析"
        );
    }

    @Override
    public AgentResult handle(AgentTask task) {
        log.info("GenericAnalysisAgent handling task: {}", task.getTaskId());

        try {
            // 统一参数验证
            AgentResult validationError = validateParams(task);
            if (validationError != null) {
                return validationError;
            }

            // 获取用户输入
            String userText = task.getParam("text");

            // 调用 LLM 分析
            String systemPrompt = "你是一个专业分析助手，请根据用户输入总结重点，提取关键信息。";
            String response = llmClient.chat(systemPrompt, userText);

            // 构造结果
            return AgentResult.ok(response, Map.of(
                    "raw_input", userText,
                    "analysis", response
            ));

        } catch (Exception e) {
            log.error("GenericAnalysisAgent failed: {}", e.getMessage(), e);
            return AgentResult.error("Analysis failed: " + e.getMessage());
        }
    }

    @Override
    public String description() {
        return AGENT_TYPE.getDescription();
    }
}
