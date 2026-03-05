package com.shengong.agentruntime.service.runtime.orchestration;

import com.shengong.agentruntime.core.capability.CapabilityRuntime;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.ToolResult;
import com.shengong.agentruntime.model.runtime.CapabilityResult;
import com.shengong.agentruntime.model.runtime.PlannerAction;
import com.shengong.agentruntime.model.runtime.RoleRequest;
import com.shengong.agentruntime.model.spec.CapabilitySpec;
import com.shengong.agentruntime.model.spec.RolePolicy;
import com.shengong.agentruntime.service.runtime.argument.CapabilityArgExtractor;
import com.shengong.agentruntime.service.runtime.capability.executor.CapabilityExecutor;
import com.shengong.agentruntime.service.runtime.registry.DynamicSpecRegistry;
import com.shengong.agentruntime.service.runtime.tool.ToolExecutor;
import com.shengong.agentruntime.service.runtime.validation.JsonSchemaValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 能力编排器，实现 Role -> Capability -> Tool 的完整执行链路。
 * 支持多步骤编排（默认最大 8 步）、能力嵌套调用（默认最大深度 2）和 A2A 远程调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CapabilityOrchestrator implements CapabilityRuntime {

    private final DynamicSpecRegistry specRegistry;
    private final CapabilityPlanner planner;
    private final ToolExecutor toolExecutor;
    private final List<CapabilityExecutor> capabilityExecutors;
    private final CapabilityArgExtractor capabilityArgExtractor;
    private final JsonSchemaValidationService schemaValidationService;

    @Value("${agent-runtime.role-orchestration.max-steps:8}")
    private int maxSteps;

    @Value("${agent-runtime.role-orchestration.max-depth:2}")
    private int maxDepth;

    public AgentResult executeRole(RoleRequest request) {
        String role = request.role();
        RolePolicy rolePolicy = specRegistry.findRolePolicy(role).orElse(null);
        if (rolePolicy == null) {
            return AgentResult.error("ROLE_NOT_FOUND: " + role)
                    .addData("errors", List.of(Map.of("code", "ROLE_NOT_FOUND", "role", role)));
        }

        List<String> allowedCapabilities = sanitizeAllowedCapabilities(rolePolicy);
        if (allowedCapabilities.isEmpty()) {
            return AgentResult.error("No allowed capabilities for role: " + role)
                    .addData("errors", List.of(Map.of("code", "ROLE_NO_CAPABILITIES", "role", role)));
        }

        Set<String> roleAllowedTools = collectAllowedTools(allowedCapabilities);
        List<Map<String, Object>> observations = new ArrayList<>();
        Map<String, Object> context = request.context() != null ? new HashMap<>(request.context()) : new HashMap<>();
        context.putIfAbsent("inputText", request.inputText());
        if (request.payload() != null && !request.payload().isEmpty()) {
            context.putIfAbsent("payload", request.payload());
        }
        boolean forcePlanner = Boolean.TRUE.equals(context.get("forcePlanner"));

        if (allowedCapabilities.size() == 1 && !forcePlanner) {
            return executeSingleCapabilityDirectly(request, context, allowedCapabilities.get(0));
        }

        for (int step = 1; step <= maxSteps; step++) {
            PlannerAction action = planner.planNextAction(role,
                    request.inputText(), context, allowedCapabilities, observations, step, maxSteps);

            if (PlannerAction.FINAL_ANSWER.equals(action.action())) {
                String summary = action.finalAnswer() != null && !action.finalAnswer().isBlank()
                        ? action.finalAnswer()
                        : fallbackSummary(observations, request.inputText());

                return AgentResult.ok(summary, Map.of(
                        "role", role,
                        "observations", observations,
                        "steps", step
                ));
            }

            if (PlannerAction.CALL_CAPABILITY.equals(action.action())) {
                String capabilityKey = action.capability();
                if (!allowedCapabilities.contains(capabilityKey)) {
                    return AgentResult.error("CAPABILITY_DENIED: " + capabilityKey)
                            .addData("errors", List.of(Map.of(
                                    "code", "CAPABILITY_DENIED",
                                    "role", role,
                                    "capability", capabilityKey
                            )));
                }

                CapabilityResult capabilityResult = callCapability(capabilityKey, action.args(), context, 1);
                observations.add(Map.of(
                        "type", "capability",
                        "key", capabilityKey,
                        "success", capabilityResult.isSuccess(),
                        "summary", capabilityResult.getSummary(),
                        "data", capabilityResult.getData(),
                        "errors", capabilityResult.getErrors()
                ));

                if (!capabilityResult.isSuccess()) {
                    return AgentResult.partial(
                            "Capability execution failed",
                            Map.of("role", role, "observations", observations),
                            capabilityResult.getErrors().stream().map(Objects::toString).toList()
                    );
                }
                continue;
            }

            if (PlannerAction.CALL_TOOL.equals(action.action())) {
                String toolKey = action.tool();
                if (!roleAllowedTools.contains(toolKey)) {
                    return AgentResult.error("TOOL_NOT_ALLOWED: " + toolKey)
                            .addData("errors", List.of(Map.of(
                                    "code", "TOOL_NOT_ALLOWED",
                                    "role", role,
                                    "tool", toolKey
                            )));
                }

                ToolResult toolResult = callTool(toolKey, action.args(), context);
                observations.add(Map.of(
                        "type", "tool",
                        "key", toolKey,
                        "success", toolResult.isSuccess(),
                        "data", toolResult.getData(),
                        "error", toolResult.getError()
                ));

                if (!toolResult.isSuccess()) {
                    return AgentResult.partial(
                            "Tool execution failed",
                            Map.of("role", role, "observations", observations),
                            List.of(toolResult.getError())
                    );
                }
                continue;
            }

            return AgentResult.error("Unsupported planner action: " + action.action());
        }

        return AgentResult.partial(
                "Orchestration reached max steps",
                Map.of("role", role, "observations", observations),
                List.of("MAX_STEPS_EXCEEDED")
        );
    }

    @Override
    public CapabilityResult callCapability(String capabilityKey, Map<String, Object> args,
                                           Map<String, Object> context, int depth) {
        if (depth > maxDepth) {
            return CapabilityResult.failure("CAPABILITY_DEPTH_EXCEEDED",
                    "Capability depth exceeded max depth: " + maxDepth);
        }

        CapabilitySpec spec = specRegistry.findCapabilitySpec(capabilityKey).orElse(null);
        if (spec == null) {
            return CapabilityResult.failure("CAPABILITY_NOT_FOUND", "Capability spec not found: " + capabilityKey);
        }

        Map<String, Object> normalizedArgs = args != null ? args : Map.of();

        List<String> inputErrors = schemaValidationService.validate(spec.inputSchema(), normalizedArgs);
        if (!inputErrors.isEmpty()) {
            return CapabilityResult.failure("SCHEMA_VALIDATION_FAILED", String.join("; ", inputErrors));
        }

        Map<String, Object> runtimeContext = context != null ? new HashMap<>(context) : new HashMap<>();
        runtimeContext.put(RuntimeContextKeys.CURRENT_CAPABILITY_KEY, capabilityKey);

        CapabilityExecutor executor = selectCapabilityExecutor(spec);
        if (executor == null) {
            return CapabilityResult.failure("CAPABILITY_EXECUTION_FAILED",
                    "No capability executor found for: " + capabilityKey);
        }

        CapabilityResult result = executor.execute(spec, normalizedArgs, runtimeContext, depth, this);

        if (result == null) {
            return CapabilityResult.failure("CAPABILITY_EXECUTION_FAILED", "Capability returned null result");
        }

        List<String> outputErrors = schemaValidationService.validate(spec.outputSchema(), result.getData());
        if (!outputErrors.isEmpty()) {
            return CapabilityResult.failure("SCHEMA_VALIDATION_FAILED", String.join("; ", outputErrors));
        }

        return result;
    }

    @Override
    public ToolResult callTool(String toolKey, Map<String, Object> args, Map<String, Object> context) {
        if (context != null && context.get(RuntimeContextKeys.CURRENT_CAPABILITY_KEY) != null) {
            String currentCapability = String.valueOf(context.get(RuntimeContextKeys.CURRENT_CAPABILITY_KEY));
            CapabilitySpec capabilitySpec = specRegistry.findCapabilitySpec(currentCapability).orElse(null);
            if (capabilitySpec == null) {
                return ToolResult.failure("CAPABILITY_NOT_FOUND: " + currentCapability);
            }
            List<String> allowedTools = capabilitySpec.allowedTools() != null
                    ? capabilitySpec.allowedTools() : List.of();
            if (!allowedTools.contains(toolKey)) {
                return ToolResult.failure("TOOL_NOT_ALLOWED: " + toolKey);
            }
        }

        return toolExecutor.execute(toolKey, args, context);
    }

    private List<String> sanitizeAllowedCapabilities(RolePolicy rolePolicy) {
        Set<String> allowed = new HashSet<>();
        if (rolePolicy.allowedCapabilities() != null) {
            allowed.addAll(rolePolicy.allowedCapabilities());
        }
        if (rolePolicy.deniedCapabilities() != null) {
            allowed.removeAll(rolePolicy.deniedCapabilities());
        }
        return new ArrayList<>(allowed);
    }

    private Set<String> collectAllowedTools(List<String> capabilities) {
        Set<String> tools = new HashSet<>();
        for (String capability : capabilities) {
            specRegistry.findCapabilitySpec(capability)
                    .map(CapabilitySpec::allowedTools)
                    .ifPresent(list -> {
                        if (list != null) {
                            tools.addAll(list);
                        }
                    });
        }
        return tools;
    }

    private String fallbackSummary(List<Map<String, Object>> observations, String inputText) {
        if (observations.isEmpty()) {
            return "未触发任何能力或工具，原始输入: " + inputText;
        }
        Map<String, Object> last = observations.get(observations.size() - 1);
        Object summary = last.get("summary");
        if (summary != null) {
            return String.valueOf(summary);
        }
        return "任务执行完成";
    }

    private AgentResult executeSingleCapabilityDirectly(RoleRequest request,
                                                        Map<String, Object> context,
                                                        String capabilityKey) {
        CapabilitySpec spec = specRegistry.findCapabilitySpec(capabilityKey).orElse(null);
        if (spec == null) {
            return AgentResult.error("CAPABILITY_NOT_FOUND: " + capabilityKey)
                    .addData("errors", List.of(Map.of(
                            "code", "CAPABILITY_NOT_FOUND",
                            "capability", capabilityKey
                    )));
        }

        Map<String, Object> args = capabilityArgExtractor.extractArgs(
                capabilityKey,
                request.inputText(),
                request.payload(),
                context,
                spec.inputSchema()
        );

        CapabilityResult capabilityResult = callCapability(capabilityKey, args, context, 1);
        if (!capabilityResult.isSuccess()) {
            Map<String, Object> data = new HashMap<>();
            data.put("role", request.role());
            data.put("capability", capabilityKey);
            data.put("args", args);
            data.put("errors", capabilityResult.getErrors());
            return AgentResult.partial(
                    "Capability execution failed",
                    data,
                    capabilityResult.getErrors().stream().map(Objects::toString).toList()
            );
        }

        Map<String, Object> data = new HashMap<>();
        data.put("role", request.role());
        data.put("capability", capabilityKey);
        data.put("args", args);
        data.putAll(capabilityResult.getData());

        AgentResult result = AgentResult.ok(capabilityResult.getSummary(), data);
        result.addDebug("mode", "single_capability_direct");
        return result;
    }

    private CapabilityExecutor selectCapabilityExecutor(CapabilitySpec spec) {
        for (CapabilityExecutor executor : capabilityExecutors) {
            if (executor.supports(spec)) {
                return executor;
            }
        }
        return null;
    }
}
