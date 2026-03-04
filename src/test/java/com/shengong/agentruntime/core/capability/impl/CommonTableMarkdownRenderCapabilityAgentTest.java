package com.shengong.agentruntime.core.capability.impl;

import com.shengong.agentruntime.llm.LlmClient;
import com.shengong.agentruntime.model.runtime.CapabilityRequest;
import com.shengong.agentruntime.model.runtime.CapabilityResult;
import com.shengong.agentruntime.model.spec.CapabilitySpec;
import com.shengong.agentruntime.service.runtime.formatter.MarkdownTableRenderer;
import com.shengong.agentruntime.service.runtime.registry.DynamicSpecRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommonTableMarkdownRenderCapabilityAgentTest {

    private LlmClient llmClient;
    private DynamicSpecRegistry specRegistry;
    private CommonTableMarkdownRenderCapabilityAgent agent;

    @BeforeEach
    void setup() {
        llmClient = Mockito.mock(LlmClient.class);
        specRegistry = Mockito.mock(DynamicSpecRegistry.class);
        agent = new CommonTableMarkdownRenderCapabilityAgent(new MarkdownTableRenderer(), llmClient, specRegistry);
    }

    @Test
    void shouldRenderByRuleWhenLlmDisabled() {
        when(specRegistry.findCapabilitySpec("common.table_markdown_render"))
                .thenReturn(Optional.of(specWithLlm(false)));

        CapabilityResult result = agent.execute(new CapabilityRequest(
                "common.table_markdown_render",
                Map.of(
                        "columns", List.of(
                                Map.of("key", "region", "header", "地区"),
                                Map.of("key", "date", "header", "日期")
                        ),
                        "rows", List.of(
                                Map.of("region", "英国", "date", "2026-03-01")
                        )
                ),
                Map.of(),
                1
        ), null);

        assertTrue(result.isSuccess());
        assertTrue(String.valueOf(result.getData().get("formattedData")).contains("| 地区 | 日期 |"));
        assertEquals(false, ((Map<?, ?>) result.getData().get("meta")).get("useLlm"));
        verify(llmClient, never()).chat(anyString(), anyString());
    }

    @Test
    void shouldCallLlmWhenContextOverridesUseLlmTrue() {
        when(specRegistry.findCapabilitySpec("common.table_markdown_render"))
                .thenReturn(Optional.of(specWithLlm(false)));
        when(llmClient.chat(anyString(), anyString()))
                .thenReturn("```markdown\n| 地区 | 日期 |\n| --- | --- |\n| 英国 | 2026-03-01 |\n```");

        CapabilityResult result = agent.execute(new CapabilityRequest(
                "common.table_markdown_render",
                Map.of(
                        "columns", List.of(
                                Map.of("key", "region", "header", "地区"),
                                Map.of("key", "date", "header", "日期")
                        ),
                        "rows", List.of(
                                Map.of("region", "英国", "date", "2026-03-01")
                        )
                ),
                Map.of("formatter", Map.of("useLlm", true)),
                1
        ), null);

        assertTrue(result.isSuccess());
        assertEquals(true, ((Map<?, ?>) result.getData().get("meta")).get("useLlm"));
        assertEquals(true, ((Map<?, ?>) result.getData().get("meta")).get("llmApplied"));
        verify(llmClient).chat(anyString(), anyString());
    }

    @Test
    void shouldReturnNoDataWhenRowsEmpty() {
        when(specRegistry.findCapabilitySpec("common.table_markdown_render"))
                .thenReturn(Optional.of(specWithLlm(true)));

        CapabilityResult result = agent.execute(new CapabilityRequest(
                "common.table_markdown_render",
                Map.of(
                        "columns", List.of(
                                Map.of("key", "region", "header", "地区")
                        ),
                        "rows", List.of()
                ),
                Map.of(),
                1
        ), null);

        assertTrue(result.isSuccess());
        assertEquals("暂无数据", result.getData().get("formattedData"));
        assertEquals(false, ((Map<?, ?>) result.getData().get("meta")).get("llmApplied"));
    }

    private CapabilitySpec specWithLlm(boolean llmEnabled) {
        return new CapabilitySpec(
                "common.table_markdown_render",
                "desc",
                "prompt",
                "local",
                List.of(),
                Map.of(),
                Map.of(),
                Map.of("llmEnabled", llmEnabled)
        );
    }
}
