package com.shengong.agentruntime.service;

import com.shengong.agentruntime.core.tool.Tool;
import com.shengong.agentruntime.model.AgentTask;
import com.shengong.agentruntime.model.ToolCallPlan;
import com.shengong.agentruntime.model.ToolExecutionResult;
import com.shengong.agentruntime.model.ToolResult;
import com.shengong.agentruntime.schema.Schema;
import com.shengong.agentruntime.schema.SchemaValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes LLM-planned tool calls with schema validation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolExecutionService {

    private final ToolCallingService toolCallingService;
    private final ToolRegistry toolRegistry;
    private final SchemaValidator schemaValidator;

    public ToolExecutionResult executeToolLoop(AgentTask task,
                                               String agentName,
                                               String agentDescription,
                                               Map<String, Object> inputParams,
                                               List<String> allowedTools,
                                               int maxSteps) {
        ToolExecutionResult executionResult = new ToolExecutionResult();
        List<Map<String, Object>> history = new ArrayList<>();
        ToolResult lastResult = null;

        for (int step = 0; step < maxSteps; step++) {
            ToolCallPlan plan = toolCallingService.planToolCall(
                task,
                agentName,
                agentDescription,
                inputParams != null ? inputParams : task.getPayload(),
                allowedTools,
                history
            );

            if (plan.getError() != null) {
                executionResult.setLastResult(ToolResult.failure(plan.getError()));
                executionResult.setHistory(history);
                return executionResult;
            }
            if (plan.isNone()) {
                executionResult.setLastResult(lastResult);
                executionResult.setHistory(history);
                return executionResult;
            }

            Tool tool = toolRegistry.getTool(plan.getToolName()).orElse(null);
            if (tool == null) {
                executionResult.setLastResult(ToolResult.failure("Selected tool not available: " + plan.getToolName()));
                executionResult.setHistory(history);
                return executionResult;
            }

            Map<String, Object> arguments = new HashMap<>(plan.getArguments());
            arguments = mergeMissingArguments(tool.inputSchema(), arguments, task.getPayload());

            List<String> inputErrors = schemaValidator.validate(tool.inputSchema(), arguments);
            if (!inputErrors.isEmpty()) {
                executionResult.setLastResult(ToolResult.failure(
                    "Tool arguments validation failed: " + String.join("; ", inputErrors)));
                executionResult.setHistory(history);
                return executionResult;
            }

            long startTime = System.currentTimeMillis();
            ToolResult result = tool.invoke(arguments);
            result.setToolName(tool.name());
            result.setLatencyMs(System.currentTimeMillis() - startTime);

            if (result.isSuccess()) {
                List<String> outputErrors = schemaValidator.validate(tool.outputSchema(), result.getData());
                if (!outputErrors.isEmpty()) {
                    executionResult.setLastResult(ToolResult.failure(
                        "Tool output validation failed: " + String.join("; ", outputErrors)));
                    executionResult.setHistory(history);
                    return executionResult;
                }
            }

            Map<String, Object> toolCallContext = new HashMap<>();
            toolCallContext.put("tool", tool.name());
            toolCallContext.put("arguments", arguments);
            toolCallContext.put("reason", plan.getReason());
            toolCallContext.put("success", result.isSuccess());
            toolCallContext.put("result", result.getData());
            history.add(toolCallContext);

            lastResult = result;
        }

        executionResult.setLastResult(lastResult);
        executionResult.setHistory(history);
        return executionResult;
    }

    private Map<String, Object> mergeMissingArguments(Schema schema,
                                                      Map<String, Object> arguments,
                                                      Map<String, Object> fallback) {
        if (schema == null || schema.getRequired() == null || schema.getRequired().isEmpty()) {
            return arguments;
        }
        Map<String, Object> merged = new HashMap<>(arguments);
        for (String required : schema.getRequired()) {
            if (!merged.containsKey(required) && fallback != null && fallback.containsKey(required)) {
                merged.put(required, fallback.get(required));
            }
        }
        return merged;
    }
}
