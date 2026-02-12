package com.shengong.agentruntime.core.agent.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.shengong.agentruntime.core.agent.AbstractAgent;
import com.shengong.agentruntime.core.agent.annotation.AgentDefinition;
import com.shengong.agentruntime.core.param.AgentParam;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import com.shengong.agentruntime.model.ToolResult;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 创建fastmoss商品关联达人RPA采集任务 Agent
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
@AgentDefinition(
    name = "FastmossGoodsAuthorFetchAgent",
    domains = {"shop"},
    taskType = "fastmoss_goods_author_fetch",
    description = "Create a fastmoss task to fetch associated authors for the specified product"
)
public class FastmossGoodsAuthorFetchAgent extends AbstractAgent<FastmossGoodsAuthorFetchAgent.FastmossGoodsAuthorFetchParams> {

    private final String fastmosssTaskUrl;

    public FastmossGoodsAuthorFetchAgent(@Value("${agent-runtime.fastmoss.task-url:http://192.168.84.30:8090/api/fastmoss/rpa/task}") String fastmosssTaskUrl) {
        super(FastmossGoodsAuthorFetchParams.class);
        this.fastmosssTaskUrl = fastmosssTaskUrl;
    }

    @Data
    public static class FastmossGoodsAuthorFetchParams {
        @AgentParam(required = true, description = "国家/地区代码（如 GB, US, FR 等）")
        private String region;

        @AgentParam(required = false, description = "商品分类（选择项：女士香水、理发棒）")
        private String category = "女士香水";
    }

    @Override
    protected AgentResult execute(AgentTask task, FastmossGoodsAuthorFetchParams params) {
        log.info("FastmossGoodsAuthorFetchAgent handling task: {}", task.getTaskId());

        try {
            String taskId = UUID.randomUUID().toString();
            Map<String, Object> requestBody = Map.of(
                "region", params.getRegion(),
                "productCategory", params.getCategory(),
                "taskId", taskId
            );
            ToolResult toolResult = invokeToolWithLlm(task, Map.of(
                    "url", fastmosssTaskUrl,
                    "method", "POST",
                    "headers", Map.of("Content-Type", "application/json"),
                    "body", requestBody
            ));

            if (!toolResult.isSuccess()) {
                return AgentResult.error("Failed to fetch fastmoss goods author: " + toolResult.getError());
            }

            return AgentResult.ok("fastmoss采集任务创建成功，任务ID：" + taskId, Map.of("taskId", taskId));
        } catch (Exception e) {
            log.error("FastmossGoodsAuthorFetch failed: {}", e.getMessage(), e);
            return AgentResult.error("fastmoss采集任务创建失败，错误信息：" + e.getMessage());
        }
    }

    @Override
    public List<String> allowedToolNames() {
        return List.of("http_client_tool");
    }
}
