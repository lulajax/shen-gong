package com.shengong.agentruntime.controller;

import com.shengong.agentruntime.core.role.RoleAgent;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.runtime.RoleRequest;
import com.shengong.agentruntime.service.runtime.registry.RoleAgentRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * v2 role-driven orchestration endpoint.
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/role-agent")
@RequiredArgsConstructor
@Tag(name = "Role Agent API", description = "角色门面 + 能力编排入口")
public class RoleAgentController {

    private final RoleAgentRegistry roleAgentRegistry;

    @PostMapping("/send")
    @Operation(summary = "角色编排消息", description = "通过角色Agent触发能力与工具编排")
    public ResponseEntity<RoleAgentResponse> send(@RequestBody RoleAgentRequest request) {
        if (request == null) {
            RoleAgentResponse response = new RoleAgentResponse();
            response.setSuccess(false);
            response.setStatus("error");
            response.setMessage("INVALID_REQUEST");
            response.setData(Map.of("errors", java.util.List.of(Map.of("code", "INVALID_REQUEST"))));
            return ResponseEntity.badRequest().body(response);
        }

        String resolvedRole = resolveRole(request);

        RoleAgent roleAgent = roleAgentRegistry.find(resolvedRole).orElse(null);
        if (roleAgent == null) {
            RoleAgentResponse response = new RoleAgentResponse();
            response.setSuccess(false);
            response.setMessage("ROLE_NOT_FOUND: " + resolvedRole);
            response.setStatus("error");
            response.setData(Map.of("errors", java.util.List.of(Map.of("code", "ROLE_NOT_FOUND", "role", resolvedRole))));
            return ResponseEntity.ok(response);
        }

        String inputText = resolveInputText(request);
        if (inputText == null) {
            inputText = "";
        }
        Map<String, Object> context = request.getContext() != null
                ? new HashMap<>(request.getContext()) : new HashMap<>();
        context.putIfAbsent("role", resolvedRole);

        RoleRequest roleRequest = new RoleRequest(
                resolvedRole,
                request.getUserId(),
                request.getSessionId(),
                inputText,
                context,
                request.getPayload() != null ? request.getPayload() : Map.of()
        );

        log.info("Role agent request: role={}, userId={}", resolvedRole, request.getUserId());
        AgentResult result = roleAgent.handle(roleRequest);

        RoleAgentResponse response = new RoleAgentResponse();
        response.setSuccess(result.isSuccess());
        response.setStatus(result.getStatus());
        response.setMessage(result.getSummary());
        response.setData(result.getData());
        Map<String, Object> debug = new HashMap<>();
        if (result.getDebug() != null) {
            debug.putAll(result.getDebug());
        }
        debug.put("resolvedRole", resolvedRole);
        response.setDebug(debug);
        return ResponseEntity.ok(response);
    }

    private String resolveRole(RoleAgentRequest request) {
        String role = null;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            role = request.getRole();
        } else if (request.getContext() != null && request.getContext().get("role") != null) {
            role = String.valueOf(request.getContext().get("role"));
        }

        if (role == null || role.isBlank()) {
            role = "ecom_assistant";
        }
        if ("电商小助理".equals(role)) {
            return "ecom_assistant";
        }
        return role;
    }

    private String resolveInputText(RoleAgentRequest request) {
        if (request.getInputText() != null && !request.getInputText().isBlank()) {
            return request.getInputText();
        }
        if (request.getPayload() != null && request.getPayload().get("text") != null) {
            return String.valueOf(request.getPayload().get("text"));
        }
        return "";
    }

    @Data
    public static class RoleAgentRequest {
        private String role;
        private String userId;
        private String sessionId;
        private String inputText;
        private Map<String, Object> context;
        private Map<String, Object> payload;
    }

    @Data
    public static class RoleAgentResponse {
        private String status;
        private boolean success;
        private String message;
        private Map<String, Object> data;
        private Map<String, Object> debug;
    }
}
