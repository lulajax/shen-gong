package com.shengong.agentruntime.controller;

import com.shengong.agentruntime.core.role.RoleAgent;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.runtime.RoleRequest;
import com.shengong.agentruntime.service.runtime.registry.RoleAgentRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleAgentControllerTest {

    private RoleAgentRegistry roleAgentRegistry;
    private RoleAgent roleAgent;
    private RoleAgentController controller;

    @BeforeEach
    void setup() {
        roleAgentRegistry = Mockito.mock(RoleAgentRegistry.class);
        roleAgent = Mockito.mock(RoleAgent.class);
        controller = new RoleAgentController(roleAgentRegistry);

        when(roleAgent.handle(any(RoleRequest.class)))
                .thenReturn(AgentResult.ok("ok", Map.of("formattedData", "|table|")));
    }

    @Test
    void shouldMapChineseRoleAliasToEcomAssistant() {
        when(roleAgentRegistry.find("ecom_assistant")).thenReturn(Optional.of(roleAgent));

        RoleAgentController.RoleAgentRequest request = new RoleAgentController.RoleAgentRequest();
        request.setRole("电商小助理");
        request.setUserId("u1");
        request.setSessionId("s1");
        request.setInputText("查询英国订单日报");
        request.setContext(Map.of());
        request.setPayload(Map.of());

        ResponseEntity<RoleAgentController.RoleAgentResponse> response = controller.send(request);

        verify(roleAgentRegistry).find("ecom_assistant");
        assertNotNull(response.getBody());
        assertEquals("ecom_assistant", response.getBody().getDebug().get("resolvedRole"));

        ArgumentCaptor<RoleRequest> roleRequestCaptor = ArgumentCaptor.forClass(RoleRequest.class);
        verify(roleAgent).handle(roleRequestCaptor.capture());
        assertEquals("ecom_assistant", roleRequestCaptor.getValue().role());
    }

    @Test
    void shouldUseEcomAssistantAsDefaultRole() {
        when(roleAgentRegistry.find("ecom_assistant")).thenReturn(Optional.of(roleAgent));

        RoleAgentController.RoleAgentRequest request = new RoleAgentController.RoleAgentRequest();
        request.setUserId("u1");
        request.setSessionId("s1");
        request.setInputText("查询美国订单日报");
        request.setContext(Map.of());
        request.setPayload(Map.of());

        ResponseEntity<RoleAgentController.RoleAgentResponse> response = controller.send(request);

        verify(roleAgentRegistry).find(eq("ecom_assistant"));
        assertNotNull(response.getBody());
        assertEquals("ecom_assistant", response.getBody().getDebug().get("resolvedRole"));
    }
}
