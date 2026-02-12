package com.shengong.agentruntime.model;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * Tool call plan returned by LLM selection.
 */
@Data
public class ToolCallPlan {

    private String toolName;
    private Map<String, Object> arguments = new HashMap<>();
    private String reason;
    private String error;
    private String rawResponse;

    public boolean isNone() {
        return toolName == null || toolName.trim().isEmpty() || "none".equalsIgnoreCase(toolName);
    }
}
