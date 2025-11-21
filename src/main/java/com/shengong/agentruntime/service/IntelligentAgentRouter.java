package com.shengong.agentruntime.service;

import com.shengong.agentruntime.core.agent.Agent;
import com.shengong.agentruntime.core.param.ParamValidator;
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
    private final ParamValidator paramValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据用户输入智能路由到合适的 Agent
     * 自动判断是否需要参数收集
     *
     * @param userInput 用户输入
     * @return AgentTask
     */
    public AgentTask routeFromUserInput(String userInput) {
        return routeFromUserInput(userInput, null);
    }

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
        List<AgentInfo> availableAgents = getAvailableAgents();

        // 2. 使用 LLM 分析用户意图并选择 Agent
        AgentSelection selection = analyzeAndSelectAgent(userInput, availableAgents);

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

        // 5. 检查 Agent 是否需要参数（有 requiredParams）
        List<String> requiredParams = agent.requiredParams();
        if (requiredParams == null || requiredParams.isEmpty()) {
            // 无需参数收集，直接返回
            log.debug("Agent {} 无需参数收集", agent.name());
            task.putContext("paramValidationPassed", true);
            return task;
        }

        // 6. 需要参数，进行自动收集和验证
        log.debug("Agent {} 需要参数: {}", agent.name(), requiredParams);
        return collectAndValidateParams(task, agent, userInput);
    }

    /**
     * 收集和验证参数
     */
    private AgentTask collectAndValidateParams(AgentTask task, Agent agent, String userInput) {
        // 1. 验证参数完整性
        List<String> missingParams = paramValidator.validateAndCollect(task, agent);

        // 2. 如果有缺失参数，尝试从用户输入中提取
        if (!missingParams.isEmpty()) {
            log.info("检测到缺失参数 {}, 尝试从用户输入中提取...", missingParams);
            collectMissingParams(task, agent, missingParams, userInput);
        }

        // 3. 再次验证
        List<String> stillMissing = paramValidator.validateAndCollect(task, agent);
        if (!stillMissing.isEmpty()) {
            log.warn("参数收集后仍缺失必填参数: {}", stillMissing);
            task.putContext("missingParams", stillMissing);
            task.putContext("paramCollectionFailed", true);
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
     * 收集缺失的参数
     */
    private void collectMissingParams(AgentTask task, Agent agent,
                                     List<String> missingParams, String userInput) {
        try {
            // 获取参数描述
            Map<String, String> paramDescriptions = agent.paramDescriptions();

            // 构建详细的参数说明
            StringBuilder paramDetails = new StringBuilder();
            for (String param : missingParams) {
                String description = paramDescriptions.getOrDefault(param, "");
                paramDetails.append(String.format("- %s: %s\n", param,
                    description.isEmpty() ? "需要提供此参数" : description));
            }

            // 构建参数提取 Prompt
            String systemPrompt = String.format("""
                你是一个智能参数提取助手。请从用户输入中提取以下参数:

                需要提取的参数:
                %s

                请仔细分析用户输入，提取上述参数。

                注意事项:
                1. 只提取明确提到的参数，不要猜测
                2. 时间相关参数请转换为标准格式 (yyyy-MM-dd HH:mm:ss)
                   - "昨天" → 计算具体日期
                   - "最近7天" → 计算开始和结束时间
                   - "下午3点到5点" → 补充日期部分
                3. 如果无法从用户输入中提取某个参数，请省略该字段
                4. 返回标准 JSON 格式

                示例输入输出:
                用户输入: "分析昨天下午3点到5点的直播数据"
                输出:
                {
                  "timeRange": {
                    "startTime": "2025-11-20 15:00:00",
                    "endTime": "2025-11-20 17:00:00"
                  }
                }

                用户输入: "查看直播间12345的数据"
                输出:
                {
                  "filters": {
                    "liveRoomId": "12345"
                  }
                }

                现在请从以下用户输入中提取参数:
                """,
                paramDetails.toString()
            );

            log.debug("参数提取 Prompt: {}", systemPrompt);

            String llmResponse = llmClient.chat(systemPrompt, userInput);
            log.debug("LLM 参数提取结果: {}", llmResponse);

            // 解析 LLM 响应
            Map<String, Object> extractedParams = parseJsonResponse(llmResponse);

            // 将提取的参数添加到 task
            if (extractedParams != null && !extractedParams.isEmpty()) {
                extractedParams.forEach(task::putPayload);
                log.info("成功提取参数: {}", extractedParams.keySet());
            } else {
                log.warn("LLM 未能从用户输入中提取任何参数");
            }

        } catch (Exception e) {
            log.error("参数收集失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 生成友好的参数补充提示
     *
     * @param agent Agent 实例
     * @param missingParams 缺失的参数列表
     * @return 用户友好的提示信息
     */
    public String generateParamPrompt(Agent agent, List<String> missingParams) {
        if (missingParams == null || missingParams.isEmpty()) {
            return "";
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("为了更好地帮助您，我还需要以下信息：\n\n");

        Map<String, String> descriptions = agent.paramDescriptions();

        for (int i = 0; i < missingParams.size(); i++) {
            String param = missingParams.get(i);
            String desc = descriptions.get(param);

            prompt.append(String.format("%d. ", i + 1));

            if (desc != null && !desc.isEmpty()) {
                // 使用描述生成更友好的提示
                prompt.append(formatParamPrompt(param, desc));
            } else {
                // 没有描述，使用默认格式
                prompt.append(String.format("请提供 %s", param));
            }

            prompt.append("\n");
        }

        prompt.append("\n请补充上述信息，我将继续为您服务。");

        return prompt.toString();
    }

    /**
     * 格式化参数提示
     */
    private String formatParamPrompt(String paramName, String description) {
        // 针对常见参数类型生成友好提示
        if (paramName.contains("time") || paramName.contains("Time")) {
            return String.format("**时间范围**: %s\n   例如: \"昨天\"、\"最近7天\"、\"2025-11-20 到 2025-11-21\"",
                               description);
        } else if (paramName.contains("filter") || paramName.contains("Filter")) {
            return String.format("**筛选条件**: %s\n   例如: \"直播间ID: 12345\" 或 \"主播: 张三\"",
                               description);
        } else if (paramName.equals("text")) {
            return String.format("**内容**: %s", description);
        } else {
            return String.format("**%s**: %s", paramName, description);
        }
    }

    /**
     * 解析 JSON 响应
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String response) {
        try {
            String jsonContent = extractJsonFromResponse(response);
            return objectMapper.readValue(jsonContent, Map.class);
        } catch (Exception e) {
            log.error("解析 JSON 失败: {}", e.getMessage());
            return Map.of();
        }
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
                    info.setTaskType(agent.taskType());
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
            prompt.append(String.format("   - 任务类型: %s\n", agent.getTaskType()));
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
        private String taskType;
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
