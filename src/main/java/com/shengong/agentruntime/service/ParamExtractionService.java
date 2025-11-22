package com.shengong.agentruntime.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengong.agentruntime.core.agent.Agent;
import com.shengong.agentruntime.core.param.ParamBinder;
import com.shengong.agentruntime.core.param.ParamValidator;
import com.shengong.agentruntime.llm.LlmClient;
import com.shengong.agentruntime.model.AgentTask;
import com.shengong.agentruntime.model.ParamValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 参数提取服务
 * 负责分析缺失参数、调用 LLM 提取参数以及校验参数
 *
 * @author 神工团队
 * @since 1.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParamExtractionService {

    private final LlmClient llmClient;
    private final ParamBinder paramBinder;
    private final ParamValidator paramValidator;
    private final ObjectMapper objectMapper;

    /**
     * 确保 Agent 所需参数已就绪
     * 如果缺失，尝试从用户输入提取
     *
     * @param agent     目标 Agent
     * @param task      当前任务
     * @param userInput 用户输入
     * @return 验证结果
     */
    public ParamValidationResult ensureParams(Agent agent, AgentTask task, String userInput) {
        // 1. 初步验证，获取缺失参数
        List<String> missingParams = paramValidator.validateAndCollect(task, agent);

        if (missingParams.isEmpty()) {
            return ParamValidationResult.success();
        }

        // 2. 尝试利用 LLM 从用户输入中提取参数
        log.info("Agent {} 缺失参数: {}, 尝试从用户输入中提取...", agent.name(), missingParams);
        extractParamsWithLlm(agent, task, missingParams, userInput);

        // 3. 再次验证
        List<String> stillMissing = paramValidator.validateAndCollect(task, agent);
        if (stillMissing.isEmpty()) {
            return ParamValidationResult.success();
        }

        // 4. 仍有缺失，生成提示语
        String missingPrompt = generateMissingParamsPrompt(agent, stillMissing);
        return ParamValidationResult.fail(stillMissing, missingPrompt);
    }

    private void extractParamsWithLlm(Agent agent, AgentTask task, List<String> missingParams, String userInput) {
        try {
            // 尝试获取参数类的 Prompt
            String paramDesc = "";
            Class<?> paramType = agent.getParamType();

            if (paramType != null) {
                // 使用 ParamBinder 生成详细的参数提取 Prompt
                paramDesc = paramBinder.generateExtractionPrompt(paramType);
            } else {
                // Fallback: 仅列出缺失参数及其描述
                StringBuilder sb = new StringBuilder();
                Map<String, String> descs = agent.paramDescriptions();
                for (String param : missingParams) {
                    String desc = descs.getOrDefault(param, "");
                    if (desc != null && !desc.isEmpty()) {
                        sb.append(String.format("- %s: %s\n", param, desc));
                    } else {
                        sb.append(String.format("- %s\n", param));
                    }
                }
                paramDesc = sb.toString();
            }

            // 获取上下文中的历史记录
            StringBuilder historyInput = new StringBuilder();
            Object historyObj = task.getContextValue("conversationHistory");
            if (historyObj instanceof List) {
                List<?> history = (List<?>) historyObj;
                if (!history.isEmpty()) {
                    historyInput.append("对话历史上下文:\n");
                    // 只取最近的 10 条记录用于参数提取
                    int maxHistory = 10;
                    int start = Math.max(0, history.size() - maxHistory);

                    // 遍历历史记录 (排除最后一条，因为它是当前的 userInput)
                    for (int i = start; i < history.size() - 1; i++) {
                        Object item = history.get(i);
                        // 这里我们需要简单的反射或者假设是 IntelligentAgentRouter.ConversationMessage
                        // 简单处理：toString 或者转 json
                        try {
                             // 尝试获取 getTextContent 方法
                             java.lang.reflect.Method method = item.getClass().getMethod("getTextContent");
                             Object roleObj = item.getClass().getMethod("getRole").invoke(item);
                             Object contentObj = method.invoke(item);
                             historyInput.append(String.format("%s: %s\n", roleObj, contentObj));
                        } catch (Exception ignore) {
                             // 如果无法获取，忽略
                        }
                    }
                    historyInput.append("\n当前用户输入: ");
                }
            }
            historyInput.append(userInput);


            // 构建 Prompt
            String systemPrompt = String.format("""
                你是一个智能参数提取助手。请从用户输入和对话历史中提取以下参数:

                需要提取的参数:
                %s

                请仔细分析用户输入，提取上述参数。

                注意事项:
                1. 只提取明确提到的参数，不要猜测
                2. 时间相关参数请转换为标准格式 (yyyy-MM-dd HH:mm:ss)
                   - "昨天" → 计算具体日期
                   - "最近7天" → 计算开始和结束时间
                3. 返回标准 JSON 格式

                用户输入(含上下文): "%s"
                """, paramDesc, historyInput.toString());

            log.debug("参数提取 Prompt: {}", systemPrompt);
            String response = llmClient.chat(systemPrompt, userInput);
            log.debug("LLM 参数提取结果: {}", response);

            Map<String, Object> extracted = parseJsonResponse(response);
            if (extracted != null && !extracted.isEmpty()) {
                extracted.forEach(task::putPayload);
                log.info("成功提取参数: {}", extracted.keySet());
            }

        } catch (Exception e) {
            log.error("LLM 参数提取失败", e);
        }
    }

    /**
     * 生成缺失参数的提示语
     */
    private String generateMissingParamsPrompt(Agent agent, List<String> missingParams) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("为了更好地帮助您，我还需要以下信息：\n\n");

        Map<String, String> descriptions = agent.paramDescriptions();

        for (int i = 0; i < missingParams.size(); i++) {
            String param = missingParams.get(i);
            String desc = descriptions.get(param);

            prompt.append(String.format("%d. ", i + 1));

            if (desc != null && !desc.isEmpty()) {
                prompt.append(formatParamPrompt(param, desc));
            } else {
                prompt.append(String.format("请提供 %s", param));
            }

            prompt.append("\n");
        }
        
        prompt.append("\n请补充上述信息。");
        return prompt.toString();
    }

    private String formatParamPrompt(String paramName, String description) {
        if (paramName.toLowerCase().contains("time")) {
            return String.format("**时间范围**: %s\n   例如: \"昨天\"、\"2025-01-01\"", description);
        } else if (paramName.toLowerCase().contains("filter")) {
            return String.format("**筛选条件**: %s", description);
        } else {
            return String.format("**%s**: %s", paramName, description);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String response) {
        try {
            String jsonContent = response;
            if (response.contains("```json")) {
                int start = response.indexOf("```json") + 7;
                int end = response.indexOf("```", start);
                if (end > start) {
                    jsonContent = response.substring(start, end).trim();
                }
            } else if (response.contains("```")) {
                int start = response.indexOf("```") + 3;
                int end = response.indexOf("```", start);
                if (end > start) {
                    jsonContent = response.substring(start, end).trim();
                }
            }
            
            // 去掉可能的前缀后缀
            if (jsonContent.indexOf('{') >= 0) {
                jsonContent = jsonContent.substring(jsonContent.indexOf('{'));
            }
            if (jsonContent.lastIndexOf('}') >= 0) {
                jsonContent = jsonContent.substring(0, jsonContent.lastIndexOf('}') + 1);
            }

            return objectMapper.readValue(jsonContent, Map.class);
        } catch (Exception e) {
            log.error("JSON 解析失败: {}", e.getMessage());
            return Map.of();
        }
    }
}

