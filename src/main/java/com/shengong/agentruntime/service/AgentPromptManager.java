package com.shengong.agentruntime.service;

import com.shengong.agentruntime.core.agent.Agent;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent Prompt 管理器
 * 负责构建路由选择 Prompt
 *
 * @author 神工团队
 * @since 1.1.0
 */
@Service
public class AgentPromptManager {

    /**
     * 构建 Agent 选择 Prompt
     *
     * @param agents 可用 Agent 列表
     * @return Prompt 字符串
     */
    public String buildSelectionPrompt(List<Agent> agents) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个智能 Agent 路由器。根据用户输入和对话历史,从以下可用的 Agent 中选择最合适的一个。\n\n");
        prompt.append("可用的 Agent:\n");

        for (int i = 0; i < agents.size(); i++) {
            Agent agent = agents.get(i);
            prompt.append(String.format("%d. %s\n", i + 1, agent.name()));
            prompt.append(String.format("   - 领域: %s\n", String.join(", ", agent.domains())));
            prompt.append(String.format("   - 任务类型: %s\n", agent.taskType()));
            prompt.append(String.format("   - 描述: %s\n", agent.description()));
            prompt.append("\n");
        }

        prompt.append("请分析用户输入,返回以下 JSON 格式:\n");
        prompt.append("{\n");
        prompt.append("  \"agentName\": \"选择的 Agent 名称\",\n");
        prompt.append("  \"taskType\": \"任务类型\",\n");
        prompt.append("  \"domain\": \"业务领域\",\n");
        prompt.append("  \"confidence\": 0.95,\n");
        prompt.append("  \"reason\": \"选择理由\",\n");
        prompt.append("  \"extractedParams\": {\n");
        prompt.append("    \"key\": \"从用户输入中提取的参数\"\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");
        prompt.append("如果用户输入不明确,选择 GenericAnalysisAgent。");

        return prompt.toString();
    }
}

