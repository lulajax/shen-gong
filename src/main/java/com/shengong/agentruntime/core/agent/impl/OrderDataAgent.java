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
 * 订单数据获取 Agent
 * 从订单服务拉取订单数据
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
@AgentDefinition(
    name = "OrderDataAgent",
    domains = {"order"},
    taskType = "anomaly_detection",
    description = "Fetch order data for anomaly detection"
)
public class OrderDataAgent extends AbstractAgent<OrderDataAgent.OrderDataParams> {

    private final ToolRegistry toolRegistry;

    public OrderDataAgent(ToolRegistry toolRegistry) {
        super(OrderDataParams.class);
        this.toolRegistry = toolRegistry;
    }

    @Data
    public static class OrderDataParams {
        @AgentParam(required = true, description = "时间范围，包含 startTime 和 endTime")
        private Map<String, Object> timeRange;

        @AgentParam(required = false, defaultValue = "all", description = "查询类型，如: all, refunded, delayed 等")
        private String queryType;
    }

    @Override
    protected AgentResult execute(AgentTask task, OrderDataParams params) {
        log.info("OrderDataAgent handling task: {}", task.getTaskId());

        try {
            Map<String, Object> timeRange = params.getTimeRange();
            String queryType = params.getQueryType();
            if (queryType == null || queryType.isEmpty()) {
                queryType = "all";
            }

            // 调用订单数据 Tool
            Tool orderTool = toolRegistry.getTool("order_data_tool")
                    .orElse(null);

            if (orderTool == null) {
                return AgentResult.error("Order data tool not available");
            }

            ToolResult toolResult = orderTool.invoke(Map.of(
                    "timeRange", timeRange,
                    "queryType", queryType
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
}
