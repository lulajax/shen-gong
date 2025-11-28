package com.shengong.agentruntime.core.agent.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengong.agentruntime.core.agent.AbstractAgent;
import com.shengong.agentruntime.core.agent.annotation.AgentDefinition;
import com.shengong.agentruntime.core.param.AgentParam;
import com.shengong.agentruntime.core.tool.Tool;
import com.shengong.agentruntime.llm.LlmClient;
import com.shengong.agentruntime.model.AgentResult;
import com.shengong.agentruntime.model.AgentTask;
import com.shengong.agentruntime.model.ToolResult;
import com.shengong.agentruntime.service.ToolRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * 订单每日统计 Agent
 * 查询指定销售地区列表和日期范围内的每日订单统计数据
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@Component
@AgentDefinition(
    name = "OrderStatisticsDailyAgent",
    domains = {"order"},
    taskType = "order_statistics_daily",
    description = "Fetch daily order statistics by regions and date range"
)
public class OrderDailyStatisticsAgent extends AbstractAgent<OrderDailyStatisticsAgent.OrderStatisticsDailyParams> {

    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final LlmClient llmClient;


    // 地区代码与中文名称映射
    private static final Map<String, String> REGION_NAME_MAP = Map.of(
        "GB", "英国",
        "US", "美国",
        "ES", "西班牙",
        "DE", "德国",
        "IE", "爱尔兰",
        "FR", "法国",
        "IT", "意大利"
    );

    public OrderDailyStatisticsAgent(ToolRegistry toolRegistry, LlmClient llmClient) {
        super(OrderStatisticsDailyParams.class);
        this.toolRegistry = toolRegistry;
        this.objectMapper = new ObjectMapper();
        this.llmClient = llmClient;
    }

    @Data
    public static class OrderStatisticsDailyParams {
        @AgentParam(required = true, description = "日期范围，格式：yyyy-MM-dd，例如 [\"2025-11-28\", \"2025-11-29\"]")
        private List<String> dateRange;

        @AgentParam(required = true, description = "销售地区代码列表（如：GB, US, ES等），例如 [\"GB\", \"US\", \"ES\"]")
        private List<String> regions;
    }

    @Override
    protected AgentResult execute(AgentTask task, OrderStatisticsDailyParams params) {
        log.info("OrderStatisticsDailyAgent handling task: {}", task.getTaskId());

        try {
            List<String> dateRange = params.getDateRange();
            List<String> regions = params.getRegions();

            // 参数验证
            if (dateRange == null || dateRange.size() != 2) {
                return AgentResult.error("dateRange 必须包含两个日期：开始日期和结束日期");
            }
            if (regions == null || regions.isEmpty()) {
                return AgentResult.error("regions 不能为空");
            }

            // 构建 HTTP 请求参数
            String url = buildRequestUrl(dateRange, regions);

            // 调用 HttpClientTool
            Tool httpTool = toolRegistry.getTool("http_client_tool")
                    .orElseThrow(() -> new RuntimeException("HTTP client tool not available"));

            ToolResult toolResult = httpTool.invoke(Map.of(
                    "url", url,
                    "method", "GET"
            ));

            if (!toolResult.isSuccess()) {
                return AgentResult.error("Failed to fetch order statistics: " + toolResult.getError());
            }

            // 解析响应数据
            Map<String, Object> response = (Map<String, Object>) toolResult.getData();
            String responseBody = (String) response.get("body");

            // 将 JSON 字符串解析为对象列表
            List<OrderStatisticsData> statisticsDataList = objectMapper.readValue(
                    responseBody,
                    new TypeReference<List<OrderStatisticsData>>() {}
            );

            if (CollectionUtils.isEmpty(statisticsDataList)) {
                return AgentResult.error("没有获取到订单数据");
            }
            // 使用 LLM 格式化数据为文本块
            String formattedData = formatDataWithLlm(statisticsDataList);

            return AgentResult.ok(formattedData, Map.of(
                    "rawData", statisticsDataList,
                    "formattedData", formattedData
            ));

        } catch (Exception e) {
            log.error("OrderStatisticsDailyAgent failed: {}", e.getMessage(), e);
            return AgentResult.error("订单统计数据获取失败: " + e.getMessage());
        }
    }

