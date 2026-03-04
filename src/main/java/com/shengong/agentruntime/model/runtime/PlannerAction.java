package com.shengong.agentruntime.model.runtime;

import java.util.Map;

/**
 * 编排器动作模型，描述下一步应调用能力、工具或直接结束。
 */
public record PlannerAction(
        String action,
        String capability,
        String tool,
        Map<String, Object> args,
        String finalAnswer,
        String reason
) {

    public static final String CALL_CAPABILITY = "CALL_CAPABILITY";
    public static final String CALL_TOOL = "CALL_TOOL";
    public static final String FINAL_ANSWER = "FINAL_ANSWER";
}
