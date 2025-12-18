package com.shengong.agentruntime.collaboration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengong.agentruntime.collaboration.prompt.TaskDecompositionPromptBuilder;
import com.shengong.agentruntime.collaboration.util.ExecutionPlanValidator;
import com.shengong.agentruntime.core.agent.Agent;
import com.shengong.agentruntime.llm.LlmClient;
import com.shengong.agentruntime.model.collaboration.ExecutionMode;
import com.shengong.agentruntime.model.collaboration.ExecutionPlan;
import com.shengong.agentruntime.model.collaboration.SubTask;
import com.shengong.agentruntime.service.AgentRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 任务编排器
 * 使用LLM智能分解复杂任务为多个子任务
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskOrchestrator {

    private final AgentRegistry agentRegistry;
    private final LlmClient llmClient;
    private final TaskDecompositionPromptBuilder promptBuilder;
    private final ExecutionPlanValidator planValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 判断是否需要多Agent协同
     *
     * @param userInput 用户输入
     * @param context   上下文信息
     * @return true if needs collaboration
     */
    public boolean needsCollaboration(String userInput, Map<String, Object> context) {
        // 检查是否强制启用多Agent模式
        if (context != null && Boolean.TRUE.equals(context.get("forceMultiAgent"))) {
            log.info("强制启用多Agent模式");
            return true;
        }

        try {
            // 获取所有可用Agent
            List<Agent> availableAgents = new ArrayList<>(agentRegistry.getAllAgents());

            // 构建Prompt
            String prompt = promptBuilder.buildCollaborationCheckPrompt(userInput, availableAgents);

            // 调用LLM分析
            String llmResponse = llmClient.chat(prompt, userInput);

            // 解析响应
            Map<String, Object> result = parseJsonResponse(llmResponse);

            Boolean needsCollaboration = (Boolean) result.get("needsCollaboration");
            String reason = (String) result.get("reason");

            log.info("协同判断结果: needsCollaboration={}, reason={}", needsCollaboration, reason);

            return Boolean.TRUE.equals(needsCollaboration);

        } catch (Exception e) {
            log.warn("判断是否需要协同失败,默认返回false: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 分解任务并生成执行计划
     *
     * @param userInput 用户输入
     * @param context   上下文信息
     * @return 执行计划
     */
    public ExecutionPlan decompose(String userInput, Map<String, Object> context) {
        int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("尝试分解任务,第{}/{}次", attempt, maxAttempts);

                // 获取所有可用Agent
                List<Agent> availableAgents = new ArrayList<>(agentRegistry.getAllAgents());

                // 构建Prompt
                String prompt = promptBuilder.buildDecompositionPrompt(userInput, availableAgents);

                // 调用LLM进行任务分解
                String llmResponse = llmClient.chat(prompt, userInput);

                // 解析响应
                ExecutionPlan plan = parseDecompositionResponse(llmResponse);

                // 验证执行计划
                ExecutionPlanValidator.ValidationResult validationResult = planValidator.validate(plan);

                if (validationResult.isValid()) {
                    log.info("任务分解成功: planId={}, 子任务数={}, 执行模式={}",
                            plan.getPlanId(), plan.getTaskCount(), plan.getMode());
                    return plan;
                } else {
                    log.warn("执行计划验证失败(第{}/{}次): {}", attempt, maxAttempts,
                            validationResult.getErrorMessage());

                    if (attempt == maxAttempts) {
                        // 最后一次尝试仍然失败,返回失败计划
                        plan.setReason("计划验证失败: " + validationResult.getErrorMessage());
                        return plan;
                    }
                }

            } catch (Exception e) {
                log.error("任务分解失败(第{}/{}次): {}", attempt, maxAttempts, e.getMessage(), e);

                if (attempt == maxAttempts) {
                    // 所有尝试都失败,返回降级计划
                    return createFallbackPlan(userInput, e.getMessage());
                }
            }
        }

        // 理论上不会到这里,但为了安全返回降级计划
        return createFallbackPlan(userInput, "未知错误");
    }

    /**
     * 解析LLM的任务分解响应
     *
     * @param llmResponse LLM响应
     * @return 执行计划
     */
    @SuppressWarnings("unchecked")
    private ExecutionPlan parseDecompositionResponse(String llmResponse) throws Exception {
        // 提取JSON
        String jsonContent = extractJsonFromResponse(llmResponse);

        // 解析JSON
        Map<String, Object> responseMap = objectMapper.readValue(jsonContent, Map.class);

        ExecutionPlan plan = new ExecutionPlan();

        // 解析基本信息
        plan.setReason((String) responseMap.get("reason"));

        // 解析执行模式
        String modeStr = (String) responseMap.getOrDefault("executionMode", "sequential");
        try {
            plan.setMode(ExecutionMode.valueOf(modeStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.warn("无效的执行模式: {}, 使用默认值SEQUENTIAL", modeStr);
            plan.setMode(ExecutionMode.SEQUENTIAL);
        }

        // 解析子任务列表
        List<Map<String, Object>> subTasksList = (List<Map<String, Object>>) responseMap.get("subTasks");
        if (subTasksList != null) {
            for (Map<String, Object> taskMap : subTasksList) {
                SubTask subTask = new SubTask();
                subTask.setTaskId((String) taskMap.get("taskId"));
                subTask.setAgentName((String) taskMap.get("agentName"));
                subTask.setTaskType((String) taskMap.get("taskType"));
                subTask.setDomain((String) taskMap.get("domain"));
                subTask.setDescription((String) taskMap.get("description"));

                // 依赖关系
                List<String> dependencies = (List<String>) taskMap.get("dependencies");
                if (dependencies != null) {
                    subTask.setDependencies(dependencies);
                }

                // 数据映射
                Map<String, String> dataMappings = (Map<String, String>) taskMap.get("dataMappings");
                if (dataMappings != null) {
                    subTask.setDataMappings(dataMappings);
                }

                // 静态参数
                Map<String, Object> staticParams = (Map<String, Object>) taskMap.get("staticParams");
                if (staticParams != null) {
                    subTask.setStaticParams(staticParams);
                }

                // 可选任务标识
                Boolean optional = (Boolean) taskMap.get("optional");
                if (optional != null) {
                    subTask.setOptional(optional);
                }

                plan.getSubTasks().add(subTask);
            }
        }

        return plan;
    }

    /**
     * 从LLM响应中提取JSON
     *
     * @param response LLM响应
     * @return JSON字符串
     */
    private String extractJsonFromResponse(String response) {
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        } else if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        } else if (response.trim().startsWith("{")) {
            return response.trim();
        }
        return response;
    }

    /**
     * 解析JSON响应为Map
     *
     * @param response JSON响应
     * @return Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String response) throws Exception {
        String jsonContent = extractJsonFromResponse(response);
        return objectMapper.readValue(jsonContent, Map.class);
    }

    /**
     * 创建降级执行计划
     * 当LLM分解失败时,使用GenericAnalysisAgent作为降级方案
     *
     * @param userInput    用户输入
     * @param errorMessage 错误信息
     * @return 降级执行计划
     */
    private ExecutionPlan createFallbackPlan(String userInput, String errorMessage) {
        log.warn("创建降级执行计划: {}", errorMessage);

        ExecutionPlan plan = new ExecutionPlan();
        plan.setMode(ExecutionMode.SEQUENTIAL);
        plan.setReason("LLM任务分解失败,降级为单Agent处理: " + errorMessage);

        // 创建一个使用GenericAnalysisAgent的子任务
        SubTask fallbackTask = new SubTask();
        fallbackTask.setTaskId("task-fallback");
        fallbackTask.setAgentName("GenericAnalysisAgent");
        fallbackTask.setTaskType("generic_analysis");
        fallbackTask.setDomain("generic");
        fallbackTask.setDescription("通用分析(降级方案)");
        fallbackTask.getStaticParams().put("text", userInput);

        plan.getSubTasks().add(fallbackTask);

        return plan;
    }
}
