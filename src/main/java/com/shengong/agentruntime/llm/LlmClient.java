package com.shengong.agentruntime.llm;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM 客户端封装 - 支持多模态和代理配置
 * <p>
 * 模型提供商通过 langchain4j.model.provider 配置项切换
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
public class LlmClient {

    private final ChatLanguageModel model;
    private final String modelProvider;

    public LlmClient(
            ChatLanguageModel model,
            @Value("${langchain4j.model.provider:open-ai}") String modelProvider
    ) {
        this.model = model;
        this.modelProvider = modelProvider;

        log.info("LLM Client initialized with provider: {}", modelProvider);
    }

    /**
     * 简单对话 (纯文本)
     */
    public String chat(String systemPrompt, String userMessage) {
        try {
            List<ChatMessage> messages = new ArrayList<>();

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(SystemMessage.from(systemPrompt));
            }

            messages.add(UserMessage.from(userMessage));

            dev.langchain4j.model.output.Response<AiMessage> response = model.generate(messages);
            String result = response.content().text();

            log.debug("LLM response received: length={}", result.length());
            return result;

        } catch (Exception e) {
            log.error("LLM chat failed: {}", e.getMessage(), e);
            throw new RuntimeException("LLM chat failed: " + e.getMessage(), e);
        }
    }

    /**
     * 仅用户消息
     */
    public String chat(String message) {
        return chat("", message);
    }

    /**
     * 多模态对话 - 支持文本、图片、文件等
     *
     * @param messages 消息列表 (可包含 UserMessage, SystemMessage, AiMessage)
     * @return AI 响应文本
     */
    public String chatMultimodal(List<ChatMessage> messages) {
        try {
            dev.langchain4j.model.output.Response<AiMessage> response = model.generate(messages);
            String result = response.content().text();

            log.debug("LLM multimodal response received: length={}", result.length());
            return result;

        } catch (Exception e) {
            log.error("LLM multimodal chat failed: {}", e.getMessage(), e);
            throw new RuntimeException("LLM multimodal chat failed: " + e.getMessage(), e);
        }
    }

    /**
     * 对话 - 支持历史消息
     *
     * @param conversationHistory 对话历史
     * @param newUserMessage 新的用户消息
     * @return AI 响应文本
     */
    public String chatWithHistory(List<ChatMessage> conversationHistory, String newUserMessage) {
        try {
            List<ChatMessage> allMessages = new ArrayList<>(conversationHistory);
            allMessages.add(UserMessage.from(newUserMessage));

            return chatMultimodal(allMessages);

        } catch (Exception e) {
            log.error("LLM chat with history failed: {}", e.getMessage(), e);
            throw new RuntimeException("LLM chat with history failed: " + e.getMessage(), e);
        }
    }

    /**
     * 获取模型提供商
     */
    public String getModelName() {
        return modelProvider;
    }
}
