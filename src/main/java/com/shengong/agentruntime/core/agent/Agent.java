package com.shengong.agentruntime.core.agent;

import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import java.util.List;

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
}
