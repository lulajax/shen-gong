# Shen Gong Agent Runtime (RoleAgent + Capability)

本项目已收敛为单一运行链路：

`RoleAgent -> CapabilityOrchestrator -> Capability -> ToolExecutor -> ToolAdapter`

## 当前入口

- `POST /api/v2/role-agent/send`

旧入口已下线：

- `/api/v1/agent/**`
- `/api/v1/chat/send`
- `/api/v1/tasks/**`

## 请求协议

```json
{
  "role": "ecom_assistant",
  "userId": "u-1",
  "sessionId": "s-1",
  "inputText": "查询英国和美国 2026-03-01 到 2026-03-03 的订单日报",
  "context": {
    "role": "ecom_assistant",
    "messages": []
  },
  "payload": {
    "dateRange": ["2026-03-01", "2026-03-03"],
    "regions": ["GB", "US"]
  }
}
```

默认值与别名：

- `role` 未传时优先读取 `context.role`，都没有则默认 `ecom_assistant`
- `role="电商小助理"` 自动映射到 `ecom_assistant`
- 单能力默认直连，`context.forcePlanner=true` 时才走 planner
- 编排参数：`maxSteps=8`，`maxDepth=2`

## 当前能力与工具

- Role: `ecom_assistant`（电商小助理）
- Capability: `order.daily_statistics`
- Tool: `order.daily.statistics.fetch`（HTTP）

## 配置驱动

运行时规格由 YAML 加载：

- `src/main/resources/agent-runtime/roles.yml`
- `src/main/resources/agent-runtime/capabilities.yml`
- `src/main/resources/agent-runtime/tools.yml`

参数协议统一为 `Map<String,Object> + JSON Schema`，采用严格校验失败策略。

## 目录概览

- `core/role`: 角色门面
- `core/capability`: 能力单元
- `service/runtime/orchestration`: 编排层（`CapabilityOrchestrator`、`CapabilityPlanner`）
- `service/runtime/registry`: 规格与组件注册层（role/capability/spec）
- `service/runtime/capability/executor`: 能力执行策略层（`LocalCapabilityExecutor`、`A2aCapabilityExecutor`）
- `service/runtime/tool`: 工具执行层
- `service/runtime/tool/adapter`: 工具适配器策略层
- `service/runtime/argument`: 动态参数抽取层
- `service/runtime/validation`: JSON Schema 校验层
- `model/runtime`: 动态请求与能力结果
- `model/spec`: 角色/能力/工具规格模型

## 本地运行

```bash
mvn -DskipTests compile
mvn test
mvn spring-boot:run
```

## 内置静态页

- `src/main/resources/static/chat.js`
- `src/main/resources/static/embed/chat-embed.js`

已迁移至 `POST /api/v2/role-agent/send`，默认 `role=ecom_assistant`。
