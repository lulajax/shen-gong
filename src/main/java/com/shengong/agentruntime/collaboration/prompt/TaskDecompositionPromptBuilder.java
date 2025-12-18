package com.shengong.agentruntime.collaboration.prompt;

import com.shengong.agentruntime.core.agent.Agent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 任务分解Prompt构建器
 * 生成用于LLM进行任务分解的System Prompt
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
public class TaskDecompositionPromptBuilder {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 构建判断是否需要协同的Prompt
     *
     * @param userInput 用户输入
     * @param agents    可用Agent列表
     * @return Prompt文本
     */
    public String buildCollaborationCheckPrompt(String userInput, List<Agent> agents) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个任务复杂度分析专家。请判断以下用户需求是否需要多个Agent协同处理。\n\n");

        // 可用Agent信息
        prompt.append("## 可用的Agent列表\n");
        for (Agent agent : agents) {
            prompt.append(String.format("- **%s** (%s)\n", agent.name(), agent.taskType()));
            prompt.append(String.format("  - 业务域: %s\n", String.join(", ", agent.domains())));
            prompt.append(String.format("  - 描述: %s\n", agent.description()));
        }
        prompt.append("\n");

        // 判断标准
        prompt.append("## 判断标准\n");
        prompt.append("以下情况需要多Agent协同:\n");
        prompt.append("1. 任务需要多个数据源(如同时需要直播数据和订单数据)\n");
        prompt.append("2. 任务有明显的多个步骤(如先获取数据,再计算指标,最后分析)\n");
        prompt.append("3. 任务需要多个专业能力(如数据获取+数据处理+数据分析)\n");
        prompt.append("4. 任务描述中明确提到\"对比\"、\"综合\"、\"分析...和...\"等关键词\n\n");

        prompt.append("以下情况不需要协同(单Agent即可):\n");
        prompt.append("1. 简单的问候、闲聊\n");
        prompt.append("2. 单一数据源的查询\n");
        prompt.append("3. 明确只需要一个Agent能力的任务\n\n");

        // 用户输入
        prompt.append("## 用户需求\n");
        prompt.append(String.format("\"%s\"\n\n", userInput));

        // 输出格式
        prompt.append("## 输出格式\n");
        prompt.append("请输出JSON格式,包含以下字段:\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"needsCollaboration\": true/false,\n");
        prompt.append("  \"reason\": \"判断理由\",\n");
        prompt.append("  \"suggestedAgents\": [\"Agent1\", \"Agent2\"]\n");
        prompt.append("}\n");
        prompt.append("```\n");

