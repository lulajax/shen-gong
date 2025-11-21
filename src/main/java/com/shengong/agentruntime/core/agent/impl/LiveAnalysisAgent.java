package com.shengong.agentruntime.core.agent.impl;

import com.shengong.agentruntime.core.agent.Agent;
import com.shengong.agentruntime.core.agent.AgentType;
import com.shengong.agentruntime.llm.LlmClient;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/**
 * 直播数据分析 Agent
 * 使用 LLM 分析直播数据,找出问题和机会
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveAnalysisAgent implements Agent {

    private static final AgentType AGENT_TYPE = AgentType.LIVE_ANALYSIS;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        return List.of("metrics");
    }

    @Override
    public Map<String, String> paramDescriptions() {
        return Map.of(
            "metrics", "直播指标数据，通常来自 LiveDataPrepAgent 的输出"
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public AgentResult handle(AgentTask task) {
        log.info("LiveAnalysisAgent handling task: {}", task.getTaskId());

        try {
            // 统一参数验证
            AgentResult validationError = validateParams(task);
            if (validationError != null) {
                return validationError;
            }

            // 获取指标数据
            Map<String, Object> metrics = task.getParam("metrics");

            // 构建分析 Prompt
            String systemPrompt = """
                你是一个直播数据分析专家,擅长从指标变化中找出问题与机会。
                请分析以下直播数据,按照以下结构输出:

                1. 核心发现 (findings): 列出 2-3 个关键现象
                2. 可能原因 (rootCauses): 分析 2-3 个可能的根本原因
                3. 优化建议 (suggestions): 提供 3-5 个可执行的优化建议

                请以 JSON 格式输出,包含 findings、rootCauses、suggestions 三个数组字段。
                """;

            String metricsJson = objectMapper.writeValueAsString(metrics);
            String userPrompt = "直播数据指标如下:\n" + metricsJson + "\n\n请进行分析。";

            log.debug("Calling LLM for analysis...");
            String llmResponse = llmClient.chat(systemPrompt, userPrompt);

            // 尝试解析 LLM 响应为 JSON
            Map<String, Object> analysis;
            try {
                analysis = parseAnalysisResponse(llmResponse);
            } catch (Exception e) {
                log.warn("Failed to parse LLM response as JSON, using raw text");
                analysis = Map.of(
                        "summary", llmResponse,
                        "findings", List.of(),
                        "rootCauses", List.of(),
                        "suggestions", List.of()
                );
            }

            return AgentResult.ok("Analysis completed", Map.of(
                    "analysis", analysis,
                    "metrics", metrics
            ));

        } catch (Exception e) {
            log.error("LiveAnalysisAgent failed: {}", e.getMessage(), e);
            return AgentResult.error("Analysis failed: " + e.getMessage());
        }
    }

    /**
     * 解析 LLM 分析响应
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAnalysisResponse(String response) throws Exception {
        // 尝试从响应中提取 JSON
        String jsonContent = response;

        // 如果响应包含 markdown 代码块,提取 JSON 部分
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

        return objectMapper.readValue(jsonContent, Map.class);
    }

    @Override
    public String description() {
        return AGENT_TYPE.getDescription();
    }
}
