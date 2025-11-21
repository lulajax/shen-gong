package com.shengong.agentruntime.llm;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * LLM 客户端封装 - 支持 Google Gemini 多模态和代理配置
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
public class LlmClient {

    private final ChatLanguageModel model;
    private final String modelName;

    public LlmClient(
            @Value("${langchain4j.google-ai-gemini.api-key}") String apiKey,
            @Value("${langchain4j.google-ai-gemini.model-name:gemini-2.5-flash}") String modelName,
            @Value("${langchain4j.google-ai-gemini.temperature:0.7}") Double temperature,
            @Value("${langchain4j.google-ai-gemini.max-tokens:8192}") Integer maxTokens,
            @Value("${langchain4j.google-ai-gemini.timeout:60s}") Duration timeout,
            @Value("${langchain4j.proxy.enabled:false}") Boolean proxyEnabled,
            @Value("${langchain4j.proxy.host:}") String proxyHost,
            @Value("${langchain4j.proxy.port:0}") Integer proxyPort,
            @Value("${langchain4j.proxy.type:HTTP}") String proxyType
    ) {
        this.modelName = modelName;

        // 设置系统代理（如果启用）
        if (proxyEnabled && proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0) {
            configureSystemProxy(proxyHost, proxyPort, proxyType);
        }

        // 构建 Gemini 模型
        this.model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxOutputTokens(maxTokens)
                .timeout(timeout)
                .logRequestsAndResponses(true)
                .build();

        log.info("LLM Client initialized: model={}, temperature={}, maxTokens={}, proxyEnabled={}",
                modelName, temperature, maxTokens, proxyEnabled);
    }

    /**
     * 配置系统代理
     */
    private void configureSystemProxy(String host, Integer port, String type) {
        if ("SOCKS".equalsIgnoreCase(type)) {
            // SOCKS 代理
            System.setProperty("socksProxyHost", host);
            System.setProperty("socksProxyPort", String.valueOf(port));
            log.info("LLM Client SOCKS proxy configured: host={}, port={}", host, port);
        } else {
            // HTTP/HTTPS 代理
            System.setProperty("http.proxyHost", host);
            System.setProperty("http.proxyPort", String.valueOf(port));
            System.setProperty("https.proxyHost", host);
            System.setProperty("https.proxyPort", String.valueOf(port));
            log.info("LLM Client HTTP/HTTPS proxy configured: host={}, port={}", host, port);
        }

        // 设置不需要代理的主机（可选）
        System.setProperty("http.nonProxyHosts", "localhost|127.0.0.1|*.local");
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
     * 获取模型名称
     */
    public String getModelName() {
        return modelName;
    }
}
