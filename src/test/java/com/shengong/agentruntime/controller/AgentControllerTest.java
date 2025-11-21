package com.shengong.agentruntime.controller;

import com.shengong.agentruntime.model.AgentTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Agent Controller 集成测试
 *
 * @author 神工团队
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/v1/agent/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(" is running"));
    }

    @Test
    void testHandleGenericAnalysisTask() throws Exception {
        AgentTask task = new AgentTask();
        task.setTaskType("analysis");
        task.setDomain("generic");
        task.putPayload("text", "This is a test analysis request");
        task.setUserId("test_user");

        String taskJson = objectMapper.writeValueAsString(task);

        mockMvc.perform(post("/api/v1/agent/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.summary").exists());
    }

    @Test
    void testHandleTaskWithMissingParameter() throws Exception {
        AgentTask task = new AgentTask();
        task.setTaskType("analysis");
        task.setDomain("generic");
        // Missing required "text" parameter

        String taskJson = objectMapper.writeValueAsString(task);

        mockMvc.perform(post("/api/v1/agent/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void testHandleUnknownTaskType() throws Exception {
        AgentTask task = new AgentTask();
        task.setTaskType("unknown_task");
        task.setDomain("unknown_domain");

        String taskJson = objectMapper.writeValueAsString(task);

        mockMvc.perform(post("/api/v1/agent/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.summary").value(org.hamcrest.Matchers.containsString("No suitable agent found")));
    }
}
