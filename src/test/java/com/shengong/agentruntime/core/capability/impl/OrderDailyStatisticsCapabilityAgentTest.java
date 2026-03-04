package com.shengong.agentruntime.core.capability.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengong.agentruntime.core.capability.CapabilityRuntime;
import com.shengong.agentruntime.model.ToolResult;
import com.shengong.agentruntime.model.runtime.CapabilityRequest;
import com.shengong.agentruntime.model.runtime.CapabilityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderDailyStatisticsCapabilityAgentTest {

    private CapabilityRuntime runtime;
    private OrderDailyStatisticsCapabilityAgent agent;

    @BeforeEach
    void setup() {
        runtime = Mockito.mock(CapabilityRuntime.class);
        agent = new OrderDailyStatisticsCapabilityAgent(new ObjectMapper());
    }

    @Test
    void shouldReturnFormattedDataWhenToolAndRenderCapabilitySucceed() {
        ToolResult toolResult = ToolResult.success(Map.of(
                "body", "[{\"saleRegion\":\"GB\",\"statDate\":\"2026-03-01\",\"sampleCount\":1,\"creatorCount\":2,\"selfSellCount\":3,\"productCardCount\":4}]",
                "statusCode", 200
        ));
        when(runtime.callTool(eq("order.daily.statistics.fetch"), anyMap(), anyMap()))
                .thenReturn(toolResult);
        when(runtime.callCapability(eq("common.table_markdown_render"), anyMap(), anyMap(), anyInt()))
                .thenReturn(CapabilityResult.success("ok", Map.of(
                        "formattedData", "| 地区 | 日期 |\\n| --- | --- |\\n| 英国 | 2026-03-01 |",
                        "meta", Map.of("useLlm", false)
                )));

        CapabilityResult result = agent.execute(new CapabilityRequest(
                "order.daily_statistics",
                Map.of(
                        "dateRange", List.of("2026-03-01", "2026-03-01"),
                        "regions", List.of("GB")
                ),
                Map.of("formatter", Map.of("useLlm", true)),
                1
        ), runtime);

        assertTrue(result.isSuccess());
        assertEquals(200, result.getData().get("statusCode"));
        assertTrue(result.getData().containsKey("rawData"));
        assertTrue(result.getData().containsKey("formattedData"));

        ArgumentCaptor<Map<String, Object>> argsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(runtime).callCapability(eq("common.table_markdown_render"), argsCaptor.capture(), anyMap(), eq(2));
        Map<String, Object> renderArgs = argsCaptor.getValue();
        assertTrue(renderArgs.containsKey("columns"));
        assertTrue(renderArgs.containsKey("rows"));
        assertEquals(true, ((Map<?, ?>) renderArgs.get("options")).get("useLlm"));
    }

    @Test
    void shouldReturnFailureWhenToolCallFails() {
        when(runtime.callTool(eq("order.daily.statistics.fetch"), anyMap(), anyMap()))
                .thenReturn(ToolResult.failure("upstream failed"));

        CapabilityResult result = agent.execute(new CapabilityRequest(
                "order.daily_statistics",
                Map.of(),
                Map.of(),
                1
        ), runtime);

        assertFalse(result.isSuccess());
        assertEquals("TOOL_EXECUTION_FAILED", result.getErrors().get(0).get("code"));
    }

    @Test
    void shouldFallbackToRawBodyWhenJsonParseFails() {
        when(runtime.callTool(eq("order.daily.statistics.fetch"), anyMap(), anyMap()))
                .thenReturn(ToolResult.success(Map.of(
                        "body", "NOT_JSON",
                        "statusCode", 200
                )));

        CapabilityResult result = agent.execute(new CapabilityRequest(
                "order.daily_statistics",
                Map.of(),
                Map.of(),
                1
        ), runtime);

        assertTrue(result.isSuccess());
        assertEquals("NOT_JSON", result.getData().get("formattedData"));
        assertEquals("NOT_JSON", result.getData().get("rawBody"));
        verify(runtime, never()).callCapability(eq("common.table_markdown_render"), anyMap(), anyMap(), anyInt());
    }
}
