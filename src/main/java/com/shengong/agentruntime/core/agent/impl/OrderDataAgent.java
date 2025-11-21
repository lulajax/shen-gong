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
 * 订单数据获取 Agent
 * 从订单服务拉取订单数据
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDataAgent implements Agent {

    private static final AgentType AGENT_TYPE = AgentType.ORDER_DATA;

    private final ToolRegistry toolRegistry;

    @Override
    public String name() {
        return AGENT_TYPE.getName();
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
    public AgentResult handle(AgentTask task) {
        log.info("OrderDataAgent handling task: {}", task.getTaskId());

        try {
            Map<String, Object> timeRange = task.getPayloadValue("timeRange");
            if (timeRange == null) {
                return AgentResult.error("Missing required parameter: timeRange");
            }

            // 调用订单数据 Tool
            Tool orderTool = toolRegistry.getTool("order_data_tool")
                    .orElse(null);

            if (orderTool == null) {
                return AgentResult.error("Order data tool not available");
            }

            ToolResult toolResult = orderTool.invoke(Map.of(
                    "timeRange", timeRange,
                    "queryType", task.getPayloadValue("queryType")
            ));

            if (!toolResult.isSuccess()) {
                return AgentResult.error("Failed to fetch order data: " + toolResult.getError());
            }

            return AgentResult.ok("Order data fetched successfully", Map.of(
                    "orders", toolResult.getData()
            ));

        } catch (Exception e) {
            log.error("OrderDataAgent failed: {}", e.getMessage(), e);
            return AgentResult.error("Data fetch failed: " + e.getMessage());
        }
    }

    @Override
    public String description() {
        return AGENT_TYPE.getDescription();
    }
}
