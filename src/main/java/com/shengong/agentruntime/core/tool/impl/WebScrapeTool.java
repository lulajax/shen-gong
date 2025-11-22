package com.shengong.agentruntime.core.tool.impl;

import com.shengong.agentruntime.core.tool.AbstractTool;
import com.shengong.agentruntime.core.tool.annotation.ToolDefinition;
import com.shengong.agentruntime.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.*;

/**
 * 网页抓取 Tool
 * 使用 Jsoup 抓取网页数据
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Slf4j
@ToolDefinition(
        name = "web_scrape_tool",
        description = "Scrape data from web pages using CSS selectors",
        category = "scraper"
)
public class WebScrapeTool extends AbstractTool {

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult invoke(Map<String, Object> arguments) {
        try {
            String url = (String) arguments.get("url");
            Map<String, String> selectors = (Map<String, String>) arguments.getOrDefault("selectors", Map.of());
            int timeout = ((Number) arguments.getOrDefault("timeout", 10000)).intValue();

            log.info("Scraping URL: {}", url);

            Document doc = Jsoup.connect(url)
                    .timeout(timeout)
                    .userAgent("Mozilla/5.0 (compatible; ShengongBot/1.0)")
                    .get();

            Map<String, Object> scrapedData = new HashMap<>();

            // 根据选择器提取数据
            for (Map.Entry<String, String> entry : selectors.entrySet()) {
                String fieldName = entry.getKey();
                String selector = entry.getValue();

                Elements elements = doc.select(selector);
                List<String> values = new ArrayList<>();

                for (Element element : elements) {
                    values.add(element.text());
                }

                scrapedData.put(fieldName, values.size() == 1 ? values.get(0) : values);
            }

            // 如果没有指定选择器,返回标题和文本内容
            if (selectors.isEmpty()) {
                scrapedData.put("title", doc.title());
                scrapedData.put("text", doc.body().text());
            }

            return ToolResult.success(Map.of(
                    "url", url,
                    "data", scrapedData
            ));

        } catch (Exception e) {
            log.error("Web scraping failed: {}", e.getMessage(), e);
            return ToolResult.failure("Web scraping failed: " + e.getMessage());
        }
    }
}
