package com.shengong.agentruntime.core.agent.impl;

import com.shengong.agentruntime.core.agent.AbstractAgent;
import com.shengong.agentruntime.core.agent.annotation.AgentDefinition;
import com.shengong.agentruntime.core.param.AgentParam;
import com.shengong.agentruntime.core.tool.Tool;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import com.shengong.agentruntime.model.ToolResult;
import com.shengong.agentruntime.service.ToolRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
@AgentDefinition(
    name = "LiveDataFetchAgent",
    domains = {"live"},
    taskType = "live_data_fetch",
    description = "Fetch live streaming data from external services"
)
public class LiveDataFetchAgent extends AbstractAgent<LiveDataFetchAgent.LiveDataFetchParams> {

    private final ToolRegistry toolRegistry;

    public LiveDataFetchAgent(ToolRegistry toolRegistry) {
        super(LiveDataFetchParams.class);
        this.toolRegistry = toolRegistry;
    }

    @Data
    public static class LiveDataFetchParams {
        @AgentParam(required = true, description = "时间范围，包含 startTime 和 endTime，格式如: {\"startTime\": \"2025-11-20 00:00:00\", \"endTime\": \"2025-11-21 00:00:00\"}")
        private Map<String, Object> timeRange;

        @AgentParam(required = false, description = "过滤条件，如直播间ID、主播ID等，格式如: {\"liveRoomId\": \"12345\"}")
        private Map<String, Object> filters;
    }

    @Override
    protected AgentResult execute(AgentTask task, LiveDataFetchParams params) {
        log.info("LiveDataFetchAgent handling task: {}", task.getTaskId());

        try {
            // 参数验证通过，直接使用
            Map<String, Object> timeRange = params.getTimeRange();
            Map<String, Object> filters = params.getFilters();

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
}
