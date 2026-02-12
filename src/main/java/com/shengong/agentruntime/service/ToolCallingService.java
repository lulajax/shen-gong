package com.shengong.agentruntime.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengong.agentruntime.core.tool.Tool;
import com.shengong.agentruntime.llm.LlmClient;
import com.shengong.agentruntime.model.AgentTask;
import com.shengong.agentruntime.model.ToolCallPlan;
import com.shengong.agentruntime.schema.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM-based tool selection service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolCallingService {

    private final ToolRegistry toolRegistry;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToolCallPlan planToolCall(AgentTask task,
                                     String agentName,
                                     String agentDescription,
                                     Map<String, Object> inputParams,
                                     List<String> allowedTools,
                                     List<Map<String, Object>> toolHistory) {
        ToolCallPlan plan = new ToolCallPlan();
        try {
            Collection<Tool> tools = toolRegistry.getAllTools();
            if (allowedTools != null && !allowedTools.isEmpty()) {
                tools = tools.stream()
                    .filter(tool -> allowedTools.contains(tool.name()))
                    .toList();
            }

            String toolListJson = buildToolListJson(tools);
            String systemPrompt = """
                You are a tool selection planner.
                Decide whether to call one tool for the agent, and return ONLY JSON.
                You can use the tool call history to decide the next step.
                If no tool is needed, set tool to "none" and arguments to {}.

                Output JSON schema:
                {
                  "tool": "tool_name_or_none",
                  "arguments": { ... },
                  "reason": "short reason"
                }
                """;

            Map<String, Object> context = new HashMap<>();
            context.put("agentName", agentName);
            context.put("agentDescription", agentDescription);
            context.put("taskType", task.getTaskType());
            context.put("domain", task.getDomain());
            context.put("payload", task.getPayload());
            context.put("inputParams", inputParams);
            context.put("toolHistory", toolHistory != null ? toolHistory : List.of());
            context.put("availableTools", objectMapper.readValue(toolListJson, Object.class));

            String userPrompt = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
            String response = llmClient.chat(systemPrompt, userPrompt);

            plan.setRawResponse(response);
            Map<String, Object> responseMap = parseJsonResponse(response);
            Object toolObj = responseMap.get("tool");
            plan.setToolName(toolObj != null ? toolObj.toString() : null);
            Object reason = responseMap.get("reason");
            if (reason != null) {
                plan.setReason(reason.toString());
            }

            Object arguments = responseMap.get("arguments");
            if (arguments instanceof Map) {
                plan.setArguments((Map<String, Object>) arguments);
            }
        } catch (Exception e) {
            log.error("Tool selection failed: {}", e.getMessage(), e);
            plan.setError("Tool selection failed: " + e.getMessage());
        }
        return plan;
    }

    private String buildToolListJson(Collection<Tool> tools) throws Exception {
        List<Map<String, Object>> toolList = tools.stream()
            .map(tool -> {
                Map<String, Object> item = new HashMap<>();
                item.put("name", tool.name());
                item.put("description", tool.description());
                item.put("category", tool.category());
                Schema schema = tool.inputSchema();
                item.put("inputSchema", schema != null ? schema : Schema.empty(tool.name() + "_input"));
                return item;
            })
            .toList();

        return objectMapper.writeValueAsString(toolList);
    }

    private Map<String, Object> parseJsonResponse(String response) throws Exception {
        String jsonContent = extractJsonFromResponse(response);
        return objectMapper.readValue(jsonContent, Map.class);
    }

    private String extractJsonFromResponse(String response) {
        if (response == null) {
            return "{}";
        }
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
}
