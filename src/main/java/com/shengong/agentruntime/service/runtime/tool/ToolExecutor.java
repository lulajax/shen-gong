package com.shengong.agentruntime.service.runtime.tool;

import com.shengong.agentruntime.model.ToolResult;
import com.shengong.agentruntime.model.spec.ToolSpec;
import com.shengong.agentruntime.service.runtime.registry.DynamicSpecRegistry;
import com.shengong.agentruntime.service.runtime.tool.adapter.ToolAdapter;
import com.shengong.agentruntime.service.runtime.validation.JsonSchemaValidationService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具执行器，负责根据工具规范执行相应的工具调用。
 * 支持参数合并、Schema 校验和适配器路由。
 */
@Service
public class ToolExecutor {

    private final DynamicSpecRegistry specRegistry;
    private final JsonSchemaValidationService schemaValidationService;
    private final Map<String, ToolAdapter> adapters;

    public ToolExecutor(DynamicSpecRegistry specRegistry,
                        JsonSchemaValidationService schemaValidationService,
                        List<ToolAdapter> adapters) {
        this.specRegistry = specRegistry;
        this.schemaValidationService = schemaValidationService;

        Map<String, ToolAdapter> index = new HashMap<>();
        for (ToolAdapter adapter : adapters) {
            index.put(adapter.type(), adapter);
        }
        this.adapters = index;
    }

    public ToolResult execute(String toolKey, Map<String, Object> args, Map<String, Object> context) {
        ToolSpec spec = specRegistry.findToolSpec(toolKey).orElse(null);
        if (spec == null) {
            return ToolResult.failure("TOOL_NOT_FOUND: " + toolKey);
        }

        Map<String, Object> mergedArgs = merge(spec.defaults(), args);
        List<String> validationErrors = schemaValidationService.validate(spec.inputSchema(), mergedArgs);
        if (!validationErrors.isEmpty()) {
            return ToolResult.failure("SCHEMA_VALIDATION_FAILED: " + String.join("; ", validationErrors));
        }

        ToolAdapter adapter = adapters.get(spec.adapterType());
        if (adapter == null) {
            return ToolResult.failure("TOOL_EXECUTION_FAILED: unknown adapter type: " + spec.adapterType());
        }

        ToolResult result = adapter.execute(spec, mergedArgs,
                context != null ? context : Collections.emptyMap());
        result.setToolName(toolKey);
        return result;
    }

    private Map<String, Object> merge(Map<String, Object> defaults, Map<String, Object> args) {
        Map<String, Object> merged = new HashMap<>();
        if (defaults != null) {
            merged.putAll(defaults);
        }
        if (args != null) {
            merged.putAll(args);
        }
        return merged;
    }
}
