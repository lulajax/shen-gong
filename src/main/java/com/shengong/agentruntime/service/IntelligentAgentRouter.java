package com.shengong.agentruntime.service;

import com.shengong.agentruntime.llm.LlmClient;
import com.shengong.agentruntime.model.AgentTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能 Agent 路由器
 * 根据用户输入自动选择合适的 Agent 并创建 AgentTask
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntelligentAgentRouter {

    private final AgentRegistry agentRegistry;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据用户输入智能路由到合适的 Agent
     *
     * @param userInput 用户输入
     * @return AgentTask
     */
    public AgentTask routeFromUserInput(String userInput) {
        return routeFromUserInput(userInput, null);
    }

    /**
     * 根据用户输入和上下文智能路由
     *
     * @param userInput 用户输入
     * @param context   上下文信息
     * @return AgentTask
     */
    public AgentTask routeFromUserInput(String userInput, Map<String, Object> context) {
        log.info("智能路由: 分析用户输入...");

        // 1. 获取所有可用的 Agent 信息
        List<AgentInfo> availableAgents = getAvailableAgents();

        // 2. 使用 LLM 分析用户意图并选择 Agent
        AgentSelection selection = analyzeAndSelectAgent(userInput, availableAgents);

        // 3. 创建 AgentTask
        AgentTask task = createAgentTask(selection, userInput, context);

        log.info("路由完成: taskType={}, domain={}, agent={}",
                task.getTaskType(), task.getDomain(), selection.getAgentName());

        return task;
    }

    /**
     * 批量处理对话历史,创建任务
     */
    public AgentTask routeFromConversation(List<ConversationMessage> conversation) {
        // 提取最新的用户消息
        String latestUserMessage = conversation.stream()
                .filter(msg -> "user".equals(msg.getRole()))
                .reduce((first, second) -> second)
                .map(ConversationMessage::getContent)
                .orElse("");

        // 构建上下文
        Map<String, Object> context = new HashMap<>();
        context.put("conversationHistory", conversation);
        context.put("messageCount", conversation.size());

        return routeFromUserInput(latestUserMessage, context);
    }

    /**
     * 获取所有可用的 Agent 信息
     */
    private List<AgentInfo> getAvailableAgents() {
        return agentRegistry.getAllAgents().stream()
                .map(agent -> {
                    AgentInfo info = new AgentInfo();
                    info.setName(agent.name());
                    info.setDomains(agent.domains());
                    info.setDescription(agent.description());
                    return info;
                })
                .collect(Collectors.toList());
    }

    /**
     * 使用 LLM 分析用户意图并选择 Agent
     */
    private AgentSelection analyzeAndSelectAgent(String userInput, List<AgentInfo> agents) {
        try {
            String systemPrompt = buildSelectionPrompt(agents);
            String llmResponse = llmClient.chat(systemPrompt, userInput);

            // 解析 LLM 响应
            return parseAgentSelection(llmResponse, agents);

        } catch (Exception e) {
            log.warn("LLM 分析失败,使用默认路由: {}", e.getMessage());
            return getDefaultSelection();
        }
    }

    /**
     * 构建 Agent 选择的 Prompt
     */
    private String buildSelectionPrompt(List<AgentInfo> agents) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个智能 Agent 路由器。根据用户输入,从以下可用的 Agent 中选择最合适的一个。\n\n");
        prompt.append("可用的 Agent:\n");

        for (int i = 0; i < agents.size(); i++) {
            AgentInfo agent = agents.get(i);
            prompt.append(String.format("%d. %s\n", i + 1, agent.getName()));
            prompt.append(String.format("   - 领域: %s\n", String.join(", ", agent.getDomains())));
            prompt.append(String.format("   - 描述: %s\n", agent.getDescription()));
            prompt.append("\n");
        }

        prompt.append("请分析用户输入,返回以下 JSON 格式:\n");
        prompt.append("{\n");
        prompt.append("  \"agentName\": \"选择的 Agent 名称\",\n");
        prompt.append("  \"taskType\": \"任务类型 (如: analysis, analysis_report, anomaly_detection 等)\",\n");
        prompt.append("  \"domain\": \"业务领域 (如: generic, live, order, data 等)\",\n");
        prompt.append("  \"confidence\": 0.95,\n");
        prompt.append("  \"reason\": \"选择理由\",\n");
        prompt.append("  \"extractedParams\": {\n");
        prompt.append("    \"key\": \"从用户输入中提取的参数\"\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");
        prompt.append("如果用户输入不明确,选择 GenericAnalysisAgent。");

        return prompt.toString();
    }

    /**
     * 解析 LLM 的 Agent 选择结果
     */
    @SuppressWarnings("unchecked")
    private AgentSelection parseAgentSelection(String llmResponse, List<AgentInfo> agents) {
        try {
            // 提取 JSON 内容
            String jsonContent = extractJsonFromResponse(llmResponse);

            Map<String, Object> selectionMap = objectMapper.readValue(jsonContent, Map.class);

            AgentSelection selection = new AgentSelection();
            selection.setAgentName((String) selectionMap.get("agentName"));
            selection.setTaskType((String) selectionMap.get("taskType"));
            selection.setDomain((String) selectionMap.get("domain"));
            selection.setConfidence(((Number) selectionMap.getOrDefault("confidence", 0.8)).doubleValue());
            selection.setReason((String) selectionMap.getOrDefault("reason", ""));
            selection.setExtractedParams((Map<String, Object>) selectionMap.getOrDefault("extractedParams", new HashMap<>()));

            return selection;

        } catch (Exception e) {
            log.error("解析 Agent 选择失败: {}", e.getMessage());
            return getDefaultSelection();
        }
    }

    /**
     * 从响应中提取 JSON
     */
    private String extractJsonFromResponse(String response) {
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        } else if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        } else if (response.trim().startsWith("{")) {
            // 直接是 JSON
            return response.trim();
        }

        return response;
    }

    /**
     * 创建 AgentTask
     */
    private AgentTask createAgentTask(AgentSelection selection, String userInput,
                                      Map<String, Object> context) {
        AgentTask task = new AgentTask();
        task.setTaskType(selection.getTaskType());
        task.setDomain(selection.getDomain());

        // 设置 payload
        task.putPayload("text", userInput);
        task.putPayload("originalInput", userInput);

        // 添加 LLM 提取的参数
        if (selection.getExtractedParams() != null && !selection.getExtractedParams().isEmpty()) {
            selection.getExtractedParams().forEach(task::putPayload);
        }

        // 设置上下文
        if (context != null) {
            context.forEach(task::putContext);
        }

        // 添加路由信息到 context
        task.putContext("routingInfo", Map.of(
                "selectedAgent", selection.getAgentName(),
                "confidence", selection.getConfidence(),
                "reason", selection.getReason()
        ));

        return task;
    }

    /**
     * 获取默认的 Agent 选择
     */
    private AgentSelection getDefaultSelection() {
        AgentSelection selection = new AgentSelection();
        selection.setAgentName("GenericAnalysisAgent");
        selection.setTaskType("analysis");
        selection.setDomain("generic");
        selection.setConfidence(0.5);
        selection.setReason("默认路由到通用分析 Agent");
        selection.setExtractedParams(new HashMap<>());
        return selection;
    }

    /**
     * Agent 信息
     */
    @Data
    private static class AgentInfo {
        private String name;
        private List<String> domains;
        private String description;
    }

    /**
     * Agent 选择结果
     */
    @Data
    private static class AgentSelection {
        private String agentName;
        private String taskType;
        private String domain;
        private double confidence;
        private String reason;
        private Map<String, Object> extractedParams;
    }

    /**
     * 对话消息
     */
    @Data
    public static class ConversationMessage {
        private String role;     // user / assistant / system
        private String content;  // 消息内容
        private Long timestamp;  // 时间戳
    }
}
