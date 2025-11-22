package com.shengong.agentruntime.core.tool.impl;

import com.shengong.agentruntime.core.tool.AbstractTool;
import com.shengong.agentruntime.core.tool.annotation.ToolDefinition;
import com.shengong.agentruntime.model.ToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;
import java.util.Map;

/**
 * MCP 代理 Tool
 * 通过 HTTP 代理调用 MCP 服务
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@ConditionalOnProperty(prefix = "agent-runtime.mcp", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@ToolDefinition(
        name = "mcp_proxy_tool",
        description = "Proxy tool for Model Context Protocol (MCP) services",
        category = "mcp"
)
public class McpProxyTool extends AbstractTool {

    @Value("${agent-runtime.mcp.proxy.url}")
    private String mcpProxyUrl;

    @Value("${agent-runtime.mcp.proxy.timeout:30000}")
    private int timeout;

    private final WebClient.Builder webClientBuilder;

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult invoke(Map<String, Object> arguments) {
        try {
            String server = (String) arguments.get("server");
            String tool = (String) arguments.get("tool");
            Map<String, Object> toolArguments = (Map<String, Object>) arguments.getOrDefault("arguments", Map.of());

            log.info("Invoking MCP tool: server={}, tool={}", server, tool);

            WebClient webClient = webClientBuilder.baseUrl(mcpProxyUrl).build();

            Map<String, Object> requestBody = Map.of(
                    "server", server,
                    "tool", tool,
                    "arguments", toolArguments
            );

            Map<String, Object> response = webClient.post()
                    .uri("/mcp/invoke")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            if (response == null) {
                return ToolResult.failure("MCP proxy returned null response");
            }

            Boolean success = (Boolean) response.getOrDefault("success", false);
            if (success) {
                return ToolResult.success((Map<String, Object>) response.get("result"));
            } else {
                String error = (String) response.getOrDefault("error", "Unknown error");
                return ToolResult.failure("MCP tool failed: " + error);
            }

        } catch (Exception e) {
            log.error("MCP proxy call failed: {}", e.getMessage(), e);
            return ToolResult.failure("MCP proxy call failed: " + e.getMessage());
        }
    }
}
