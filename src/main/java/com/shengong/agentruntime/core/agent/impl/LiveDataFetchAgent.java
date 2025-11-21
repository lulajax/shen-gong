package com.shengong.agentruntime.core.agent.impl;

import com.shengong.agentruntime.core.agent.Agent;
import com.shengong.agentruntime.core.agent.AgentType;
import com.shengong.agentruntime.core.tool.Tool;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import com.shengong.agentruntime.model.ToolResult;
import com.shengong.agentruntime.service.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/**
 * 直播数据获取 Agent
 * 从外部服务拉取直播间数据
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveDataFetchAgent implements Agent {

    private static final AgentType AGENT_TYPE = AgentType.LIVE_DATA_FETCH;

    private final ToolRegistry toolRegistry;

    @Override
    public String name() {
        return AGENT_TYPE.getName();
    }
    
    @Override
    public String taskType() {
        return AGENT_TYPE.getTaskType();
    }

    @Override
    public List<String> domains() {
        return AGENT_TYPE.getDomains();
    }

    @Override
    public boolean supports(String taskType, String domain) {
        return AGENT_TYPE.supports(taskType, domain);
    }

    @Override
    public List<String> requiredParams() {
        return List.of("timeRange");
    }

    @Override
    public List<String> optionalParams() {
        return List.of("filters");
    }

    @Override
    public Map<String, String> paramDescriptions() {
        return Map.of(
            "timeRange", "时间范围，包含 startTime 和 endTime，格式如: {\"startTime\": \"2025-11-20 00:00:00\", \"endTime\": \"2025-11-21 00:00:00\"}",
            "filters", "过滤条件，如直播间ID、主播ID等，格式如: {\"liveRoomId\": \"12345\"}"
        );
    }

    @Override
    public AgentResult handle(AgentTask task) {
        log.info("LiveDataFetchAgent handling task: {}", task.getTaskId());

        try {
            // 统一参数验证（自动识别路由层验证结果）
            AgentResult validationError = validateParams(task);
            if (validationError != null) {
                return validationError;
            }

            // 参数验证通过，直接使用
            Map<String, Object> timeRange = task.getParam("timeRange");
            Map<String, Object> filters = task.getParam("filters", Map.of());

            // 调用直播数据 Tool
            Tool liveTool = toolRegistry.getTool("live_data_tool")
                    .orElse(null);

            if (liveTool == null) {
                return AgentResult.error("Live data tool not available");
            }

            ToolResult toolResult = liveTool.invoke(Map.of(
                    "timeRange", timeRange,
                    "filters", filters != null ? filters : Map.of()
            ));

            if (!toolResult.isSuccess()) {
                return AgentResult.error("Failed to fetch live data: " + toolResult.getError());
            }

            // 返回原始数据
            return AgentResult.ok("Live data fetched successfully", Map.of(
                    "rawData", toolResult.getData()
            ));

        } catch (Exception e) {
            log.error("LiveDataFetchAgent failed: {}", e.getMessage(), e);
            return AgentResult.error("Data fetch failed: " + e.getMessage());
        }
    }

    @Override
    public String description() {
        return AGENT_TYPE.getDescription();
    }
}
