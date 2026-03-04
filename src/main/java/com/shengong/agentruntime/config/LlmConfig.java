package com.shengong.agentruntime.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LLM 模型配置 - 支持通过配置切换模型提供商
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class LlmConfig {

    @Value("${langchain4j.proxy.enabled:false}")
    private Boolean proxyEnabled;

    @Value("${langchain4j.proxy.host:}")
    private String proxyHost;

    @Value("${langchain4j.proxy.port:0}")
    private Integer proxyPort;

    @Value("${langchain4j.proxy.type:HTTP}")
    private String proxyType;

    /**
     * OpenAI 模型配置
     */
    @Bean
    @ConditionalOnProperty(name = "langchain4j.model.provider", havingValue = "openai")
    public ChatModel openAiChatModel(
            @Value("${langchain4j.open-ai.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.model-name:gpt-4o}") String modelName,
            @Value("${langchain4j.open-ai.temperature:0.7}") Double temperature,
            @Value("${langchain4j.open-ai.max-tokens:2000}") Integer maxTokens,
            @Value("${langchain4j.open-ai.timeout:120s}") Duration timeout
    ) {
        configureSystemProxy();

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(timeout)
                .logRequests(true)
                .logResponses(true)
                .build();

        log.info("OpenAI Chat Model initialized: model={}, temperature={}, maxTokens={}, proxyEnabled={}",
                modelName, temperature, maxTokens, proxyEnabled);

        return model;
    }

    /**
     * Google Gemini 模型配置
     */
    @Bean
    @ConditionalOnProperty(name = "langchain4j.model.provider", havingValue = "gemini", matchIfMissing = true)
    public ChatModel geminiChatModel(
            @Value("${langchain4j.google-ai-gemini.api-key}") String apiKey,
            @Value("${langchain4j.google-ai-gemini.model-name:gemini-2.5-flash}") String modelName,
            @Value("${langchain4j.google-ai-gemini.temperature:0.7}") Double temperature,
            @Value("${langchain4j.google-ai-gemini.max-tokens:8192}") Integer maxTokens,
            @Value("${langchain4j.google-ai-gemini.timeout:120s}") Duration timeout
    ) {
        configureSystemProxy();

        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxOutputTokens(maxTokens)
                .timeout(timeout)
                .logRequestsAndResponses(true)
                .build();

        log.info("Google Gemini Chat Model initialized: model={}, temperature={}, maxTokens={}, proxyEnabled={}",
                modelName, temperature, maxTokens, proxyEnabled);

        return model;
    }

    /**
     * 配置系统代理
     */
    private void configureSystemProxy() {
        if (proxyEnabled && proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0) {
            if ("SOCKS".equalsIgnoreCase(proxyType)) {
                System.setProperty("socksProxyHost", proxyHost);
                System.setProperty("socksProxyPort", String.valueOf(proxyPort));
                log.info("LLM SOCKS proxy configured: host={}, port={}", proxyHost, proxyPort);
            } else {
                System.setProperty("http.proxyHost", proxyHost);
                System.setProperty("http.proxyPort", String.valueOf(proxyPort));
                System.setProperty("https.proxyHost", proxyHost);
                System.setProperty("https.proxyPort", String.valueOf(proxyPort));
                log.info("LLM HTTP/HTTPS proxy configured: host={}, port={}", proxyHost, proxyPort);
            }
            System.setProperty("http.nonProxyHosts", "localhost|127.0.0.1|*.local");
        }
    }
}