    /**
     * 构建请求 URL
     */
    private String buildRequestUrl(List<String> dateRange, List<String> regions) {
        String baseUrl = "https://service-wehub.youliaolive.cn/api/tiktok-seller-order/statistics/daily-by-type";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        builder.queryParam("regions", regions);
        // 添加日期范围参数
        builder.queryParam("dateRange", dateRange.get(0));
        builder.queryParam("dateRange", dateRange.get(1));

        String url = builder.build().encode().toUriString();
        log.info("Request URL: {}", url);
        return url;
    }

    /**
     * 使用 LLM 格式化数据为 Markdown 表格
     * 将原始订单统计数据格式化为清晰的 Markdown 表格
     */
    private String formatDataWithLlm(List<OrderStatisticsData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return "暂无数据";
        }

        try {
            // 构建格式化 Prompt
            String systemPrompt = """
                你是一个数据格式化专家,擅长将数据转换为清晰易读的 Markdown 表格格式。

                要求:
                1. 将提供的订单统计数据格式化为 Markdown 表格
                2. 表格列顺序为: 地区 | 日期 | 样品订单 | 达人订单 | 自营订单 | 商品卡订单 | 真实订单
                3. 国家地区需要翻译为中文（GB=英国, US=美国, ES=西班牙, DE=德国, IE=爱尔兰, FR=法国, IT=意大利）
                4. "真实订单" = 达人订单 + 自营订单 + 商品卡订单
                5. 只输出 Markdown 表格,不要添加额外的说明文字、标题或代码块标记

                示例格式:
                | 地区 | 日期 | 样品订单 | 达人订单 | 自营订单 | 商品卡订单 | 真实订单 |
                |------|------|----------|----------|----------|------------|----------|
                | 英国 | 2025-11-28 | 0 | 7 | 0 | 3 | 10 |
                | 美国 | 2025-11-28 | 2 | 15 | 5 | 8 | 28 |
                """;

            String dataJson = objectMapper.writeValueAsString(dataList);
            String userPrompt = "请将以下订单统计数据格式化为 Markdown 表格:\n\n" + dataJson;

            log.debug("Calling LLM to format data to Markdown table...");
            String markdownTable = llmClient.chat(systemPrompt, userPrompt);

            // 清理可能的额外内容（如代码块标记）
            markdownTable = markdownTable.trim();
            if (markdownTable.startsWith("```markdown")) {
                markdownTable = markdownTable.substring(11);
            }
            if (markdownTable.startsWith("```")) {
                int firstNewline = markdownTable.indexOf('\n');
                if (firstNewline > 0) {
                    markdownTable = markdownTable.substring(firstNewline + 1);
                }
            }
            if (markdownTable.endsWith("```")) {
                markdownTable = markdownTable.substring(0, markdownTable.length() - 3);
            }

            return markdownTable.trim();

        } catch (Exception e) {
            log.error("Failed to format data with LLM: {}", e.getMessage(), e);
            // 降级到简单格式化
            return formatResultFallback(dataList);
        }
    }

    /**
     * 降级的格式化方法（当 LLM 失败时使用）
     */
    private String formatResultFallback(List<OrderStatisticsData> dataList) {
        StringBuilder result = new StringBuilder();
        result.append("| 地区 | 日期 | 样品订单 | 达人订单 | 自营订单 | 商品卡订单 | 真实订单 |\n");
        result.append("|------|------|----------|----------|----------|------------|----------|\n");

        for (OrderStatisticsData data : dataList) {
            String regionName = REGION_NAME_MAP.getOrDefault(data.getSaleRegion(), data.getSaleRegion());
            int realOrders = data.getCreatorCount() + data.getSelfSellCount() + data.getProductCardCount();

            result.append(String.format("| %s | %s | %d | %d | %d | %d | %d |\n",
                    regionName,
                    data.getStatDate(),
                    data.getSampleCount(),
                    data.getCreatorCount(),
                    data.getSelfSellCount(),
                    data.getProductCardCount(),
                    realOrders
            ));
        }

        return result.toString();
    }

    /**
     * 订单统计数据模型
     */
    @Data
    public static class OrderStatisticsData {
        private String saleRegion;      // 销售地区代码
        private String statDate;         // 统计日期
        private int sampleCount;         // 样品订单数
        private int creatorCount;        // 达人订单数
        private int selfSellCount;       // 自营订单数
        private int productCardCount;    // 商品卡订单数
    }
}
