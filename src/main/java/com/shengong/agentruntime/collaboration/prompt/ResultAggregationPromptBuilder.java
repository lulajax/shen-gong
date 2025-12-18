package com.shengong.agentruntime.collaboration.prompt;

import com.shengong.agentruntime.model.collaboration.SubTaskResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 结果聚合Prompt构建器
 * 生成用于LLM进行结果聚合的System Prompt
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
public class ResultAggregationPromptBuilder {

    /**
     * 构建结果聚合Prompt
     *
     * @param results       子任务结果列表
     * @param originalInput 用户原始输入
     * @return Prompt文本
     */
    public String buildAggregationPrompt(List<SubTaskResult> results, String originalInput) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个结果聚合专家。请整合以下多个Agent的执行结果,生成一个连贯、完整的回答。\n\n");

        // 用户原始需求
        prompt.append("## 用户原始需求\n");
        prompt.append(String.format("\"%s\"\n\n", originalInput));

        // 各Agent执行结果
        prompt.append("## 各Agent执行结果\n");
        int successCount = 0;
        int failureCount = 0;

        for (int i = 0; i < results.size(); i++) {
            SubTaskResult result = results.get(i);
            prompt.append(String.format("### %d. %s (%s)\n",
                    i + 1, result.getAgentName(), result.getTaskId()));

            if (result.isSuccess()) {
                successCount++;
                prompt.append(String.format("**状态**: 成功 ✓\n"));
                prompt.append(String.format("**描述**: %s\n", result.getDescription()));
                prompt.append(String.format("**结果摘要**: %s\n", result.getResult().getSummary()));

                // 如果有数据,展示数据
                if (result.getResult().getData() != null && !result.getResult().getData().isEmpty()) {
                    prompt.append(String.format("**数据**: %s\n", formatData(result.getResult().getData())));
                }
            } else {
                failureCount++;
                prompt.append(String.format("**状态**: 失败 ✗\n"));
                prompt.append(String.format("**描述**: %s\n", result.getDescription()));
                prompt.append(String.format("**错误**: %s\n", result.getResult().getSummary()));
            }

            if (result.getLatencyMs() != null) {
                prompt.append(String.format("**耗时**: %dms\n", result.getLatencyMs()));
            }

            prompt.append("\n");
        }

        // 统计信息
        prompt.append(String.format("## 执行统计\n"));
        prompt.append(String.format("- 总任务数: %d\n", results.size()));
        prompt.append(String.format("- 成功: %d\n", successCount));
        prompt.append(String.format("- 失败: %d\n", failureCount));
        prompt.append("\n");

        // 聚合要求
        prompt.append("## 聚合要求\n");
        prompt.append("请生成一个完整、连贯的回答,要求:\n");
        prompt.append("1. **回答用户问题**: 直接回答用户的原始需求\n");
        prompt.append("2. **整合所有结果**: 将各Agent的输出整合为一个完整的答案\n");
        prompt.append("3. **突出关键信息**: 提取最重要的数据和见解\n");
        prompt.append("4. **保持连贯性**: 确保回答逻辑清晰、前后呼应\n");
        prompt.append("5. **处理失败情况**: 如果有Agent失败,说明哪些部分可能不完整\n");
        prompt.append("6. **格式友好**: 使用Markdown格式,如表格、列表等\n\n");

        if (failureCount > 0) {
            prompt.append("**注意**: 有部分Agent执行失败,请在回答中说明可能存在的不完整信息。\n\n");
        }

        prompt.append("请直接输出最终回答,不要包含任何额外的说明或格式标记。\n");

        return prompt.toString();
    }

    /**
     * 格式化数据为字符串
     *
     * @param data 数据Map
     * @return 格式化后的字符串
     */
    private String formatData(Object data) {
        if (data == null) {
            return "无";
        }

        // 简单格式化,避免过长
        String dataStr = data.toString();
        if (dataStr.length() > 500) {
            return dataStr.substring(0, 500) + "...";
        }
        return dataStr;
    }
}
