package com.shengong.agentruntime.core.agent;

import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import java.util.List;
import java.util.Map;

/**
 * Agent 接口
 * 所有 Agent 必须实现此接口
 *
 * @author 神工团队
 * @since 1.0.0
 */
public interface Agent {

    /**
     * Agent 名称 (唯一标识)
     *
     * @return Agent 名称
     */
    String name();

    /**
     * Agent 支持的业务域列表
     *
     * @return 业务域列表 (如: generic, live, order, data等)
     */
    List<String> domains();

    /**
     * Agent 支持的任务类型
     *
     * @return 任务类型 (如: analysis, analysis_report, anomaly_detection 等)
     */
    String taskType();

    /**
     * 判断是否支持该任务
     *
     * @param taskType 任务类型
     * @param domain   业务域
     * @return true 支持, false 不支持
     */
    boolean supports(String taskType, String domain);

    /**
     * 处理任务
     *
     * @param task 任务对象
     * @return 执行结果
     */
    AgentResult handle(AgentTask task);

    /**
     * Agent 描述
     *
     * @return 描述信息
     */
    default String description() {
        return "Agent implementation: " + name();
    }

    /**
     * Agent 版本
     *
     * @return 版本号
     */
    default String version() {
        return "1.0.0";
    }

    /**
     * 必填参数列表
     * Agent 执行前必须具备的参数
     *
     * @return 必填参数名称列表
     */
    default List<String> requiredParams() {
        return List.of();
    }

    /**
     * 可选参数列表
     * Agent 可以使用但非必需的参数
     *
     * @return 可选参数名称列表
     */
    default List<String> optionalParams() {
        return List.of();
    }

    /**
     * 参数描述信息
     * 用于生成提示文本给 LLM 提取参数
     *
     * @return 参数名 -> 描述信息的映射
     */
    default Map<String, String> paramDescriptions() {
        return Map.of();
    }

    /**
     * 验证任务参数是否完整
     * 如果使用了 routeWithParamCollection，此方法会返回验证结果
     *
     * @param task 任务对象
     * @return 验证通过返回 null，否则返回错误结果
     */
    default AgentResult validateParams(AgentTask task) {
        // 检查是否已经过路由层验证
        Boolean validationPassed = task.getContextValue("paramValidationPassed");
        if (Boolean.TRUE.equals(validationPassed)) {
            return null; // 验证通过
        }

        // 检查是否验证失败
        Boolean validationFailed = task.getContextValue("paramCollectionFailed");
        if (Boolean.TRUE.equals(validationFailed)) {
            List<String> missingParams = task.getContextValue("missingParams");
            String errorMsg = String.format("缺少必填参数: %s",
                missingParams != null ? String.join(", ", missingParams) : "unknown");
            return AgentResult.error(errorMsg);
        }

        // 未经过路由层验证，手动验证
        List<String> required = requiredParams();
        if (required != null && !required.isEmpty()) {
            for (String param : required) {
                if (!task.hasParam(param)) {
                    return AgentResult.error("Missing required parameter: " + param);
                }
            }
        }

        return null; // 验证通过
    }

    /**
     * 获取参数类型类
     * 用于参数提取和绑定
     *
     * @return 参数类型 Class
     */
    Class<?> getParamType();
}
