package com.shengong.agentruntime.service.runtime.orchestration;

import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.runtime.CapabilityResult;
import com.shengong.agentruntime.model.runtime.PlannerAction;
import com.shengong.agentruntime.model.runtime.RoleRequest;
import com.shengong.agentruntime.model.spec.CapabilitySpec;
import com.shengong.agentruntime.model.spec.RolePolicy;
import com.shengong.agentruntime.service.runtime.argument.CapabilityArgExtractor;
import com.shengong.agentruntime.service.runtime.capability.executor.CapabilityExecutor;
import com.shengong.agentruntime.service.runtime.registry.DynamicSpecRegistry;
import com.shengong.agentruntime.service.runtime.tool.ToolExecutor;
import com.shengong.agentruntime.service.runtime.validation.JsonSchemaValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CapabilityOrchestratorTest {

    private DynamicSpecRegistry specRegistry;
    private CapabilityPlanner planner;
    private ToolExecutor toolExecutor;
    private CapabilityExecutor capabilityExecutor;
    private CapabilityArgExtractor capabilityArgExtractor;
    private JsonSchemaValidationService schemaValidationService;
    private CapabilityOrchestrator orchestrator;

    @BeforeEach
    void setup() {
        specRegistry = Mockito.mock(DynamicSpecRegistry.class);
        planner = Mockito.mock(CapabilityPlanner.class);
        toolExecutor = Mockito.mock(ToolExecutor.class);
        capabilityExecutor = Mockito.mock(CapabilityExecutor.class);
        capabilityArgExtractor = Mockito.mock(CapabilityArgExtractor.class);
        schemaValidationService = new JsonSchemaValidationService(new com.fasterxml.jackson.databind.ObjectMapper());

        orchestrator = new CapabilityOrchestrator(
                specRegistry,
                planner,
                toolExecutor,
                List.of(capabilityExecutor),
                capabilityArgExtractor,
                schemaValidationService
        );

        ReflectionTestUtils.setField(orchestrator, "maxSteps", 8);
        ReflectionTestUtils.setField(orchestrator, "maxDepth", 2);
    }

    @Test
    void shouldReturnRoleNotFoundWhenPolicyMissing() {
        when(specRegistry.findRolePolicy("unknown")).thenReturn(Optional.empty());

        AgentResult result = orchestrator.executeRole(new RoleRequest(
                "unknown",
                "u1",
                "s1",
                "查询英国订单",
                Map.of(),
                Map.of()
        ));

        assertFalse(result.isSuccess());
        assertTrue(result.getSummary().contains("ROLE_NOT_FOUND"));
    }

    @Test
    void shouldExecuteSingleCapabilityDirectly() {
        RolePolicy policy = new RolePolicy(
                "ecom_assistant",
                List.of("order.daily_statistics"),
                List.of(),
                Map.of()
        );
        CapabilitySpec spec = dailyStatisticsSpec();

        when(specRegistry.findRolePolicy("ecom_assistant")).thenReturn(Optional.of(policy));
        when(specRegistry.findCapabilitySpec("order.daily_statistics")).thenReturn(Optional.of(spec));
        when(capabilityArgExtractor.extractArgs(
                eq("order.daily_statistics"), anyString(), anyMap(), anyMap(), anyMap()))
                .thenReturn(Map.of(
                        "dateRange", List.of("2026-03-01", "2026-03-01"),
                        "regions", List.of("GB")
                ));
        when(capabilityExecutor.supports(spec)).thenReturn(true);
        when(capabilityExecutor.execute(any(), anyMap(), anyMap(), anyInt(), any())).thenReturn(
                CapabilityResult.success("ok", Map.of("formattedData", "|table|"))
        );

        AgentResult result = orchestrator.executeRole(new RoleRequest(
                "ecom_assistant",
                "u1",
                "s1",
                "帮我查英国昨天订单日报",
                Map.of(),
                Map.of()
        ));

        assertTrue(result.isSuccess());
        assertEquals("single_capability_direct", result.getDebug().get("mode"));
        assertEquals("order.daily_statistics", result.getData().get("capability"));
        verify(planner, never()).planNextAction(anyString(), anyString(), anyMap(), anyList(), anyList(), anyInt(), anyInt());
    }

    @Test
    void shouldUsePlannerWhenForcePlannerEnabled() {
        RolePolicy policy = new RolePolicy(
                "ecom_assistant",
                List.of("order.daily_statistics"),
                List.of(),
                Map.of()
        );

        when(specRegistry.findRolePolicy("ecom_assistant")).thenReturn(Optional.of(policy));
        when(planner.planNextAction(anyString(), anyString(), anyMap(), anyList(), anyList(), anyInt(), anyInt()))
                .thenReturn(new PlannerAction(
                        PlannerAction.FINAL_ANSWER,
                        null,
                        null,
                        Map.of(),
                        "planner done",
                        "forced"
                ));

        AgentResult result = orchestrator.executeRole(new RoleRequest(
                "ecom_assistant",
                "u1",
                "s1",
                "查地区订单",
                Map.of("forcePlanner", true),
                Map.of()
        ));

        assertTrue(result.isSuccess());
        assertEquals("planner done", result.getSummary());
        verify(planner).planNextAction(anyString(), anyString(), anyMap(), anyList(), anyList(), anyInt(), anyInt());
        verify(capabilityArgExtractor, never()).extractArgs(anyString(), anyString(), anyMap(), anyMap(), anyMap());
    }

    @Test
    void shouldCallArgExtractorWithPayloadPriority() {
        RolePolicy policy = new RolePolicy(
                "ecom_assistant",
                List.of("order.daily_statistics"),
                List.of(),
                Map.of()
        );
        CapabilitySpec spec = dailyStatisticsSpec();

        when(specRegistry.findRolePolicy("ecom_assistant")).thenReturn(Optional.of(policy));
        when(specRegistry.findCapabilitySpec("order.daily_statistics")).thenReturn(Optional.of(spec));
        when(capabilityArgExtractor.extractArgs(
                eq("order.daily_statistics"), anyString(), anyMap(), anyMap(), anyMap()))
                .thenReturn(Map.of(
                        "dateRange", List.of("2026-03-01", "2026-03-02"),
                        "regions", List.of("US", "GB")
                ));
        when(capabilityExecutor.supports(spec)).thenReturn(true);
        when(capabilityExecutor.execute(any(), anyMap(), anyMap(), anyInt(), any())).thenReturn(
                CapabilityResult.success("ok", Map.of("formattedData", "|table|"))
        );

        orchestrator.executeRole(new RoleRequest(
                "ecom_assistant",
                "u1",
                "s1",
                "查询美英两地 3月1日到3月2日订单",
                Map.of(),
                Map.of("regions", List.of("US"))
        ));

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(capabilityArgExtractor).extractArgs(
                eq("order.daily_statistics"),
                anyString(),
                payloadCaptor.capture(),
                anyMap(),
                anyMap()
        );
        assertEquals(List.of("US"), payloadCaptor.getValue().get("regions"));
    }

    @Test
    void shouldReturnSchemaValidationErrorWhenExtractorCannotFillRequiredArgs() {
        RolePolicy policy = new RolePolicy(
                "ecom_assistant",
                List.of("order.daily_statistics"),
                List.of(),
                Map.of()
        );
        CapabilitySpec spec = dailyStatisticsSpec();

        when(specRegistry.findRolePolicy("ecom_assistant")).thenReturn(Optional.of(policy));
        when(specRegistry.findCapabilitySpec("order.daily_statistics")).thenReturn(Optional.of(spec));
        when(capabilityArgExtractor.extractArgs(
                eq("order.daily_statistics"), anyString(), anyMap(), anyMap(), anyMap()))
                .thenReturn(Map.of());

        AgentResult result = orchestrator.executeRole(new RoleRequest(
                "ecom_assistant",
                "u1",
                "s1",
                "帮我查日报",
                Map.of(),
                Map.of()
        ));

        assertEquals("partial", result.getStatus());
        assertTrue(result.getErrors().stream().anyMatch(err -> err.contains("SCHEMA_VALIDATION_FAILED")));
        assertEquals("order.daily_statistics", result.getData().get("capability"));
    }

    private CapabilitySpec dailyStatisticsSpec() {
        return new CapabilitySpec(
                "order.daily_statistics",
                "desc",
                "prompt",
                "local",
                List.of("order.daily.statistics.fetch"),
                Map.of(
                        "$schema", "https://json-schema.org/draft/2020-12/schema",
                        "type", "object",
                        "required", List.of("dateRange", "regions"),
                        "additionalProperties", false,
                        "properties", Map.of(
                                "dateRange", Map.of(
                                        "type", "array",
                                        "minItems", 2,
                                        "maxItems", 2,
                                        "items", Map.of("type", "string")
                                ),
                                "regions", Map.of(
                                        "type", "array",
                                        "minItems", 1,
                                        "items", Map.of("type", "string")
                                )
                        )
                ),
                Map.of(
                        "$schema", "https://json-schema.org/draft/2020-12/schema",
                        "type", "object",
                        "required", List.of("formattedData"),
                        "additionalProperties", true,
                        "properties", Map.of(
                                "formattedData", Map.of("type", "string")
                        )
                ),
                Map.of()
        );
    }
}
