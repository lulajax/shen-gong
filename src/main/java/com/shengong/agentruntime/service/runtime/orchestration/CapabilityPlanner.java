package com.shengong.agentruntime.service.runtime.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengong.agentruntime.llm.LlmClient;
import com.shengong.agentruntime.model.runtime.PlannerAction;
import com.shengong.agentruntime.model.spec.CapabilitySpec;
import com.shengong.agentruntime.service.runtime.registry.DynamicSpecRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 能力规划器，基于 LLM 决定下一步编排动作。
 * 根据角色、可用能力、历史观察记录生成执行计划。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CapabilityPlanner {

    private final LlmClient llmClient;
    private final DynamicSpecRegistry specRegistry;
    private final ObjectMapper objectMapper;

    public PlannerAction planNextAction(String role,
                                        String inputText,
                                        Map<String, Object> context,
                                        List<String> allowedCapabilities,
                                        List<Map<String, Object>> observations,
                                        int step,
                                        int maxSteps) {
        try {
            String systemPrompt = buildSystemPrompt(role, allowedCapabilities, maxSteps);
            String userPrompt = buildUserPrompt(inputText, context, observations, step);
            String raw = llmClient.chat(systemPrompt, userPrompt);
            return parse(raw);
        } catch (Exception e) {
            log.warn("Planner failed, fallback to FINAL_ANSWER: {}", e.getMessage());
            return new PlannerAction(
                    PlannerAction.FINAL_ANSWER,
                    null,
                    null,
                    Map.of(),
                    "规划失败，返回当前可用结果。",
                    e.getMessage());
        }
    }

    private String buildSystemPrompt(String role, List<String> allowedCapabilities, int maxSteps) {
        String capabilityDesc = allowedCapabilities.stream()
                .map(key -> {
                    CapabilitySpec spec = specRegistry.findCapabilitySpec(key).orElse(null);
                    if (spec == null) {
                        return "- " + key;
                    }
                    return String.format("- %s: %s", key, spec.description());
                })
                .collect(Collectors.joining("\n"));

        return """
                你是角色能力编排器，负责决定下一步动作。
                角色: %s
                最大步骤: %d
                可用能力:
                %s

                仅返回 JSON，不要返回额外文本。
                JSON 结构:
                {
                  "action": "CALL_CAPABILITY|CALL_TOOL|FINAL_ANSWER",
                  "capability": "能力Key，可空",
                  "tool": "工具Key，可空",
                  "args": {"任意动态参数": "值"},
                  "finalAnswer": "最终回答文本，可空",
                  "reason": "动作原因"
                }

                优先使用 CALL_CAPABILITY。
                只有在你确定可直接结束时才输出 FINAL_ANSWER。
                """.formatted(role, maxSteps, capabilityDesc);
    }

    private String buildUserPrompt(String inputText,
                                   Map<String, Object> context,
                                   List<Map<String, Object>> observations,
                                   int step) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("step", step);
            payload.put("inputText", inputText);
            payload.put("context", context != null ? context : Map.of());
            payload.put("observations", observations);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{\"inputText\":\"" + inputText + "\",\"step\":" + step + "}";
        }
    }

    @SuppressWarnings("unchecked")
    private PlannerAction parse(String raw) {
        try {
            String json = extractJson(raw);
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            return new PlannerAction(
                    String.valueOf(map.getOrDefault("action", PlannerAction.FINAL_ANSWER)),
                    (String) map.get("capability"),
                    (String) map.get("tool"),
                    (Map<String, Object>) map.getOrDefault("args", Map.of()),
                    (String) map.getOrDefault("finalAnswer", ""),
                    (String) map.getOrDefault("reason", "")
            );
        } catch (Exception e) {
            log.warn("Failed to parse planner action, fallback FINAL_ANSWER: {}", e.getMessage());
            return new PlannerAction(
                    PlannerAction.FINAL_ANSWER,
                    null,
                    null,
                    Map.of(),
                    "无法解析规划动作，直接返回结果。",
                    "parse_failed"
            );
        }
    }

    private String extractJson(String response) {
        if (response == null) {
            return "{}";
        }
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        return response.trim();
    }
}
