package com.shengong.agentruntime.service.runtime.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengong.agentruntime.model.spec.CapabilitySpec;
import com.shengong.agentruntime.model.spec.CapabilitySpecsConfig;
import com.shengong.agentruntime.model.spec.RolePoliciesConfig;
import com.shengong.agentruntime.model.spec.RolePolicy;
import com.shengong.agentruntime.model.spec.ToolSpec;
import com.shengong.agentruntime.model.spec.ToolSpecsConfig;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 动态规格注册表，从类路径 YAML 文件加载角色、能力、工具规格。
 * 加载路径：agent-runtime/roles.yml、capabilities.yml、tools.yml
 */
@Slf4j
@Component
public class DynamicSpecRegistry {

    private static final String ROLE_SPEC_PATH = "agent-runtime/roles.yml";
    private static final String CAPABILITY_SPEC_PATH = "agent-runtime/capabilities.yml";
    private static final String TOOL_SPEC_PATH = "agent-runtime/tools.yml";

    private final ObjectMapper objectMapper;

    @Getter
    private Map<String, RolePolicy> rolePolicies = Collections.emptyMap();

    @Getter
    private Map<String, CapabilitySpec> capabilitySpecs = Collections.emptyMap();

    @Getter
    private Map<String, ToolSpec> toolSpecs = Collections.emptyMap();

    public DynamicSpecRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        this.rolePolicies = indexRoles(loadYaml(ROLE_SPEC_PATH, RolePoliciesConfig.class).roles());
        this.capabilitySpecs = indexCapabilities(loadYaml(CAPABILITY_SPEC_PATH, CapabilitySpecsConfig.class).capabilities());
        this.toolSpecs = indexTools(loadYaml(TOOL_SPEC_PATH, ToolSpecsConfig.class).tools());

        log.info("Dynamic specs loaded: roles={}, capabilities={}, tools={}",
                rolePolicies.size(), capabilitySpecs.size(), toolSpecs.size());
    }

    public Optional<RolePolicy> findRolePolicy(String roleKey) {
        return Optional.ofNullable(rolePolicies.get(roleKey));
    }

    public Optional<CapabilitySpec> findCapabilitySpec(String capabilityKey) {
        return Optional.ofNullable(capabilitySpecs.get(capabilityKey));
    }

    public Optional<ToolSpec> findToolSpec(String toolKey) {
        return Optional.ofNullable(toolSpecs.get(toolKey));
    }

    private <T> T loadYaml(String classpath, Class<T> type) {
        try (InputStream inputStream = new ClassPathResource(classpath).getInputStream()) {
            Yaml yaml = new Yaml();
            Object raw = yaml.load(inputStream);
            return objectMapper.convertValue(raw, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load spec yaml: " + classpath, e);
        }
    }

    private Map<String, RolePolicy> indexRoles(List<RolePolicy> roles) {
        Map<String, RolePolicy> map = new LinkedHashMap<>();
        if (roles == null) {
            return map;
        }
        for (RolePolicy policy : roles) {
            map.put(policy.roleKey(), policy);
        }
        return map;
    }

    private Map<String, CapabilitySpec> indexCapabilities(List<CapabilitySpec> specs) {
        Map<String, CapabilitySpec> map = new LinkedHashMap<>();
        if (specs == null) {
            return map;
        }
        for (CapabilitySpec spec : specs) {
            map.put(spec.capabilityKey(), spec);
        }
        return map;
    }

    private Map<String, ToolSpec> indexTools(List<ToolSpec> specs) {
        Map<String, ToolSpec> map = new LinkedHashMap<>();
        if (specs == null) {
            return map;
        }
        for (ToolSpec spec : specs) {
            map.put(spec.toolKey(), spec);
        }
        return map;
    }
}
