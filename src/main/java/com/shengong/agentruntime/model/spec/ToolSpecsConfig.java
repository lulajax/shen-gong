package com.shengong.agentruntime.model.spec;

import java.util.List;

/**
 * 工具规格配置集合，对应 tools.yml 顶层结构。
 */
public record ToolSpecsConfig(List<ToolSpec> tools) {
}
