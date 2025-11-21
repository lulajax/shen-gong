package com.shengong.agentruntime.core.tool;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Tool 类型枚举
 * 统一管理所有 Tool 的配置信息
 *
 * @author 神工团队
 * @since 1.0.0
 */
public enum ToolType {

    /**
     * 直播数据工具
     */
    LIVE_DATA(
            "live_data_tool",
            "Fetch live streaming data from external service",
            "data-source"
    ),

    /**
     * 订单数据工具
     */
    ORDER_DATA(
            "order_data_tool",
            "Fetch order data from order service",
            "data-source"
    ),

    /**
     * HTTP 客户端工具
     */
    HTTP_CLIENT(
            "http_client_tool",
            "Make HTTP requests to external services",
            "http"
    ),

    /**
     * 网页抓取工具
     */
    WEB_SCRAPE(
            "web_scrape_tool",
            "Scrape data from web pages using CSS selectors",
            "scraper"
    ),

    /**
     * MCP 代理工具
     */
    MCP_PROXY(
            "mcp_proxy_tool",
            "Proxy tool for Model Context Protocol (MCP) services",
            "mcp"
    );

    private final String name;
    private final String description;
    private final String category;

    ToolType(String name, String description, String category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    /**
     * 获取 Tool 名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取 Tool 描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取 Tool 分类
     */
    public String getCategory() {
        return category;
    }

    /**
     * 根据名称查找 ToolType
     */
    public static Optional<ToolType> fromName(String name) {
        return Arrays.stream(values())
                .filter(type -> type.getName().equals(name))
                .findFirst();
    }

    /**
     * 获取所有 Tool 名称列表
     */
    public static List<String> getAllNames() {
        return Arrays.stream(values())
                .map(ToolType::getName)
                .toList();
    }

    /**
     * 根据分类查找所有 ToolType
     */
    public static List<ToolType> findByCategory(String category) {
        return Arrays.stream(values())
                .filter(type -> type.getCategory().equals(category))
                .toList();
    }

    /**
     * 获取所有分类列表
     */
    public static List<String> getAllCategories() {
        return Arrays.stream(values())
                .map(ToolType::getCategory)
                .distinct()
                .toList();
    }

    @Override
    public String toString() {
        return String.format("ToolType{name='%s', description='%s', category='%s'}",
                name, description, category);
    }
}
