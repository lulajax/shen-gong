package com.shengong.agentruntime.core.tool.impl;

import com.shengong.agentruntime.core.tool.AbstractTool;
import com.shengong.agentruntime.core.tool.annotation.ToolDefinition;
import com.shengong.agentruntime.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 订单数据 Tool (Mock 实现)
 * 模拟从订单服务拉取数据
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@ToolDefinition(
        name = "order_data_tool",
        description = "Fetch order data from order service",
        category = "data-source"
)
public class OrderDataTool extends AbstractTool {

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult invoke(Map<String, Object> arguments) {
        try {
            Map<String, Object> timeRange = (Map<String, Object>) arguments.get("timeRange");

            log.info("Fetching order data: timeRange={}", timeRange);

            // 模拟订单数据
            Map<String, Object> mockData = generateMockOrderData();

            return ToolResult.success(mockData);

        } catch (Exception e) {
            log.error("Order data fetch failed: {}", e.getMessage(), e);
            return ToolResult.failure("Failed to fetch order data: " + e.getMessage());
        }
    }

    private Map<String, Object> generateMockOrderData() {
        List<Map<String, Object>> orders = new ArrayList<>();
        Random random = new Random();

        // 生成100个模拟订单
        for (int i = 1; i <= 100; i++) {
            Map<String, Object> order = new HashMap<>();
            order.put("orderId", "ORD" + String.format("%06d", i));
            order.put("status", random.nextDouble() < 0.2 ? "REFUNDED" : "COMPLETED");
            order.put("amount", 100 + random.nextDouble() * 500);
            order.put("delayedShipment", random.nextDouble() < 0.12);
            order.put("processingTimeHours", 12 + random.nextDouble() * 60);
            orders.add(order);
        }

        return Map.of("orderList", orders, "totalCount", orders.size());
    }
}
