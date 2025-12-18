package com.shengong.agentruntime.collaboration;

import com.shengong.agentruntime.collaboration.prompt.ResultAggregationPromptBuilder;
import com.shengong.agentruntime.llm.LlmClient;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.collaboration.MultiAgentResult;
import com.shengong.agentruntime.model.collaboration.SubTaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 结果聚合器
 * 整合多个Agent的执行结果
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResultAggregator {

    private final LlmClient llmClient;
    private final ResultAggregationPromptBuilder promptBuilder;

    /**
     * 聚合所有子任务结果
     *
     * @param results       子任务结果列表
     * @param originalInput 用户原始输入
     * @return 聚合后的结果
     */
    public MultiAgentResult aggregate(List<SubTaskResult> results, String originalInput) {
        if (results == null || results.isEmpty()) {
            return MultiAgentResult.error("没有子任务结果可聚合", new ArrayList<>());
        }

        log.info("开始聚合结果,子任务数: {}", results.size());

        // 统计成功和失败数
        long successCount = results.stream().filter(SubTaskResult::isSuccess).count();
        long failureCount = results.stream().filter(SubTaskResult::isFailure).count();

        log.info("子任务统计 - 成功: {}, 失败: {}", successCount, failureCount);

        // 如果全部失败
        if (successCount == 0) {
            return handleAllFailure(results, originalInput);
        }

        // 如果部分失败
        if (failureCount > 0) {
            return handlePartialFailure(results, originalInput);
        }

        // 全部成功
        return handleAllSuccess(results, originalInput);
    }

    /**
     * 处理全部成功的情况
     *
     * @param results       子任务结果列表
     * @param originalInput 用户原始输入
     * @return 聚合结果
     */
    private MultiAgentResult handleAllSuccess(List<SubTaskResult> results, String originalInput) {
        try {
            // 使用LLM生成综合总结
            String summary = generateSummaryWithLlm(results, originalInput);

            // 收集所有结果数据
            Map<String, Object> aggregatedData = collectAllData(results);

            // 生成执行流程说明
            String executionFlow = generateExecutionFlow(results);

            MultiAgentResult result = MultiAgentResult.ok(summary, aggregatedData, results);
            result.setExecutionFlow(executionFlow);

            log.info("聚合成功,生成综合总结");
            return result;

        } catch (Exception e) {
            log.error("LLM聚合失败,使用降级方案: {}", e.getMessage());
            return handleAllSuccessFallback(results, originalInput);
        }
    }

    /**
     * 处理部分失败的情况
     *
     * @param results       子任务结果列表
     * @param originalInput 用户原始输入
     * @return 聚合结果
     */
    private MultiAgentResult handlePartialFailure(List<SubTaskResult> results, String originalInput) {
        try {
            // 使用LLM生成综合总结(说明部分失败)
            String summary = generateSummaryWithLlm(results, originalInput);

            // 收集成功任务的数据
            Map<String, Object> aggregatedData = collectSuccessData(results);

            // 收集错误信息
            List<String> errors = results.stream()
                    .filter(SubTaskResult::isFailure)
                    .map(r -> String.format("%s: %s", r.getAgentName(), r.getResult().getSummary()))
                    .collect(Collectors.toList());

            // 生成执行流程说明
            String executionFlow = generateExecutionFlow(results);

            MultiAgentResult result = MultiAgentResult.partial(summary, aggregatedData, results, errors);
            result.setExecutionFlow(executionFlow);

            log.info("聚合完成(部分失败),成功任务: {}, 失败任务: {}",
                    result.getSuccessCount(), result.getFailureCount());
            return result;

        } catch (Exception e) {
            log.error("LLM聚合失败,使用降级方案: {}", e.getMessage());
            return handlePartialFailureFallback(results, originalInput);
        }
    }

    /**
     * 处理全部失败的情况
     *
     * @param results       子任务结果列表
     * @param originalInput 用户原始输入
     * @return 聚合结果
     */
    private MultiAgentResult handleAllFailure(List<SubTaskResult> results, String originalInput) {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("所有子任务都失败了:\n");

        for (SubTaskResult result : results) {
            errorMessage.append(String.format("- %s (%s): %s\n",
                    result.getAgentName(),
                    result.getTaskId(),
                    result.getResult().getSummary()));
        }

        MultiAgentResult multiResult = MultiAgentResult.error(errorMessage.toString(), results);
        multiResult.setExecutionFlow(generateExecutionFlow(results));

        log.warn("所有子任务都失败");
        return multiResult;
    }

    /**
     * 使用LLM生成综合总结
     *
     * @param results       子任务结果列表
     * @param originalInput 用户原始输入
     * @return 综合总结
     */
    private String generateSummaryWithLlm(List<SubTaskResult> results, String originalInput) {
        try {
            // 构建Prompt
            String prompt = promptBuilder.buildAggregationPrompt(results, originalInput);

            // 调用LLM生成总结
            String summary = llmClient.chat(prompt, "请生成综合总结");

            log.debug("LLM生成的总结: {}", summary.substring(0, Math.min(summary.length(), 100)));
            return summary;

        } catch (Exception e) {
            log.error("LLM生成总结失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 收集所有成功任务的数据
     *
     * @param results 子任务结果列表
     * @return 聚合的数据Map
     */
    private Map<String, Object> collectAllData(List<SubTaskResult> results) {
        Map<String, Object> aggregatedData = new HashMap<>();

        for (SubTaskResult result : results) {
            if (result.isSuccess() && result.getResult().getData() != null) {
                // 使用taskId作为key,避免冲突
                aggregatedData.put(result.getTaskId(), result.getResult().getData());
            }
        }

        return aggregatedData;
    }

    /**
     * 收集成功任务的数据
     *
     * @param results 子任务结果列表
     * @return 聚合的数据Map
     */
    private Map<String, Object> collectSuccessData(List<SubTaskResult> results) {
        return collectAllData(results); // 实现相同
    }

    /**
     * 生成执行流程说明
     *
     * @param results 子任务结果列表
     * @return 执行流程字符串
     */
    private String generateExecutionFlow(List<SubTaskResult> results) {
        return results.stream()
                .map(r -> {
                    String status = r.isSuccess() ? "✓" : "✗";
                    return String.format("%s %s (%s)",
                            status, r.getAgentName(), r.getDescription());
                })
                .collect(Collectors.joining(" → "));
    }

    /**
     * 全部成功的降级处理
     */
    private MultiAgentResult handleAllSuccessFallback(List<SubTaskResult> results, String originalInput) {
        StringBuilder summary = new StringBuilder();
        summary.append("多Agent任务执行完成,以下是各Agent的执行结果:\n\n");

        for (SubTaskResult result : results) {
            summary.append(String.format("**%s**: %s\n",
                    result.getAgentName(),
                    result.getResult().getSummary()));
        }

        Map<String, Object> aggregatedData = collectAllData(results);
        String executionFlow = generateExecutionFlow(results);

        MultiAgentResult multiResult = MultiAgentResult.ok(summary.toString(), aggregatedData, results);
        multiResult.setExecutionFlow(executionFlow);

        return multiResult;
    }

    /**
     * 部分失败的降级处理
     */
    private MultiAgentResult handlePartialFailureFallback(List<SubTaskResult> results, String originalInput) {
        StringBuilder summary = new StringBuilder();
        summary.append("多Agent任务部分完成,以下是执行结果:\n\n");

        summary.append("**成功的任务:**\n");
        for (SubTaskResult result : results) {
            if (result.isSuccess()) {
                summary.append(String.format("- %s: %s\n",
                        result.getAgentName(),
                        result.getResult().getSummary()));
            }
        }

        summary.append("\n**失败的任务:**\n");
        List<String> errors = new ArrayList<>();
        for (SubTaskResult result : results) {
            if (result.isFailure()) {
                String error = String.format("%s: %s", result.getAgentName(), result.getResult().getSummary());
                summary.append(String.format("- %s\n", error));
                errors.add(error);
            }
        }

        Map<String, Object> aggregatedData = collectSuccessData(results);
        String executionFlow = generateExecutionFlow(results);

        MultiAgentResult multiResult = MultiAgentResult.partial(summary.toString(), aggregatedData, results, errors);
        multiResult.setExecutionFlow(executionFlow);

        return multiResult;
    }
}
