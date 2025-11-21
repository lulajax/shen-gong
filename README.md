# 神工 (Shen Gong) 

> 企业级智能 Agent 工作流平台 - 让 AI 为业务赋能

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4+-green.svg)](https://spring.io/projects/spring-boot)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.35+-purple.svg)](https://docs.langchain4j.dev/)

## ✨ 简介

**神工 (Shen Gong) ** 是一个功能强大的企业级多 Agent 协作平台，通过智能路由、工作流编排和可插拔架构，帮助企业快速构建 AI 驱动的智能决策和分析系统。

### 🎯 核心特性

- **🤖 多 Agent 协作** - 8+ 内置 Agent，支持复杂业务场景的智能协作
- **🧩 插件化架构** - Agent、Tool 均采用枚举配置，统一管理，易于扩展
- **🧠 智能路由系统** - 基于任务类型和业务域自动路由到最合适的 Agent
- **🔧 丰富的工具集** - 5+ 内置 Tool，支持数据源、HTTP、爬虫、MCP 协议等
- **📊 执行记录追踪** - 完整的任务执行历史记录和链路追踪
- **🌐 LLM 深度集成** - 基于 LangChain4j，支持 OpenAI、Gemini 等多种大语言模型
- **🚀 高性能架构** - 基于 Spring Boot，支持响应式编程和高并发

### 📦 内置能力

**Agents** (8个示例)
- `GenericAnalysisAgent` - 通用分析
- `OrderDataAgent` - 订单数据获取
- `AnomalyDetectionAgent` - 异常检测
- `RootCauseAgent` - 根因分析
- `LiveDataFetchAgent` - 直播数据获取
- `LiveDataPrepAgent` - 直播数据预处理
- `LiveAnalysisAgent` - 直播数据分析
- `LiveReportAgent` - 直播报告生成

**Tools** (5个示例)
- `LiveDataTool` - 直播数据源
- `OrderDataTool` - 订单数据源
- `HttpClientTool` - HTTP 请求
- `WebScrapeTool` - 网页抓取
- `McpProxyTool` - MCP 协议代理

## 📋 目录

- [快速开始](#-快速开始)
- [应用场景](#-应用场景)
- [系统架构](#-系统架构)
- [核心概念](#-核心概念)
- [API 文档](#-api-文档)
- [开发指南](#-开发指南)
- [配置说明](#-配置说明)
- [详细文档](#-详细文档)
- [常见问题](#-常见问题)
- [贡献指南](#-贡献指南)

## 🚀 快速开始

### 前置要求

- **JDK 21+**
- **Maven 3.9+**
- **MySQL 8.0+**
- **Docker** (可选，用于容器化部署)

### 本地快速启动

#### 1. 克隆项目

```bash
git clone https://github.com/yourcompany/shen-gong.git
cd shen-gong
```

#### 2. 配置数据库

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE shengong DEFAULT CHARACTER SET utf8mb4;"

# 导入表结构
mysql -u root -p shengong < scripts/schema.sql
```

#### 3. 配置应用

编辑 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/shengong?useUnicode=true&characterEncoding=utf8mb4
    username: root
    password: your_password

# LLM 配置 (选择其中之一)
langchain4j:
  open-ai:
    api-key: ${OPENAI_API_KEY}  # 使用 OpenAI
  # 或使用 Gemini
  google-ai-gemini:
    api-key: ${GEMINI_API_KEY}
```

#### 4. 启动应用

```bash
# 使用 Maven
mvn spring-boot:run

# 或使用 start.sh 脚本
./start.sh
```

#### 5. 验证服务

访问 Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

健康检查:
```bash
curl http://localhost:8080/api/v1/agent/health
```

### 🐳 Docker Compose 快速启动

```bash
# 启动所有服务（包括 MySQL）
docker-compose up -d

# 查看日志
docker-compose logs -f agent-runtime

# 停止服务
docker-compose down
```

### 📝 第一个 API 调用

```bash
curl -X POST http://localhost:8080/api/v1/agent/handle \
  -H "Content-Type: application/json" \
  -d '{
    "taskType": "analysis",
    "domain": "generic",
    "taskId": "test-001",
    "traceId": "trace-001",
    "payload": {
      "text": "分析一下这段话的情感倾向：今天天气真好，心情特别愉快！"
    }
  }'
```

## 💡 应用场景

### 1. 📈 直播数据分析与复盘

**场景**: 自动分析直播间数据，生成复盘报告并提供优化建议

**工作流**:
```
LiveDataFetchAgent → LiveDataPrepAgent → LiveAnalysisAgent → LiveReportAgent
```

**请求示例**:
```json
{
  "taskType": "analysis_report",
  "domain": "live",
  "taskId": "live-001",
  "traceId": "trace-live-001",
  "payload": {
    "timeRange": {
      "from": "2025-11-20T00:00:00Z",
      "to": "2025-11-20T23:59:59Z"
    },
    "filters": {
      "roomId": "123456"
    }
  }
}
```

**返回结果**:
- GMV 趋势分析
- 观众流失分析
- 互动数据洞察
- 优化建议

### 2. 🔍 订单异常检测与根因分析

**场景**: 检测订单异常（退货率高、发货延迟等），分析根本原因

**工作流**:
```
OrderDataAgent → AnomalyDetectionAgent → RootCauseAgent
```

**请求示例**:
```json
{
  "taskType": "anomaly_detection",
  "domain": "order",
  "taskId": "order-001",
  "traceId": "trace-order-001",
  "payload": {
    "timeRange": {
      "from": "2025-11-15T00:00:00Z",
      "to": "2025-11-21T23:59:59Z"
    }
  }
}
```

**返回结果**:
- 异常模式识别（退货率高、延迟发货等）
- 根因分析（供应链问题、质量问题等）
- 解决方案建议

### 3. 🌐 网页数据抓取与分析

**场景**: 抓取竞品网站数据，进行智能分析

**使用 Tool**:
```json
{
  "tool": "web_scrape_tool",
  "arguments": {
    "url": "https://example.com/products",
    "selectors": {
      "title": ".product-title",
      "price": ".product-price"
    }
  }
}
```

## 🏗️ 系统架构

### 整体架构

```
┌─────────────────────────────────────────────────────┐
│                   API Gateway Layer                  │
│              (REST API / GraphQL)                    │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│              Intelligent Router Layer                │
│   (IntelligentAgentRouter - 智能路由决策)            │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│                   Agent Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │   Analysis   │  │  DataFetch   │  │  Report   │ │
│  │    Agents    │  │    Agents    │  │  Agents   │ │
│  └──────────────┘  └──────────────┘  └───────────┘ │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│                    Tool Layer                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐          │
│  │   Data   │  │   HTTP   │  │  Scrape  │  ...     │
│  │  Source  │  │  Client  │  │   Tool   │          │
│  └──────────┘  └──────────┘  └──────────┘          │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│                  LLM Service Layer                   │
│         OpenAI / Gemini / Claude / Local            │
└─────────────────────────────────────────────────────┘
```

### 数据流

```
Request → Router → Agent Selection → Tool Execution → LLM Processing → Response
   ↓                                                                      ↑
TaskExecutionService (记录执行历史和追踪)                                |
```

## 📚 核心概念

### Agent (智能体)

Agent 是执行特定任务的智能组件，每个 Agent 负责一个明确的业务场景。

**接口定义**:
```java
public interface Agent {
    String name();                              // Agent 名称
    List<String> domains();                     // 支持的业务域
    boolean supports(String taskType, String domain);  // 是否支持某任务
    AgentResult handle(AgentTask task);         // 处理任务
    String description();                       // Agent 描述
}
```

**使用枚举配置**:
```java
public enum AgentType {
    ANOMALY_DETECTION(
        "AnomalyDetectionAgent",
        List.of("order"),
        "anomaly_detection",
        "Detect anomalies in order data using rules and LLM"
    ),
    // ...
}
```

### Tool (工具)

Tool 封装对外部系统的调用，为 Agent 提供数据和能力。

**接口定义**:
```java
public interface Tool {
    String name();                              // Tool 名称
    String description();                       // Tool 描述
    String category();                          // Tool 分类
    ToolResult invoke(Map<String, Object> arguments);  // 调用工具
}
```

**使用枚举配置**:
```java
public enum ToolType {
    LIVE_DATA(
        "live_data_tool",
        "Fetch live streaming data from external service",
        "data-source"
    ),
    // ...
}
```

### 智能路由

`IntelligentAgentRouter` 根据任务类型和业务域自动选择最合适的 Agent。

**路由逻辑**:
1. 解析任务的 `taskType` 和 `domain`
2. 从 `AgentRegistry` 中查找匹配的 Agent
3. 如果找到多个，根据优先级选择
4. 执行 Agent 并记录执行历史

### 执行记录

`TaskExecutionService` 记录所有任务的执行历史，支持：
- 任务执行追踪
- 调用链分析（traceId）
- 性能统计
- 错误诊断

## 📖 API 文档

### 核心 API

#### 1. 处理任务

```http
POST /api/v1/agent/handle
Content-Type: application/json

{
  "taskType": "analysis",
  "domain": "generic",
  "taskId": "task-001",
  "traceId": "trace-001",
  "payload": {
    "text": "要分析的文本"
  },
  "context": {}
}
```

**响应**:
```json
{
  "status": "ok",
  "summary": "分析完成",
  "data": {
    "result": "分析结果"
  },
  "latencyMs": 1234,
  "agentName": "GenericAnalysisAgent"
}
```

#### 2. 查询任务执行记录

```http
GET /api/v1/agent/task/{taskId}
```

#### 3. 查询调用链

```http
GET /api/v1/agent/trace/{traceId}
```

#### 4. 查询最近任务

```http
GET /api/v1/tasks/recent
```

#### 5. 统计信息

```http
GET /api/v1/tasks/statistics
```

### Swagger UI

访问完整的 API 文档:
```
http://localhost:8080/swagger-ui.html
```

## 🛠️ 开发指南

### 项目结构

```
shen-gong/
├── src/main/java/com/shengong/agentruntime/
│   ├── controller/              # REST API 控制器
│   │   ├── AgentController.java
│   │   ├── ChatController.java
│   │   └── TaskExecutionController.java
│   ├── core/                    # 核心模块
│   │   ├── agent/               # Agent 实现
│   │   │   ├── Agent.java
│   │   │   ├── AgentType.java   # Agent 枚举配置
│   │   │   └── impl/            # 8 个 Agent 实现
│   │   └── tool/                # Tool 实现
│   │       ├── Tool.java
│   │       ├── ToolType.java    # Tool 枚举配置
│   │       └── impl/            # 5 个 Tool 实现
│   ├── service/                 # 业务服务
│   │   ├── IntelligentAgentRouter.java  # 智能路由
│   │   ├── RouterAgentService.java      # 路由服务
│   │   ├── TaskExecutionService.java    # 执行记录
│   │   ├── AgentRegistry.java           # Agent 注册表
│   │   └── ToolRegistry.java            # Tool 注册表
│   ├── entity/                  # 数据实体
│   ├── repository/              # 数据访问层
│   ├── model/                   # 数据模型
│   ├── llm/                     # LLM 客户端
│   └── config/                  # 配置类
├── src/main/resources/
│   ├── application.yml          # 应用配置
│   ├── application-dev.yml      # 开发环境配置
│   └── application-prod.yml     # 生产环境配置
├── docs/                        # 文档目录
├── scripts/                     # 脚本目录
│   └── schema.sql               # 数据库表结构
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

### 添加自定义 Agent

1. **创建 Agent 枚举配置**:

```java
// 在 AgentType.java 中添加
CUSTOM_AGENT(
    "CustomAgent",
    List.of("custom"),
    "custom_task",
    "My custom agent description"
)
```

2. **实现 Agent 类**:

```java
@Component
public class CustomAgent implements Agent {

    private static final AgentType AGENT_TYPE = AgentType.CUSTOM_AGENT;

    @Override
    public String name() {
        return AGENT_TYPE.getName();
    }

    @Override
    public List<String> domains() {
        return AGENT_TYPE.getDomains();
    }

    @Override
    public boolean supports(String taskType, String domain) {
        return AGENT_TYPE.supports(taskType, domain);
    }

    @Override
    public AgentResult handle(AgentTask task) {
        // 实现你的业务逻辑
        return AgentResult.ok("处理完成", Map.of("result", "data"));
    }

    @Override
    public String description() {
        return AGENT_TYPE.getDescription();
    }
}
```

3. **重启应用**，Agent 会自动注册

### 添加自定义 Tool

1. **创建 Tool 枚举配置**:

```java
// 在 ToolType.java 中添加
CUSTOM_TOOL(
    "custom_tool",
    "My custom tool description",
    "custom-category"
)
```

2. **实现 Tool 类**:

```java
@Component
public class CustomTool implements Tool {

    private static final ToolType TOOL_TYPE = ToolType.CUSTOM_TOOL;

    @Override
    public String name() {
        return TOOL_TYPE.getName();
    }

    @Override
    public String description() {
        return TOOL_TYPE.getDescription();
    }

    @Override
    public String category() {
        return TOOL_TYPE.getCategory();
    }

    @Override
    public ToolResult invoke(Map<String, Object> arguments) {
        // 实现你的工具逻辑
        return ToolResult.success(Map.of("result", "data"));
    }
}
```

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行指定测试
mvn test -Dtest=AgentControllerTest

# 生成测试报告
mvn test jacoco:report
```

## ⚙️ 配置说明

### 核心配置项

```yaml
spring:
  application:
    name: agent-runtime

  # 数据库配置
  datasource:
    url: jdbc:mysql://localhost:3306/shengong
    username: root
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20

  # JPA 配置
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

# LLM 配置
langchain4j:
  open-ai:
    api-key: ${OPENAI_API_KEY}
    model-name: gpt-4
    temperature: 0.7
    timeout: 60s

#  配置
agent-runtime:
  # 限流配置
  rate-limit:
    enabled: true
    qps: 1000
```

### 环境变量

| 变量名 | 说明 | 必需 |
|--------|------|------|
| `DB_PASSWORD` | 数据库密码 | 是 |
| `OPENAI_API_KEY` | OpenAI API Key | 否* |
| `GEMINI_API_KEY` | Gemini API Key | 否* |

\* OpenAI 和 Gemini 至少配置一个

## 📚 详细文档

完整的文档已整理到 `docs` 目录：

### 📖 [文档中心](./docs/README.md)

**使用指南**
- [Gemini 多模态使用指南](./docs/guides/GEMINI_MULTIMODAL_GUIDE.md) - Gemini 多模态大模型的集成和使用
- [智能路由指南](./docs/guides/INTELLIGENT_ROUTING_GUIDE.md) - Agent 智能路由系统的使用说明

**设计文档**
- [项目设计文档](./docs/design/PROJECT_DESIGN.md) - 完整的系统架构和设计方案
- [项目摘要](./docs/design/PROJECT_SUMMARY.md) - 项目概览和核心特性

**开发文档**
- [开发指南](./docs/README_DEV.md) - 开发环境配置和开发流程

## ❓ 常见问题

### Q1: 如何切换不同的 LLM 模型？

编辑 `application.yml`:

```yaml
# 使用 OpenAI
langchain4j:
  open-ai:
    api-key: ${OPENAI_API_KEY}
    model-name: gpt-4  # 或 gpt-3.5-turbo

# 使用 Gemini
langchain4j:
  google-ai-gemini:
    api-key: ${GEMINI_API_KEY}
    model-name: gemini-1.5-flash
```

### Q2: Agent 是如何被路由的？

`IntelligentAgentRouter` 根据以下规则路由：
1. 匹配 `taskType` 和 `domain`
2. 从 `AgentRegistry` 查找所有支持的 Agent
3. 选择最匹配的 Agent 执行
4. 记录执行历史到数据库

### Q3: 如何查看任务执行历史？

```bash
# 查询单个任务
curl http://localhost:8080/api/v1/agent/task/{taskId}

# 查询调用链
curl http://localhost:8080/api/v1/agent/trace/{traceId}

# 查询最近任务
curl http://localhost:8080/api/v1/tasks/recent
```

### Q4: 如何启用 MCP 协议支持？

1. 配置 MCP 代理地址:
```yaml
agent-runtime:
  mcp:
    enabled: true
    proxy:
      url: http://localhost:3000
```

2. 使用 `McpProxyTool` 调用外部服务

### Q5: 性能如何优化？

- 启用数据库连接池
- 使用异步处理
- 启用缓存
- 调整 LLM timeout 配置

## 🤝 贡献指南

我们欢迎所有形式的贡献！

### 贡献方式

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### 提交规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/):

- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具

## 🗺️ 路线图

### v1.1.0 (2025 Q2)
- [ ] 支持更多 LLM 模型（Claude、通义千问）
- [ ] 工作流引擎
- [ ] Agent 性能监控
- [ ] 多租户支持

### v1.2.0 (2025 Q3)
- [ ] 流式响应
- [ ] Agent 热更新
- [ ] 可视化编排
- [ ] A/B 测试

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

## 🙏 致谢

感谢以下开源项目：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [LangChain4j](https://docs.langchain4j.dev/)
- [MySQL](https://www.mysql.com/)

---

<p align="center">
  <b>Made with ❤️ by 神工团队</b>
</p>

<p align="center">
  如有问题或建议，欢迎 <a href="https://github.com/yourcompany/shen-gong/issues">提交 Issue</a>
</p>
