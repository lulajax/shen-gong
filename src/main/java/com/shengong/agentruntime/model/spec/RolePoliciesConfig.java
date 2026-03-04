package com.shengong.agentruntime.model.spec;

import java.util.List;

/**
 * 角色策略配置集合，对应 roles.yml 顶层结构。
 */
public record RolePoliciesConfig(List<RolePolicy> roles) {
}
