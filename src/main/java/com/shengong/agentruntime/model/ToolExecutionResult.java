package com.shengong.agentruntime.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tool execution loop result.
 */
@Data
public class ToolExecutionResult {

    private ToolResult lastResult;
    private List<Map<String, Object>> history = new ArrayList<>();
}
