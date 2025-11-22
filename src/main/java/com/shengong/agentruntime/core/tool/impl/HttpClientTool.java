package com.shengong.agentruntime.core.tool.impl;

import com.shengong.agentruntime.core.tool.AbstractTool;
import com.shengong.agentruntime.core.tool.annotation.ToolDefinition;
import com.shengong.agentruntime.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP 客户端 Tool
 * 通用 HTTP 请求工具
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@ToolDefinition(
        name = "http_client_tool",
        description = "Make HTTP requests to external services",
        category = "http"
)
public class HttpClientTool extends AbstractTool {

    private final WebClient webClient;
    private final int timeout;

    public HttpClientTool(@Value("${agent-runtime.tool.http.timeout:30000}") int timeout) {
        this.timeout = timeout;
        this.webClient = WebClient.builder().build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult invoke(Map<String, Object> arguments) {
        try {
            String url = (String) arguments.get("url");
            String method = (String) arguments.getOrDefault("method", "GET");
            Map<String, Object> headers = (Map<String, Object>) arguments.getOrDefault("headers", Map.of());
            Object body = arguments.get("body");

            log.info("Making HTTP request: method={}, url={}", method, url);

            Map<String, Object> response = switch (method.toUpperCase()) {
                case "GET" -> makeGetRequest(url, headers);
                case "POST" -> makePostRequest(url, headers, body);
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            };

            return ToolResult.success(response);

        } catch (Exception e) {
            log.error("HTTP request failed: {}", e.getMessage(), e);
            return ToolResult.failure("HTTP request failed: " + e.getMessage());
        }
    }

    private Map<String, Object> makeGetRequest(String url, Map<String, Object> headers) {
        try {
            String response = webClient.get()
                    .uri(url)
                    .headers(h -> headers.forEach((k, v) -> h.add(k, v.toString())))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            return Map.of("statusCode", 200, "body", response);
        } catch (Exception e) {
            throw new RuntimeException("GET request failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> makePostRequest(String url, Map<String, Object> headers, Object body) {
        try {
            String response = webClient.post()
                    .uri(url)
                    .headers(h -> headers.forEach((k, v) -> h.add(k, v.toString())))
                    .bodyValue(body != null ? body : "")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            return Map.of("statusCode", 200, "body", response);
        } catch (Exception e) {
            throw new RuntimeException("POST request failed: " + e.getMessage(), e);
        }
    }
}
