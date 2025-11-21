package com.shengong.agentruntime.service;

import com.shengong.agentruntime.core.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool 注册中心
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Service
public class ToolRegistry {

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();
    private final Map<String, Set<Tool>> categoryTools = new ConcurrentHashMap<>();

    /**
     * 注册 Tool
     */
    public void register(Tool tool) {
        String name = tool.name();
        if (tools.containsKey(name)) {
            log.warn("Tool {} already registered, will be replaced", name);
        }

        tools.put(name, tool);

        // 按类别注册
        categoryTools.computeIfAbsent(tool.category(), k -> new HashSet<>()).add(tool);

        log.info("Registered tool: {} (category: {})", name, tool.category());
    }

    /**
     * 根据名称获取 Tool
     */
    public Optional<Tool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /**
     * 获取所有 Tool
     */
    public Collection<Tool> getAllTools() {
        return Collections.unmodifiableCollection(tools.values());
    }

    /**
     * 获取指定类别的所有 Tool
     */
    public Set<Tool> getToolsByCategory(String category) {
        return categoryTools.getOrDefault(category, Collections.emptySet());
    }

    /**
     * 获取注册数量
     */
    public int getCount() {
        return tools.size();
    }
}
