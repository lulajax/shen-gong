package com.shengong.agentruntime.controller;

import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import com.shengong.agentruntime.service.IntelligentAgentRouter;
import com.shengong.agentruntime.service.RouterAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 对话式 AI 控制器 - 支持多模态
 * 支持自然语言交互,智能路由到合适的 Agent
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat API", description = "对话式 AI 接口 - 支持文本、图片等多模态输入")
public class ChatController {

    private final IntelligentAgentRouter intelligentRouter;
    private final RouterAgentService routerAgentService;
    private final com.shengong.agentruntime.service.AgentRegistry agentRegistry;

    /**
     * 智能对话接口 - 自动路由到合适的 Agent
     * 统一使用多轮对话模式，支持多模态（图片）输入
     */
    @PostMapping("/send")
    @Operation(summary = "发送消息", description = "发送消息,自动选择合适的 Agent 处理。支持多轮对话(messages)和多模态图片输入(images)。")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        // 移除 message 字段兼容性处理，仅使用 messages
        List<IntelligentAgentRouter.ConversationMessage> messages = request.getMessages();
        if (messages == null) {
            messages = new ArrayList<>();
        }

        log.info("收到对话请求: userId={}, messages={}",
                request.getUserId(), messages.size());

        try {
            // 统一流程：无论是否有图片，都交由 IntelligentAgentRouter 处理
            
            // 智能路由 (传入对话历史和图片)
            // IntelligentAgentRouter 会负责将图片放入 AgentTask，并可能根据图片内容辅助路由
            AgentTask task = intelligentRouter.routeFromConversation(messages);

            // 设置公共信息
            task.setUserId(request.getUserId());
            if (request.getSessionId() != null) {
                task.putContext("sessionId", request.getSessionId());
            }

            // 处理任务
            return processAgentTask(task);

        } catch (Exception e) {
            log.error("对话处理失败: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage());
        }
    }


    /**
     * 处理 Agent 任务并构建响应
     */
    private ResponseEntity<ChatResponse> processAgentTask(AgentTask task) {
        // 检查参数收集是否失败
        Boolean paramValidationFailed = task.getContextValue("paramCollectionFailed");
        if (Boolean.TRUE.equals(paramValidationFailed)) {
            List<String> missingParams = task.getContextValue("missingParams");

            // 生成友好提示
            String userPrompt = "";

            // 优先使用 ParamExtractionService 生成的提示
            String missingPrompt = task.getContextValue("missingPrompt");
            if (missingPrompt != null && !missingPrompt.isEmpty()) {
                userPrompt = missingPrompt;
            } else {
                userPrompt = "缺少必填参数: " + String.join(", ", missingParams);
            }

            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setMessage(userPrompt);
            errorResponse.setSuccess(false);
            errorResponse.setData(Map.of(
                    "missingParams", missingParams,
                    "paramCollectionFailed", true,
                    "needMoreInfo", true,
                    "taskContext", Map.of(
                            "taskType", task.getTaskType(),
                            "domain", task.getDomain(),
                            "partialPayload", task.getPayload()
                    )
            ));

            return ResponseEntity.ok(errorResponse);
        }

        // 执行 Agent
        AgentResult result = routerAgentService.route(task);

        // 构建响应
        ChatResponse response = new ChatResponse();
        response.setMessage(result.getSummary());
        response.setSuccess(result.isSuccess());
        response.setData(result.getData());
        response.setDebug(result.getDebug());

        // 添加路由信息
        Map<String, Object> routingInfo = task.getContextValue("routingInfo");
        if (routingInfo != null) {
            response.setRoutingInfo(routingInfo);
        }

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<ChatResponse> buildErrorResponse(String errorMessage) {
        ChatResponse errorResponse = new ChatResponse();
        errorResponse.setMessage("抱歉,处理您的请求时出现了问题: " + errorMessage);
        errorResponse.setSuccess(false);
        return ResponseEntity.ok(errorResponse);
    }

    // --- DTOs ---

    @Data
    public static class ChatRequest {
        private String userId;
        private String sessionId;
        private Map<String, Object> context;
        // 支持多轮对话
        private List<IntelligentAgentRouter.ConversationMessage> messages;
    }


    @Data
    public static class PreviewRequest {
        private List<IntelligentAgentRouter.ConversationMessage> messages;
    }

    @Data
    public static class ChatResponse {
        private String message;
        private boolean success;
        private Map<String, Object> data;
        private Map<String, Object> routingInfo;
        private Map<String, Object> debug;
    }

    @Data
    public static class RoutePreview {
        private String selectedAgent;
        private String taskType;
        private String domain;
        private double confidence;
        private String reason;
        private Map<String, Object> payload;
    }
}
