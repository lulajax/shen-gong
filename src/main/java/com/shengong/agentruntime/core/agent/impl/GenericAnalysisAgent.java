package com.shengong.agentruntime.core.agent.impl;

import com.shengong.agentruntime.controller.ChatController;
import com.shengong.agentruntime.core.agent.AbstractAgent;
import com.shengong.agentruntime.core.agent.annotation.AgentDefinition;
import com.shengong.agentruntime.core.param.AgentParam;
import com.shengong.agentruntime.llm.LlmClient;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通用分析 Agent
 * 支持通用领域的分析任务，包括文本和多模态内容
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
@AgentDefinition(
    name = "GenericAnalysisAgent",
    domains = {"generic"},
    taskType = "generic_analysis",
    description = "Generic analysis agent powered by LLM (supports multimodal)"
)
public class GenericAnalysisAgent extends AbstractAgent<GenericAnalysisAgent.GenericAnalysisParams> {

    private final LlmClient llmClient;

    public GenericAnalysisAgent(LlmClient llmClient) {
        super(GenericAnalysisParams.class);
        this.llmClient = llmClient;
    }

    @Data
    public static class ImageData {
        private String base64Data;
        private String mimeType;
    }

    @Data
    public static class GenericAnalysisParams {
        @AgentParam(required = true, description = "用户输入的文本内容，需要进行分析")
        private String text;

        @AgentParam(required = false, description = "用户上传的图片列表")
        private List<ImageData> images;
    }

    @Override
    protected AgentResult execute(AgentTask task, GenericAnalysisParams params) {
        log.info("GenericAnalysisAgent handling task: {}", task.getTaskId());

        try {
            String userText = params.getText();
            List<ImageData> images = params.getImages();

            String response;

            // 判断是否有多模态内容
            if (images != null && !images.isEmpty()) {
                log.info("处理多模态分析任务: {} 张图片", images.size());

                List<Content> contents = new ArrayList<>();
                if (userText != null && !userText.isEmpty()) {
                    contents.add(TextContent.from(userText));
                } else {
                    contents.add(TextContent.from("请分析这些图片"));
                }

                for (ImageData img : images) {
                    contents.add(ImageContent.from(
                            img.getBase64Data(),
                            img.getMimeType() != null ? img.getMimeType() : "image/jpeg"
                    ));
                }

                List<ChatMessage> messages = new ArrayList<>();
                messages.add(UserMessage.from(contents));

                response = llmClient.chatMultimodal(messages);

            } else {
                // 纯文本分析
                String systemPrompt = "你是一个专业分析助手，请根据用户输入总结重点，提取关键信息。";
                response = llmClient.chat(systemPrompt, userText);
            }

            // 构造结果
            return AgentResult.ok(response, Map.of(
                    "raw_input", userText != null ? userText : "",
                    "image_count", images != null ? images.size() : 0,
                    "analysis", response
            ));

        } catch (Exception e) {
            log.error("GenericAnalysisAgent failed: {}", e.getMessage(), e);
            return AgentResult.error("Analysis failed: " + e.getMessage());
        }
    }
}
