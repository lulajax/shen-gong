package com.shengong.agentruntime.service.runtime.formatter;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Markdown 表格规则渲染器。
 * 基于 columns + rows 结构化输入稳定输出 Markdown 表格。
 */
@Component
public class MarkdownTableRenderer {

    public String render(List<Map<String, Object>> columns,
                         List<Map<String, Object>> rows,
                         Map<String, Object> options) {
        String title = resolveString(options, "title", "");
        int maxRows = resolveInt(options, "maxRows", 0);

        String prefix = "";
        if (!title.isBlank()) {
            prefix = "### " + safeCell(title) + "\n\n";
        }

        if (rows == null || rows.isEmpty()) {
            return prefix + "暂无数据";
        }

        List<Map<String, Object>> effectiveRows = rows;
        if (maxRows > 0 && rows.size() > maxRows) {
            effectiveRows = new ArrayList<>(rows.subList(0, maxRows));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        appendHeader(sb, columns);
        appendSeparator(sb, columns);
        appendRows(sb, columns, effectiveRows);
        return sb.toString();
    }

    private void appendHeader(StringBuilder sb, List<Map<String, Object>> columns) {
        sb.append("|");
        for (Map<String, Object> col : columns) {
            sb.append(" ").append(safeCell(col.get("header"))).append(" |");
        }
        sb.append("\n");
    }

    private void appendSeparator(StringBuilder sb, List<Map<String, Object>> columns) {
        sb.append("|");
        for (int i = 0; i < columns.size(); i++) {
            sb.append(" --- |");
        }
        sb.append("\n");
    }

    private void appendRows(StringBuilder sb, List<Map<String, Object>> columns, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            sb.append("|");
            for (Map<String, Object> col : columns) {
                String key = String.valueOf(col.getOrDefault("key", ""));
                Object value = row != null ? row.get(key) : null;
                sb.append(" ").append(safeCell(value)).append(" |");
            }
            sb.append("\n");
        }
    }

    private String safeCell(Object value) {
        if (value == null) {
            return "-";
        }
        return String.valueOf(value)
                .replace("|", "\\|")
                .replace("\r\n", "<br/>")
                .replace("\n", "<br/>");
    }

    private int resolveInt(Map<String, Object> options, String key, int defaultValue) {
        if (options == null || !options.containsKey(key) || options.get(key) == null) {
            return defaultValue;
        }
        Object value = options.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String resolveString(Map<String, Object> options, String key, String defaultValue) {
        if (options == null || !options.containsKey(key) || options.get(key) == null) {
            return defaultValue;
        }
        return String.valueOf(options.get(key));
    }
}
