package com.shengong.agentruntime.service.runtime.capability;

import com.shengong.agentruntime.core.capability.CapabilityRuntime;
import com.shengong.agentruntime.model.runtime.CapabilityResult;
import com.shengong.agentruntime.model.spec.CapabilitySpec;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A2A 能力执行器，通过 LangChain4j A2A 协议调用远程能力。
 * 支持配置 serverUrl、inputKeys、outputKey 等参数。
 */
@Slf4j
@Service
@Order(10)
public class A2aCapabilityExecutor implements CapabilityExecutor {

    private final Map<String, UntypedAgent> clientCache = new ConcurrentHashMap<>();

    @Override
    public boolean supports(CapabilitySpec spec) {
        if (spec == null) {
            return false;
        }
        if ("a2a".equalsIgnoreCase(spec.executionType())) {
            return true;
        }
        Map<String, Object> config = spec.config();
        if (config == null) {
            return false;
        }
        Object mode = config.get("mode");
        if (mode != null && "a2a".equalsIgnoreCase(String.valueOf(mode))) {
            return true;
        }
        return config.get("serverUrl") != null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CapabilityResult execute(CapabilitySpec spec,
                                    Map<String, Object> args,
                                    Map<String, Object> context,
                                    int depth,
                                    CapabilityRuntime runtime) {
        try {
            Map<String, Object> config = spec.config() != null ? spec.config() : Map.of();
            String serverUrl = valueAsString(config.get("serverUrl"));
            if (serverUrl == null || serverUrl.isBlank()) {
                return CapabilityResult.failure("A2A_CONFIG_INVALID",
                        "Missing serverUrl for capability: " + spec.capabilityKey());
            }

            UntypedAgent agent = clientCache.computeIfAbsent(spec.capabilityKey(), ignored ->
                    buildAgent(spec, serverUrl, config));

            Object raw = agent.invoke(args != null ? args : Map.of());
            String outputKey = valueAsString(config.getOrDefault("outputKey", "result"));

            Map<String, Object> data = new LinkedHashMap<>();
            if (raw instanceof Map<?, ?> mapResult) {
                for (Map.Entry<?, ?> entry : mapResult.entrySet()) {
                    data.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            } else {
                data.put(outputKey, raw);
            }
            data.put("_execution", "a2a");
            data.put("_serverUrl", serverUrl);

            return CapabilityResult.success("A2A capability executed", data);
        } catch (Exception e) {
            log.error("A2A capability execution failed: capability={}", spec.capabilityKey(), e);
            return CapabilityResult.failure("A2A_EXECUTION_FAILED", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private UntypedAgent buildAgent(CapabilitySpec spec, String serverUrl, Map<String, Object> config) {
        var builder = AgenticServices.a2aBuilder(serverUrl);

        List<String> inputKeys = new ArrayList<>();
        Object configuredInputKeys = config.get("inputKeys");
        if (configuredInputKeys instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    inputKeys.add(String.valueOf(item));
                }
            }
        }

        if (inputKeys.isEmpty() && spec.inputSchema() != null) {
            Object required = spec.inputSchema().get("required");
            if (required instanceof List<?> list) {
                for (Object item : list) {
                    if (item != null) {
                        inputKeys.add(String.valueOf(item));
                    }
                }
            }
        }

        if (!inputKeys.isEmpty()) {
            builder.inputKeys(inputKeys.toArray(new String[0]));
        }

        String outputKey = valueAsString(config.get("outputKey"));
        if (outputKey == null || outputKey.isBlank()) {
            outputKey = firstOutputKey(spec.outputSchema()).orElse("result");
        }
        builder.outputKey(outputKey);

        boolean async = valueAsBoolean(config.get("async"), false);
        builder.async(async);

        log.info("Build A2A client: capability={}, serverUrl={}, inputKeys={}, outputKey={}, async={}",
                spec.capabilityKey(), serverUrl, inputKeys, outputKey, async);
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private java.util.Optional<String> firstOutputKey(Map<String, Object> outputSchema) {
        if (outputSchema == null) {
            return java.util.Optional.empty();
        }
        Object properties = outputSchema.get("properties");
        if (properties instanceof Map<?, ?> props && !props.isEmpty()) {
            Object key = props.keySet().iterator().next();
            return key != null ? java.util.Optional.of(String.valueOf(key)) : java.util.Optional.empty();
        }
        Object required = outputSchema.getOrDefault("required", Collections.emptyList());
        if (required instanceof List<?> list && !list.isEmpty() && list.get(0) != null) {
            return java.util.Optional.of(String.valueOf(list.get(0)));
        }
        return java.util.Optional.empty();
    }

    private String valueAsString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private boolean valueAsBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
