package com.shengong.agentruntime.service.runtime.argument;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengong.agentruntime.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 能力参数抽取器。
 * 按 payload 优先、自然语言补全的顺序生成能力调用参数。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CapabilityArgExtractor {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public Map<String, Object> extractArgs(String capabilityKey,
                                           String inputText,
                                           Map<String, Object> payload,
                                           Map<String, Object> context,
                                           Map<String, Object> inputSchema) {
        Map<String, Object> args = new LinkedHashMap<>();
        if (payload != null) {
            args.putAll(payload);
        }

        List<String> required = requiredFields(inputSchema);
        if (requiredSatisfied(args, required)) {
            return args;
        }

        if (inputText == null || inputText.isBlank()) {
            return args;
        }

        try {
            String systemPrompt = """
                    你是参数抽取器。请根据给定 capability 的 JSON Schema，从用户输入中抽取参数。
                    要求：
                    1. 只返回 JSON 对象，不要解释。
                    2. 不确定的字段不要猜。
                    3. 日期范围尽量归一化为数组 [startDate, endDate]，格式 yyyy-MM-dd。
                    4. 地区字段输出地区代码数组，如 ["GB","US"]。
                    """;

            Map<String, Object> promptObj = new LinkedHashMap<>();
            promptObj.put("capabilityKey", capabilityKey);
            promptObj.put("inputSchema", inputSchema != null ? inputSchema : Map.of());
            promptObj.put("existingArgs", args);
            promptObj.put("inputText", inputText);
            if (context != null && context.get("messages") != null) {
                promptObj.put("messages", context.get("messages"));
            }

            String raw = llmClient.chat(systemPrompt, objectMapper.writeValueAsString(promptObj));
            String json = extractJson(raw);
            Map<String, Object> extracted = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            if (extracted != null) {
                args.putAll(extracted);
            }
        } catch (Exception e) {
            log.warn("extract args failed for capability {}: {}", capabilityKey, e.getMessage());
        }

        return args;
    }

    @SuppressWarnings("unchecked")
    private List<String> requiredFields(Map<String, Object> inputSchema) {
        if (inputSchema == null) {
            return List.of();
        }
        Object required = inputSchema.get("required");
        if (!(required instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    private boolean requiredSatisfied(Map<String, Object> args, List<String> required) {
        if (required == null || required.isEmpty()) {
            return true;
        }
        for (String key : required) {
            if (!args.containsKey(key) || args.get(key) == null) {
                return false;
            }
        }
        return true;
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
