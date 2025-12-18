package com.shengong.agentruntime.collaboration.util;

import com.shengong.agentruntime.core.agent.Agent;
import com.shengong.agentruntime.model.collaboration.ExecutionPlan;
import com.shengong.agentruntime.model.collaboration.SubTask;
import com.shengong.agentruntime.service.AgentRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 执行计划验证器
 * 验证执行计划的合法性
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionPlanValidator {

    private final AgentRegistry agentRegistry;

    /**
     * 验证执行计划
     *
     * @param plan 执行计划
     * @return 验证结果
     */
    public ValidationResult validate(ExecutionPlan plan) {
        List<String> errors = new ArrayList<>();

        if (plan == null) {
            errors.add("执行计划为空");
            return new ValidationResult(false, errors);
        }

        // 1. 验证子任务列表不为空
        if (plan.getSubTasks() == null || plan.getSubTasks().isEmpty()) {
            errors.add("子任务列表为空");
            return new ValidationResult(false, errors);
        }

        // 2. 验证taskId唯一性
        Set<String> taskIds = new HashSet<>();
        for (SubTask task : plan.getSubTasks()) {
            if (task.getTaskId() == null || task.getTaskId().trim().isEmpty()) {
                errors.add("存在taskId为空的子任务");
            } else if (!taskIds.add(task.getTaskId())) {
                errors.add(String.format("taskId重复: %s", task.getTaskId()));
            }
        }

        // 3. 验证Agent存在性
        for (SubTask task : plan.getSubTasks()) {
            if (task.getAgentName() == null || task.getAgentName().trim().isEmpty()) {
                errors.add(String.format("任务 %s 的agentName为空", task.getTaskId()));
                continue;
            }

            if (task.getTaskType() == null || task.getDomain() == null) {
                errors.add(String.format("任务 %s 的taskType或domain为空", task.getTaskId()));
                continue;
            }

            // 检查Agent是否存在
            Agent agent = agentRegistry.findAgent(task.getTaskType(), task.getDomain())
                    .orElse(null);

            if (agent == null) {
                errors.add(String.format("任务 %s 的Agent不存在: %s (type=%s, domain=%s)",
                        task.getTaskId(), task.getAgentName(),
                        task.getTaskType(), task.getDomain()));
            }
        }

        // 4. 验证依赖关系合法性
        for (SubTask task : plan.getSubTasks()) {
            if (task.getDependencies() != null) {
                for (String depId : task.getDependencies()) {
                    if (!taskIds.contains(depId)) {
                        errors.add(String.format("任务 %s 依赖的任务不存在: %s",
                                task.getTaskId(), depId));
                    }

                    // 自依赖检查
                    if (depId.equals(task.getTaskId())) {
                        errors.add(String.format("任务 %s 存在自依赖", task.getTaskId()));
                    }
                }
            }
        }

        // 5. 验证循环依赖
        if (!plan.isValid()) {
            errors.add("执行计划存在循环依赖");
        }

        boolean isValid = errors.isEmpty();
        if (!isValid) {
            log.warn("执行计划验证失败: {}", errors);
        }

        return new ValidationResult(isValid, errors);
    }

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
