package com.shengong.agentruntime.core.capability.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengong.agentruntime.core.capability.CapabilityAgent;
import com.shengong.agentruntime.core.capability.CapabilityRuntime;
import com.shengong.agentruntime.model.ToolResult;
import com.shengong.agentruntime.model.runtime.CapabilityRequest;
import com.shengong.agentruntime.model.runtime.CapabilityResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 地区订单日报能力代理。
 * 负责调用订单日报工具并将原始结果格式化为可读的 Markdown 输出。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDailyStatisticsCapabilityAgent implements CapabilityAgent {

    private final ObjectMapper objectMapper;

    @Override
    public String capabilityKey() {
        return "order.daily_statistics";
    }

    @Override
    @SuppressWarnings("unchecked")
    public CapabilityResult execute(CapabilityRequest request, CapabilityRuntime runtime) {
        Map<String, Object> args = request.args() != null ? request.args() : Map.of();

        ToolResult toolResult = runtime.callTool("order.daily.statistics.fetch", args, request.context());
        if (!toolResult.isSuccess()) {
            return CapabilityResult.failure("TOOL_EXECUTION_FAILED",
                    "order.daily.statistics.fetch failed: "                 + toolResult.getError());
        }

        Map<String, Object> toolData = toolResult.getData() != null ? toolResult.getData() : Map.of();
        String body = String.valueOf(toolData.getOrDefault("body", "[]"));

        try {
            List<Map<String, Object>> rawData = objectMapper.readValue(body, new TypeReference<List<Map<String, Object>>>() {});
            List<Map<String, Object>> columns = buildColumns();
            List<Map<String, Object>> rows = buildRows(rawData);
            Map<String, Object> renderArgs = new HashMap<>();
            renderArgs.put("columns", columns);
            renderArgs.put("rows", rows);
            Map<String, Object> renderOptions = buildRenderOptions(request.context());
            if (!renderOptions.isEmpty()) {
                renderArgs.put("options", renderOptions);
            }

            CapabilityResult renderResult = runtime.callCapability(
                    "common.table_markdown_render",
                    renderArgs,
                    request.context(),
                    request.depth() + 1
            );
            if (!renderResult.isSuccess()) {
                return CapabilityResult.failure(
                        "CAPABILITY_EXECUTION_FAILED",
                        "common.table_markdown_render failed: " + renderResult.getSummary()
                );
            }

            String markdown = String.valueOf(renderResult.getData().getOrDefault("formattedData", "暂无数据"));

            Map<String, Object> result = new HashMap<>();
            result.put("formattedData", markdown);
            result.put("rawData", rawData);
            result.put("statusCode", toolData.getOrDefault("statusCode", 200));
            if (renderResult.getData().get("meta") != null) {
                result.put("renderMeta", renderResult.getData().get("meta"));
            }

            return CapabilityResult.success("订单每日统计查询成功", result);
        } catch (Exception e) {
            log.warn("order.daily_statistics parse failed, fallback raw body: {}", e.getMessage());
            return CapabilityResult.success("订单每日统计查询完成", Map.of(
                    "formattedData", body,
                    "rawBody", body,
                    "statusCode", toolData.getOrDefault("statusCode", 200)
            ));
        }
    }

    private List<Map<String, Object>> buildColumns() {
        List<Map<String, Object>> columns = new ArrayList<>();
        columns.add(Map.of("key", "region", "header", "地区"));
        columns.add(Map.of("key", "date", "header", "日期"));
        columns.add(Map.of("key", "sampleCount", "header", "样品订单"));
        columns.add(Map.of("key", "creatorCount", "header", "达人订单"));
        columns.add(Map.of("key", "selfSellCount", "header", "自营订单"));
        columns.add(Map.of("key", "productCardCount", "header", "商品卡订单"));
        columns.add(Map.of("key", "realOrders", "header", "真实订单"));
        return columns;
    }

    private List<Map<String, Object>> buildRows(List<Map<String, Object>> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> row : dataList) {
            String region = String.valueOf(row.getOrDefault("saleRegion", ""));
            String date = String.valueOf(row.getOrDefault("statDate", ""));
            int sampleCount = numberInt(row.get("sampleCount"));
            int creatorCount = numberInt(row.get("creatorCount"));
            int selfSellCount = numberInt(row.get("selfSellCount"));
            int productCardCount = numberInt(row.get("productCardCount"));
            int realOrders = creatorCount + selfSellCount + productCardCount;

            Map<String, Object> normalizedRow = new HashMap<>();
            normalizedRow.put("region", region);
            normalizedRow.put("date", date);
            normalizedRow.put("sampleCount", sampleCount);
            normalizedRow.put("creatorCount", creatorCount);
            normalizedRow.put("selfSellCount", selfSellCount);
            normalizedRow.put("productCardCount", productCardCount);
            normalizedRow.put("realOrders", realOrders);
            rows.add(normalizedRow);
        }

        return rows;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildRenderOptions(Map<String, Object> context) {
        if (context == null || !(context.get("formatter") instanceof Map<?, ?> formatter)) {
            return Map.of();
        }
        Object useLlm = ((Map<String, Object>) formatter).get("useLlm");
        if (useLlm == null) {
            return Map.of();
        }
        return Map.of("useLlm", Boolean.parseBoolean(String.valueOf(useLlm)));
    }

    private int numberInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

}
