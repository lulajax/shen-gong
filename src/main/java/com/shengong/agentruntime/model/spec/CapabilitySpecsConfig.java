package com.shengong.agentruntime.model.spec;

import java.util.List;

/**
 * 能力规格配置集合，对应 capabilities.yml 顶层结构。
 */
public record CapabilitySpecsConfig(List<CapabilitySpec> capabilities) {
}
