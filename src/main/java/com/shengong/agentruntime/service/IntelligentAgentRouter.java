package com.shengong.agentruntime.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengong.agentruntime.controller.ChatController;
import com.shengong.agentruntime.core.agent.Agent;
import com.shengong.agentruntime.llm.LlmClient;
import com.shengong.agentruntime.model.AgentTask;
import com.shengong.agentruntime.model.ParamValidationResult;

import dev.langchain4j.data.message.ChatMessage;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final ParamExtractionService paramExtractionService;
    private final AgentPromptManager agentPromptManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据用户输入和上下文智能路由
     * 自动判断是否需要参数收集
     *
     * @param userInput 用户输入
     * @param context   上下文信息
     * @return AgentTask
     */
    public AgentTask routeFromUserInput(String userInput, Map<String, Object> context) {
        log.info("智能路由: 分析用户输入...");

        // 1. 获取所有可用的 Agent 信息
        List<Agent> availableAgents = (List<Agent>) agentRegistry.getAllAgents();

        // 2. 使用 LLM 分析用户意图并选择 Agent
        AgentSelection selection = analyzeAndSelectAgent(userInput, availableAgents, context);

        // 3. 创建 AgentTask
        AgentTask task = createAgentTask(selection, userInput, context);

        log.info("路由完成: taskType={}, domain={}, agent={}",
                task.getTaskType(), task.getDomain(), selection.getAgentName());

        // 4. 获取选中的 Agent
        Agent agent = agentRegistry.findAgent(task.getTaskType(), task.getDomain())
                .orElse(null);

        if (agent == null) {
            log.warn("未找到匹配的 Agent: taskType={}, domain={}", task.getTaskType(), task.getDomain());
            return task;
        }

        // 5. 参数收集与验证 (委托给 ParamExtractionService)
        ParamValidationResult paramResult = paramExtractionService.ensureParams(agent, task, userInput);

        if (!paramResult.isPassed()) {
            log.warn("参数收集未通过, 缺少参数: {}", paramResult.getMissingParams());
            task.putContext("paramCollectionFailed", true);
            task.putContext("missingParams", paramResult.getMissingParams());
            task.putContext("missingPrompt", paramResult.getMissingPrompt());
            task.putContext("paramValidationPassed", false);
        } else {
            log.info("参数收集完成，所有必填参数已就绪");
            task.putContext("paramValidationPassed", true);
        }

        return task;
    }

    /**
     * 批量处理对话历史,创建任务
     */
    public AgentTask routeFromConversation(List<ConversationMessage> conversation) {
        // 提取最新的用户消息和图片
        String latestUserMessage = "";        

        if (conversation != null && !conversation.isEmpty()) {
            ConversationMessage lastMsg = conversation.get(conversation.size() - 1);
            if ("user".equals(lastMsg.getRole())) {
                latestUserMessage = lastMsg.getTextContent();
            }
        }

        // 构建上下文
        Map<String, Object> context = new HashMap<>();
        context.put("conversationHistory", conversation);
        context.put("messageCount", conversation != null ? conversation.size() : 0);

        return routeFromUserInput(latestUserMessage, context);
    }
    

    /**
     * 生成友好的参数补充提示 (兼容旧 API, 实际上应该从 task context 获取)
     *
     * @param agent Agent 实例
     * @param missingParams 缺失的参数列表
     * @return 用户友好的提示信息
     */
    public String generateParamPrompt(Agent agent, List<String> missingParams) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请提供以下参数:\n");
        for (String param : missingParams) {
            prompt.append("- ").append(param).append("\n");
        }
        return prompt.toString();
    }

    /**
     * 使用 LLM 分析用户意图并选择 Agent
     */
    private AgentSelection analyzeAndSelectAgent(String userInput, List<Agent> agents, Map<String, Object> context) {
        try {
            String systemPrompt = agentPromptManager.buildSelectionPrompt(agents);

            // 构建包含历史记录的输入，以便 LLM 理解上下文
            StringBuilder fullInput = new StringBuilder();

            if (context != null && context.containsKey("conversationHistory")) {
                Object historyObj = context.get("conversationHistory");
                if (historyObj instanceof List) {
                    List<?> history = (List<?>) historyObj;
                    if (!history.isEmpty()) {
                        fullInput.append("对话历史上下文:\n");
                        // 只取最近的 10 条记录，避免 token 过多
                        int maxHistory = 10;
                        int start = Math.max(0, history.size() - maxHistory); // 简单的截断策略

                        // 遍历历史记录 (排除最后一条，因为它是当前的 userInput)
                        for (int i = start; i < history.size() - 1; i++) {
                            Object item = history.get(i);
                            if (item instanceof ConversationMessage) {
                                ConversationMessage msg = (ConversationMessage) item;
                                fullInput.append(String.format("%s: %s\n", msg.getRole(), msg.getTextContent()));
                            }
                        }
                        fullInput.append("\n当前用户输入: ");
                    }
                }
            }

            fullInput.append(userInput);

            String llmResponse = llmClient.chat(systemPrompt, fullInput.toString());
            return parseAgentSelection(llmResponse);
        } catch (Exception e) {
            log.warn("LLM 分析失败,使用默认路由: {}", e.getMessage());
            return getDefaultSelection();
        }
    }

    /**
     * 解析 LLM 的 Agent 选择结果
     */
    @SuppressWarnings("unchecked")
    private AgentSelection parseAgentSelection(String llmResponse) {
        try {
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
            
            // 显式将图片也放入 payload
            if (context.containsKey("images")) {
                task.putPayload("images", context.get("images"));
            }
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
        private String role;
        private List<ContentPart> content; // 强类型 List<ContentPart>
        private Long timestamp;

        @JsonCreator
        public ConversationMessage(@JsonProperty("role") String role, 
                                   @JsonProperty("content") List<ContentPart> content,
                                   @JsonProperty("timestamp") Long timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }

        public ConversationMessage() {}

        public String getTextContent() {
            if (content == null) return "";
            StringBuilder sb = new StringBuilder();
            for (ContentPart part : content) {
                if ("text".equals(part.getType()) && part.getText() != null) {
                    sb.append(part.getText());
                }
            }
            return sb.toString();
        }
    }
    
    /**
     * 消息内容部分
     */
    @Data
    public static class ContentPart {
        private String type; // text, image_url
        private String text;
        
        @JsonProperty("image_url")
        private ImageUrl imageUrl;
        
        @Data
        public static class ImageUrl {
            private String url; // base64 or http url
        }
    }
}
