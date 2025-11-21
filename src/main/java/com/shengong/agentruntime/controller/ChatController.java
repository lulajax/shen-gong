package com.shengong.agentruntime.controller;

import com.shengong.agentruntime.llm.LlmClient;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import com.shengong.agentruntime.service.IntelligentAgentRouter;
import com.shengong.agentruntime.service.RouterAgentService;
import dev.langchain4j.data.message.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Base64;
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
    private final LlmClient llmClient;

    /**
     * 智能对话接口 - 自动路由到合适的 Agent
     */
    @PostMapping("/send")
    @Operation(summary = "发送消息", description = "发送自然语言消息,自动选择合适的 Agent 处理")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        log.info("收到对话请求: userId={}, message={}",
                request.getUserId(), request.getMessage());

        try {
            // 1. 智能路由 - 根据用户输入创建 AgentTask
            AgentTask task = intelligentRouter.routeFromUserInput(
                    request.getMessage(),
                    request.getContext()
            );

            // 设置用户信息
            task.setUserId(request.getUserId());
            if (request.getSessionId() != null) {
                task.putContext("sessionId", request.getSessionId());
            }

            // 2. 执行 Agent
            AgentResult result = routerAgentService.route(task);

            // 3. 构建响应
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

        } catch (Exception e) {
            log.error("对话处理失败: {}", e.getMessage(), e);

            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setMessage("抱歉,处理您的请求时出现了问题: " + e.getMessage());
            errorResponse.setSuccess(false);

            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 多模态对话接口 - 支持文本 + 图片
     */
    @PostMapping(value = "/multimodal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "多模态对话", description = "支持文本、图片等多模态输入")
    public ResponseEntity<ChatResponse> multimodalChat(
            @RequestParam("userId") String userId,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam("message") String message,
            @RequestParam(value = "images", required = false) List<MultipartFile> images
    ) {
        log.info("收到多模态对话请求: userId={}, message={}, images={}",
                userId, message, images != null ? images.size() : 0);

        try {
            // 1. 构建多模态消息
            List<Content> contents = new ArrayList<>();

            // 添加文本内容
            contents.add(TextContent.from(message));

            // 添加图片内容
            if (images != null && !images.isEmpty()) {
                for (MultipartFile imageFile : images) {
                    try {
                        byte[] imageBytes = imageFile.getBytes();
                        String mimeType = imageFile.getContentType();
                        if (mimeType == null) {
                            mimeType = "image/jpeg"; // 默认类型
                        }

                        // 使用 Base64 编码图片
                        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                        contents.add(ImageContent.from(base64Image, mimeType));

                        log.debug("添加图片: name={}, size={}, type={}",
                                imageFile.getOriginalFilename(), imageBytes.length, mimeType);

                    } catch (Exception e) {
                        log.error("处理图片失败: {}", e.getMessage());
                    }
                }
            }

            // 2. 创建用户消息
            UserMessage userMessage = UserMessage.from(contents);

            // 3. 调用 LLM 进行多模态对话
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(userMessage);

            String aiResponse = llmClient.chatMultimodal(messages);

            // 4. 构建响应
            ChatResponse response = new ChatResponse();
            response.setMessage(aiResponse);
            response.setSuccess(true);
            response.setData(Map.of(
                    "modelName", llmClient.getModelName(),
                    "multimodal", true,
                    "imageCount", images != null ? images.size() : 0
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("多模态对话处理失败: {}", e.getMessage(), e);

            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setMessage("抱歉,处理您的多模态请求时出现了问题: " + e.getMessage());
            errorResponse.setSuccess(false);

            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 多模态对话接口 (JSON 格式) - 支持 Base64 编码的图片
     */
    @PostMapping("/multimodal-json")
    @Operation(summary = "多模态对话 (JSON)", description = "支持文本和 Base64 编码的图片")
    public ResponseEntity<ChatResponse> multimodalChatJson(@RequestBody MultimodalChatRequest request) {
        log.info("收到多模态对话请求 (JSON): userId={}, message={}, images={}",
                request.getUserId(), request.getMessage(),
                request.getImages() != null ? request.getImages().size() : 0);

        try {
            // 1. 构建多模态消息
            List<Content> contents = new ArrayList<>();

            // 添加文本内容
            contents.add(TextContent.from(request.getMessage()));

            // 添加图片内容
            if (request.getImages() != null && !request.getImages().isEmpty()) {
                for (ImageData imageData : request.getImages()) {
                    contents.add(ImageContent.from(
                            imageData.getBase64Data(),
                            imageData.getMimeType() != null ? imageData.getMimeType() : "image/jpeg"
                    ));
                }
            }

            // 2. 创建用户消息
            UserMessage userMessage = UserMessage.from(contents);

            // 3. 调用 LLM 进行多模态对话
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(userMessage);

            String aiResponse = llmClient.chatMultimodal(messages);

            // 4. 构建响应
            ChatResponse response = new ChatResponse();
            response.setMessage(aiResponse);
            response.setSuccess(true);
            response.setData(Map.of(
                    "modelName", llmClient.getModelName(),
                    "multimodal", true,
                    "imageCount", request.getImages() != null ? request.getImages().size() : 0
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("多模态对话处理失败: {}", e.getMessage(), e);

            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setMessage("抱歉,处理您的多模态请求时出现了问题: " + e.getMessage());
            errorResponse.setSuccess(false);

            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 多轮对话接口 - 支持对话历史
     */
    @PostMapping("/conversation")
    @Operation(summary = "多轮对话", description = "支持对话历史的多轮交互")
    public ResponseEntity<ChatResponse> conversation(@RequestBody ConversationRequest request) {
        log.info("收到对话请求: userId={}, messages={}",
                request.getUserId(), request.getMessages().size());

        try {
            // 1. 从对话历史中智能路由
            AgentTask task = intelligentRouter.routeFromConversation(request.getMessages());

            // 设置用户信息
            task.setUserId(request.getUserId());
            task.putContext("sessionId", request.getSessionId());

            // 2. 执行 Agent
            AgentResult result = routerAgentService.route(task);

            // 3. 构建响应
            ChatResponse response = new ChatResponse();
            response.setMessage(result.getSummary());
            response.setSuccess(result.isSuccess());
            response.setData(result.getData());
            response.setDebug(result.getDebug());

            Map<String, Object> routingInfo = task.getContextValue("routingInfo");
            if (routingInfo != null) {
                response.setRoutingInfo(routingInfo);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("对话处理失败: {}", e.getMessage(), e);

            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setMessage("抱歉,处理您的请求时出现了问题: " + e.getMessage());
            errorResponse.setSuccess(false);

            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 预览路由 - 不执行,仅返回路由结果
     */
    @PostMapping("/preview")
    @Operation(summary = "预览路由", description = "查看消息会被路由到哪个 Agent,不实际执行")
    public ResponseEntity<RoutePreview> previewRoute(@RequestBody PreviewRequest request) {
        try {
            AgentTask task = intelligentRouter.routeFromUserInput(
                    request.getMessage(),
                    null
            );

            RoutePreview preview = new RoutePreview();
            preview.setTaskType(task.getTaskType());
            preview.setDomain(task.getDomain());
            preview.setPayload(task.getPayload());

            Map<String, Object> routingInfo = task.getContextValue("routingInfo");
            if (routingInfo != null) {
                preview.setSelectedAgent((String) routingInfo.get("selectedAgent"));
                preview.setConfidence((Double) routingInfo.get("confidence"));
                preview.setReason((String) routingInfo.get("reason"));
            }

            return ResponseEntity.ok(preview);

        } catch (Exception e) {
            log.error("路由预览失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 对话请求
     */
    @Data
    public static class ChatRequest {
        private String userId;
        private String sessionId;
        private String message;
        private Map<String, Object> context;
    }

    /**
     * 多模态对话请求 (JSON)
     */
    @Data
    public static class MultimodalChatRequest {
        private String userId;
        private String sessionId;
        private String message;
        private List<ImageData> images;
    }

    /**
     * 图片数据
     */
    @Data
    public static class ImageData {
        private String base64Data;  // Base64 编码的图片数据
        private String mimeType;    // MIME 类型,如 image/jpeg, image/png
        private String description; // 可选的图片描述
    }

    /**
     * 多轮对话请求
     */
    @Data
    public static class ConversationRequest {
        private String userId;
        private String sessionId;
        private List<IntelligentAgentRouter.ConversationMessage> messages;
    }

    /**
     * 预览请求
     */
    @Data
    public static class PreviewRequest {
        private String message;
    }

    /**
     * 对话响应
     */
    @Data
    public static class ChatResponse {
        private String message;
        private boolean success;
        private Map<String, Object> data;
        private Map<String, Object> routingInfo;
        private Map<String, Object> debug;
    }

    /**
     * 路由预览
     */
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
