package com.shengong.agentruntime.service.runtime.tool.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengong.agentruntime.model.ToolResult;
import com.shengong.agentruntime.model.spec.ToolSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HTTP 工具适配器，用于执行外部 HTTP 接口调用。
 * 支持 GET/POST/DELETE 等方法，可配置查询参数、请求头和请求体模板。
 */
@Component
@RequiredArgsConstructor
public class HttpToolAdapter implements ToolAdapter {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${agent-runtime.tool.http.timeout:30000}")
    private int defaultTimeoutMs;

    @Override
    public String type() {
        return "http";
    }

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult execute(ToolSpec spec, Map<String, Object> args, Map<String, Object> context) {
        try {
            Map<String, Object> config = spec.config() != null ? spec.config() : Map.of();
            String url = String.valueOf(config.getOrDefault("url", "")).trim();
            if (url.isEmpty()) {
                return ToolResult.failure("TOOL_EXECUTION_FAILED: http tool url is missing");
            }

            String method = String.valueOf(config.getOrDefault("method", "POST")).toUpperCase();
            HttpMethod httpMethod;
            try {
                httpMethod = HttpMethod.valueOf(method);
            } catch (Exception ex) {
                return ToolResult.failure("TOOL_EXECUTION_FAILED: unsupported http method: " + method);
            }

            int timeoutMs = defaultTimeoutMs;
            Object timeoutObj = config.get("timeoutMs");
            if (timeoutObj instanceof Number number) {
                timeoutMs = number.intValue();
            }

            Map<String, Object> headers = config.get("headers") instanceof Map<?, ?> map
                    ? (Map<String, Object>) map
                    : Map.of();

            String finalUrl = buildUrlWithQuery(url, args, config, httpMethod);
            Object body = buildBody(args, config, httpMethod);

            WebClient.RequestBodySpec requestSpec = webClientBuilder.build()
                    .method(httpMethod)
                    .uri(finalUrl)
                    .headers(h -> headers.forEach((k, v) -> h.add(k, String.valueOf(v))));

            ResponseEntity<String> response;
            if (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE) {
                response = requestSpec
                        .retrieve()
                        .toEntity(String.class)
                        .timeout(Duration.ofMillis(timeoutMs))
                        .block();
            } else {
                response = requestSpec
                        .bodyValue(body != null ? body : Map.of())
                        .retrieve()
                        .toEntity(String.class)
                        .timeout(Duration.ofMillis(timeoutMs))
                        .block();
            }

            int statusCode = response != null ? response.getStatusCode().value() : 500;
            String responseBody = response != null && response.getBody() != null ? response.getBody() : "";

            Map<String, Object> data = Map.of(
                    "statusCode", statusCode,
                    "body", responseBody,
                    "url", finalUrl,
                    "method", method
            );
            return ToolResult.success(data);
        } catch (Exception e) {
            return ToolResult.failure("TOOL_EXECUTION_FAILED: " + e.getMessage());
        }
    }

    private String buildUrlWithQuery(String baseUrl, Map<String, Object> args,
                                     Map<String, Object> config, HttpMethod method) {
        if (method != HttpMethod.GET && method != HttpMethod.DELETE) {
            return baseUrl;
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);
        List<String> queryParamKeys = new ArrayList<>();
        Object queryKeysObj = config.get("queryParamKeys");
        if (queryKeysObj instanceof List<?> list) {
            for (Object obj : list) {
                if (obj != null) {
                    queryParamKeys.add(String.valueOf(obj));
                }
            }
        }

        if (args == null || args.isEmpty()) {
            return builder.build(true).toUriString();
        }

        if (queryParamKeys.isEmpty()) {
            args.forEach((key, value) -> appendQueryParam(builder, key, value));
            return builder.build(true).toUriString();
        }

        for (String key : queryParamKeys) {
            if (!args.containsKey(key)) {
                continue;
            }
            appendQueryParam(builder, key, args.get(key));
        }
        return builder.build(true).toUriString();
    }

    private Object buildBody(Map<String, Object> args, Map<String, Object> config, HttpMethod method) {
        if (method == HttpMethod.GET || method == HttpMethod.DELETE) {
            return null;
        }

        if (config.get("bodyField") != null && args != null) {
            String bodyField = String.valueOf(config.get("bodyField"));
            return args.get(bodyField);
        }

        if (config.get("bodyTemplate") instanceof Map<?, ?> templateMap) {
            return objectMapper.convertValue(templateMap, new TypeReference<Map<String, Object>>() {});
        }

        return args;
    }

    @SuppressWarnings("unchecked")
    private void appendQueryParam(UriComponentsBuilder builder, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item != null) {
                    builder.queryParam(key, item);
                }
            }
            return;
        }
        if (value.getClass().isArray()) {
            Object[] arr = (Object[]) value;
            for (Object item : arr) {
                if (item != null) {
                    builder.queryParam(key, item);
                }
            }
            return;
        }
        builder.queryParam(key, value);
    }
}
