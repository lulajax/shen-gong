package com.shengong.agentruntime.service.runtime.formatter;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownTableRendererTest {

    private final MarkdownTableRenderer renderer = new MarkdownTableRenderer();

    @Test
    void shouldKeepColumnOrder() {
        String markdown = renderer.render(
                List.of(
                        Map.of("key", "region", "header", "地区"),
                        Map.of("key", "date", "header", "日期"),
                        Map.of("key", "realOrders", "header", "真实订单")
                ),
                List.of(
                        Map.of("region", "英国", "date", "2026-03-01", "realOrders", 9)
                ),
                Map.of()
        );

        assertTrue(markdown.contains("| 地区 | 日期 | 真实订单 |"));
        assertTrue(markdown.contains("| 英国 | 2026-03-01 | 9 |"));
    }

    @Test
    void shouldHandleNullAndMixedValues() {
        java.util.Map<String, Object> row = new java.util.HashMap<>();
        row.put("region", null);
        row.put("note", "中文|line\nnext");

        String markdown = renderer.render(
                List.of(
                        Map.of("key", "region", "header", "地区"),
                        Map.of("key", "note", "header", "备注")
                ),
                List.of(row),
                Map.of()
        );

        assertTrue(markdown.contains("| - | 中文\\|line<br/>next |"));
    }

    @Test
    void shouldApplyMaxRowsOption() {
        String markdown = renderer.render(
                List.of(
                        Map.of("key", "region", "header", "地区")
                ),
                List.of(
                        Map.of("region", "英国"),
                        Map.of("region", "美国")
                ),
                Map.of("maxRows", 1)
        );

        int first = markdown.indexOf("| 英国 |");
        int second = markdown.indexOf("| 美国 |");
        assertTrue(first >= 0);
        assertEquals(-1, second);
    }
}