        return prompt.toString();
    }

    /**
     * 构建任务分解Prompt
     *
     * @param userInput 用户输入
     * @param agents    可用Agent列表
     * @return Prompt文本
     */
    public String buildDecompositionPrompt(String userInput, List<Agent> agents) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个任务分解专家。请将用户的复杂需求分解为多个子任务,并分配给合适的Agent。\n\n");

        // 可用Agent详细信息
        prompt.append("## 可用的Agent列表\n");
        for (Agent agent : agents) {
            prompt.append(String.format("### %s\n", agent.name()));
            prompt.append(String.format("- **任务类型**: %s\n", agent.taskType()));
            prompt.append(String.format("- **业务域**: %s\n", String.join(", ", agent.domains())));
            prompt.append(String.format("- **描述**: %s\n", agent.description()));
            prompt.append(String.format("- **版本**: %s\n", agent.version()));

            // 参数信息
            List<String> requiredParams = agent.requiredParams();
            if (requiredParams != null && !requiredParams.isEmpty()) {
                prompt.append(String.format("- **必填参数**: %s\n", String.join(", ", requiredParams)));
            }
            prompt.append("\n");
        }

        // 分解原则
        prompt.append("## 任务分解原则\n");
        prompt.append("1. **依赖关系**: 如果一个任务需要另一个任务的输出,则建立依赖关系\n");
        prompt.append("2. **执行模式**:\n");
        prompt.append("   - sequential: 任务有明确的前后依赖关系,需要串行执行\n");
        prompt.append("   - parallel: 任务之间独立,可以并行执行(暂未实现,使用sequential)\n");
        prompt.append("   - hierarchical: 主Agent动态调用子Agent(暂未实现,使用sequential)\n");
        prompt.append("3. **数据映射**: 使用dataMappings指定如何从前置任务获取数据\n");
        prompt.append("   - 格式: {\"参数名\": \"task-id.data.字段路径\"}\n");
        prompt.append("   - 例如: {\"rawData\": \"task-1.data.liveMetrics\"}\n");
        prompt.append("4. **Agent选择**: 根据任务类型和业务域选择最合适的Agent\n\n");

        // 当前时间
        prompt.append(String.format("## 当前时间\n北京时间: %s\n\n",
                                   LocalDateTime.now().format(FORMATTER)));

        // 用户输入
        prompt.append("## 用户需求\n");
        prompt.append(String.format("\"%s\"\n\n", userInput));

        // Few-shot 示例
        prompt.append("## 示例\n");
        prompt.append("**用户**: \"分析昨天直播间的表现\"\n\n");
        prompt.append("**输出**:\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"needsCollaboration\": true,\n");
        prompt.append("  \"reason\": \"需要获取直播数据、计算指标、生成分析报告,涉及3个步骤\",\n");
        prompt.append("  \"executionMode\": \"sequential\",\n");
        prompt.append("  \"subTasks\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"taskId\": \"task-1\",\n");
        prompt.append("      \"agentName\": \"LiveDataFetchAgent\",\n");
        prompt.append("      \"taskType\": \"live_data_fetch\",\n");
        prompt.append("      \"domain\": \"live\",\n");
        prompt.append("      \"description\": \"获取昨天的直播数据\",\n");
        prompt.append("      \"dependencies\": [],\n");
        prompt.append("      \"staticParams\": {\n");
        prompt.append("        \"timeRange\": \"yesterday\"\n");
        prompt.append("      }\n");
        prompt.append("    },\n");
        prompt.append("    {\n");
        prompt.append("      \"taskId\": \"task-2\",\n");
        prompt.append("      \"agentName\": \"LiveDataPrepAgent\",\n");
        prompt.append("      \"taskType\": \"live_data_prep\",\n");
        prompt.append("      \"domain\": \"live\",\n");
        prompt.append("      \"description\": \"计算直播指标\",\n");
        prompt.append("      \"dependencies\": [\"task-1\"],\n");
        prompt.append("      \"dataMappings\": {\n");
        prompt.append("        \"rawData\": \"task-1.data.liveMetrics\"\n");
        prompt.append("      }\n");
        prompt.append("    },\n");
        prompt.append("    {\n");
        prompt.append("      \"taskId\": \"task-3\",\n");
        prompt.append("      \"agentName\": \"LiveAnalysisAgent\",\n");
        prompt.append("      \"taskType\": \"live_analysis\",\n");
        prompt.append("      \"domain\": \"live\",\n");
        prompt.append("      \"description\": \"分析直播表现\",\n");
        prompt.append("      \"dependencies\": [\"task-2\"],\n");
        prompt.append("      \"dataMappings\": {\n");
        prompt.append("        \"metrics\": \"task-2.data.metrics\"\n");
        prompt.append("      }\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");

        // 输出格式要求
        prompt.append("## 输出格式\n");
        prompt.append("请严格按照以下JSON格式输出:\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"needsCollaboration\": true,\n");
        prompt.append("  \"reason\": \"为什么需要多Agent协同\",\n");
        prompt.append("  \"executionMode\": \"sequential\",\n");
        prompt.append("  \"subTasks\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"taskId\": \"task-1\",\n");
        prompt.append("      \"agentName\": \"Agent名称\",\n");
        prompt.append("      \"taskType\": \"任务类型\",\n");
        prompt.append("      \"domain\": \"业务域\",\n");
        prompt.append("      \"description\": \"任务描述\",\n");
        prompt.append("      \"dependencies\": [\"依赖的taskId列表\"],\n");
        prompt.append("      \"dataMappings\": {\"参数名\": \"task-id.data.字段\"},\n");
        prompt.append("      \"staticParams\": {\"参数名\": \"参数值\"}\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");

        prompt.append("**注意事项**:\n");
        prompt.append("- taskId使用格式: task-1, task-2, task-3...\n");
        prompt.append("- 确保Agent名称和任务类型匹配可用Agent列表\n");
        prompt.append("- 依赖关系必须合法,不能有循环依赖\n");
        prompt.append("- dataMappings中的路径格式: taskId.data.字段名\n");
        prompt.append("- 当前仅支持sequential模式\n");

        return prompt.toString();
    }
}
