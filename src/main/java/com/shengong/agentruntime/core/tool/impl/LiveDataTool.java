package com.shengong.agentruntime.core.tool.impl;

import com.shengong.agentruntime.core.tool.AbstractTool;
import com.shengong.agentruntime.core.tool.annotation.ToolDefinition;
import com.shengong.agentruntime.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 直播数据 Tool (Mock 实现)
 * 模拟从直播服务拉取数据
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@ToolDefinition(
        name = "live_data_tool",
        description = "Fetch live streaming data from external service",
        category = "data-source"
)
public class LiveDataTool extends AbstractTool {

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult invoke(Map<String, Object> arguments) {
        try {
            Map<String, Object> timeRange = (Map<String, Object>) arguments.get("timeRange");
            Map<String, Object> filters = (Map<String, Object>) arguments.getOrDefault("filters", Map.of());

            log.info("Fetching live data: timeRange={}, filters={}", timeRange, filters);

            // 模拟数据
            Map<String, Object> mockData = generateMockLiveData(timeRange, filters);

            return ToolResult.success(mockData);

        } catch (Exception e) {
            log.error("Live data fetch failed: {}", e.getMessage(), e);
            return ToolResult.failure("Failed to fetch live data: " + e.getMessage());
        }
    }

    private Map<String, Object> generateMockLiveData(Map<String, Object> timeRange,
                                                      Map<String, Object> filters) {
        Map<String, Object> data = new HashMap<>();

        data.put("timeRange", timeRange);
        data.put("filters", filters);

        // 模拟直播指标
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalGmv", 12345.67 + new Random().nextDouble() * 5000);
        metrics.put("previousGmv", 17000.0);
        metrics.put("peakViewers", 980 + new Random().nextInt(200));
        metrics.put("avgViewers", 430 + new Random().nextInt(100));
        metrics.put("totalViewers", 2500 + new Random().nextInt(500));
        metrics.put("giftCount", 230 + new Random().nextInt(50));
        metrics.put("commentCount", 1200 + new Random().nextInt(300));
        metrics.put("likeCount", 5600 + new Random().nextInt(1000));
        metrics.put("orderCount", 87 + new Random().nextInt(20));

        data.put("liveMetrics", metrics);

        // 模拟时间段数据
        List<Map<String, Object>> timeline = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            timeline.add(Map.of(
                    "timeSlot", String.format("%02d:00-%02d:00", i * 2, i * 2 + 2),
                    "viewers", 300 + new Random().nextInt(500),
                    "gmv", 1000 + new Random().nextDouble() * 2000
            ));
        }
        data.put("timeline", timeline);

        return data;
    }
}
