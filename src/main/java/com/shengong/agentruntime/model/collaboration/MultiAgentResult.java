package com.shengong.agentruntime.model.collaboration;

import com.shengong.agentruntime.model.AgentResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多Agent执行结果
 * 继承自AgentResult,增加多Agent协同相关的信息
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MultiAgentResult extends AgentResult {
    /**
     * 子任务结果列表
     */
    private List<SubTaskResult> subTaskResults = new ArrayList<>();

    /**
     * 执行计划
     */
    private ExecutionPlan executionPlan;

    /**
     * 总执行时长(毫秒)
     */
    private Long totalLatencyMs;

    /**
     * 成功的子任务数
     */
    private int successCount;

    /**
     * 失败的子任务数
     */
    private int failureCount;

    /**
     * 执行流程说明
     * 例如: "task-1 (获取数据) → task-2 (计算指标) → task-3 (生成报告)"
     */
    private String executionFlow;

    /**
     * 创建成功的多Agent结果
     */
    public static MultiAgentResult ok(String summary, Map<String, Object> data,
                                     List<SubTaskResult> subTaskResults) {
        MultiAgentResult result = new MultiAgentResult();
        result.setStatus("ok");
        result.setSummary(summary);
        result.setData(data != null ? data : new HashMap<>());
        result.setSubTaskResults(subTaskResults);

        // 统计成功和失败数
        int success = 0, failure = 0;
        for (SubTaskResult subResult : subTaskResults) {
            if (subResult.isSuccess()) {
                success++;
            } else {
                failure++;
            }
        }
        result.setSuccessCount(success);
        result.setFailureCount(failure);

        return result;
    }

    /**
     * 创建部分成功的多Agent结果
     */
    public static MultiAgentResult partial(String summary, Map<String, Object> data,
                                          List<SubTaskResult> subTaskResults,
                                          List<String> errors) {
        MultiAgentResult result = new MultiAgentResult();
        result.setStatus("partial");
        result.setSummary(summary);
        result.setData(data != null ? data : new HashMap<>());
        result.setSubTaskResults(subTaskResults);
        result.setErrors(errors != null ? errors : new ArrayList<>());

        // 统计成功和失败数
        int success = 0, failure = 0;
        for (SubTaskResult subResult : subTaskResults) {
            if (subResult.isSuccess()) {
                success++;
            } else {
                failure++;
            }
        }
        result.setSuccessCount(success);
        result.setFailureCount(failure);

        return result;
    }

    /**
     * 创建错误的多Agent结果
     */
    public static MultiAgentResult error(String errorMessage, List<SubTaskResult> subTaskResults) {
        MultiAgentResult result = new MultiAgentResult();
        result.setStatus("error");
        result.setSummary(errorMessage);
        result.getErrors().add(errorMessage);
        result.setSubTaskResults(subTaskResults != null ? subTaskResults : new ArrayList<>());

        // 统计成功和失败数
        if (subTaskResults != null) {
            int success = 0, failure = 0;
            for (SubTaskResult subResult : subTaskResults) {
                if (subResult.isSuccess()) {
                    success++;
                } else {
                    failure++;
                }
            }
            result.setSuccessCount(success);
            result.setFailureCount(failure);
        }

        return result;
    }

    /**
     * 获取任务总数
     */
    public int getTotalCount() {
        return successCount + failureCount;
    }
}
