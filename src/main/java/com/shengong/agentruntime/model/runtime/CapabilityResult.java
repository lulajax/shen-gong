package com.shengong.agentruntime.model.runtime;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 能力执行结果模型。
 */
@Data
public class CapabilityResult {

    private boolean success;
    private String summary;
    private Map<String, Object> data = new HashMap<>();
    private List<Map<String, Object>> errors = new ArrayList<>();

    public static CapabilityResult success(String summary, Map<String, Object> data) {
        CapabilityResult result = new CapabilityResult();
        result.setSuccess(true);
        result.setSummary(summary);
        result.setData(data != null ? data : new HashMap<>());
        return result;
    }

    public static CapabilityResult failure(String code, String message) {
        CapabilityResult result = new CapabilityResult();
        result.setSuccess(false);
        result.setSummary(message);
        result.getErrors().add(Map.of(
                "code", code,
                "message", message
        ));
        return result;
    }

    public CapabilityResult addError(String code, String message, Map<String, Object> details) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        if (details != null && !details.isEmpty()) {
            error.put("details", details);
        }
        this.errors.add(error);
        this.success = false;
        return this;
    }
}
