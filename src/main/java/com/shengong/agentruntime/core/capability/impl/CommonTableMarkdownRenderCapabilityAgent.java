package com.shengong.agentruntime.core.capability.impl;

import com.shengong.agentruntime.core.capability.CapabilityAgent;
import com.shengong.agentruntime.core.capability.CapabilityRuntime;
import com.shengong.agentruntime.llm.LlmClient;
import com.shengong.agentruntime.model.runtime.CapabilityRequest;
import com.shengong.agentruntime.model.runtime.CapabilityResult;
import com.shengong.agentruntime.service.runtime.formatter.MarkdownTableRenderer;
import com.shengong.agentruntime.service.runtime.registry.DynamicSpecRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用 Markdown 表格渲染能力。
 * 输入 columns + rows，输出稳定 Markdown 表格，并支持可选 LLM 增强。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommonTableMarkdownRenderCapabilityAgent implements CapabilityAgent {

    private final MarkdownTableRenderer markdownTableRenderer;
    private final LlmClient llmClient;
    private final DynamicSpecRegistry specRegistry;

    @Override
    public String capabilityKey() {
        return "common.table_markdown_render";
    }

    @Override
    @SuppressWarnings("unchecked")
    public CapabilityResult execute(CapabilityRequest request, CapabilityRuntime runtime) {
        Map<String, Object> args = request.args() != null ? request.args() : Map.of();
        List<Map<String, Object>> columns = args.get("columns") instanceof List<?> list
                ? (List<Map<String, Object>>) list
                : List.of();
        List<Map<String, Object>> rows = args.get("rows") instanceof List<?> list
                ? (List<Map<String, Object>>) list
                : List.of();
        Map<String, Object> options = args.get("options") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();

        try {
            String markdown = markdownTableRenderer.render(columns, rows, options);
            boolean useLlm = resolveUseLlm(request.context(), options);
            boolean llmApplied = false;
            if (useLlm && !markdown.isBlank() && !"暂无数据".equals(markdown.trim())) {
                String enhanced = enhanceWithLlm(markdown, columns, rows.size());
                if (enhanced != null && !enhanced.isBlank()) {
                    markdown = enhanced;
                    llmApplied = true;
                }
            }

            Map<String, Object> meta = new HashMap<>();
            meta.put("rowCount", rows.size());
            meta.put("columnCount", columns.size());
            meta.put("useLlm", useLlm);
            meta.put("llmApplied", llmApplied);

            return CapabilityResult.success("Markdown 表格渲染成功", Map.of(
                    "formattedData", markdown,
                    "meta", meta
            ));
        } catch (Exception e) {
            log.error("common.table_markdown_render failed", e);
            return CapabilityResult.failure("CAPABILITY_EXECUTION_FAILED", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private boolean resolveUseLlm(Map<String, Object> context, Map<String, Object> options) {
        if (context != null && context.get("formatter") instanceof Map<?, ?> formatter) {
            Object useLlm = ((Map<String, Object>) formatter).get("useLlm");
            if (useLlm != null) {
                return Boolean.parseBoolean(String.valueOf(useLlm));
            }
        }

        if (options != null && options.get("useLlm") != null) {
            return Boolean.parseBoolean(String.valueOf(options.get("useLlm")));
        }

        Map<String, Object> config = specRegistry.findCapabilitySpec(capabilityKey())
                .map(spec -> spec.config() != null ? spec.config() : Map.<String, Object>of())
                .orElse(Map.of());
        return Boolean.parseBoolean(String.valueOf(config.getOrDefault("llmEnabled", false)));
    }

    private String enhanceWithLlm(String markdown, List<Map<String, Object>> columns, int rowCount) {
        try {
            String systemPrompt = """
                    你是 Markdown 表格润色助手。
                    你必须遵守：
                    1. 不允许新增或删除任何列和行。
                    2. 不允许改变列顺序。
                    3. 仅输出 Markdown 表格，不要附加说明。
                    4. 无法优化时原样返回。
                    """;
            String userPrompt = "列数=" + columns.size() + ", 行数=" + rowCount + "。请润色下列表格：\n\n" + markdown;
            String response = llmClient.chat(systemPrompt, userPrompt);
            String cleaned = stripCodeFence(response);
            if (isLikelyMarkdownTable(cleaned)) {
                return cleaned;
            }
            return markdown;
        } catch (Exception e) {
            log.warn("enhance markdown with llm failed: {}", e.getMessage());
            return markdown;
        }
    }

    private boolean isLikelyMarkdownTable(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String[] lines = text.split("\\R");
        return lines.length >= 2 && lines[0].contains("|") && lines[1].contains("|");
    }

    private String stripCodeFence(String value) {
        String text = value == null ? "" : value.trim();
        if (text.startsWith("```markdown")) {
            text = text.substring("```markdown".length()).trim();
        } else if (text.startsWith("```")) {
            text = text.substring(3).trim();
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3).trim();
        }
        return text;
    }
}
