package com.shengong.agentruntime.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 参数验证结果
 *
 * @author 神工团队
 * @since 1.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParamValidationResult {

    /**
     * 验证是否通过
     */
    private boolean passed;

    /**
     * 缺失的参数列表
     */
    private List<String> missingParams;

    /**
     * 缺失参数提示（用于直接反馈给用户）
     */
    private String missingPrompt;

    public static ParamValidationResult success() {
        return ParamValidationResult.builder()
                .passed(true)
                .build();
    }

    public static ParamValidationResult fail(List<String> missingParams, String missingPrompt) {
        return ParamValidationResult.builder()
                .passed(false)
                .missingParams(missingParams)
                .missingPrompt(missingPrompt)
                .build();
    }
}

