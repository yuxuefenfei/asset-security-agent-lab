# AI Agent 智能资产安全运营平台 —— Java 从 0 到 1 教学源码

> 适用人群：会写 Java / Spring Boot，但第一次系统学习 AI Agent 的后端工程师。  
> 目标：先理解 **Tool Calling**，再理解 **RAG 检索**和 **人工审批闭环**，最后连接 **Spring AI + Ollama + MCP Server**。  
> **重要**：项目是教学实验，包含虚构漏洞与资产。不得作为真实漏洞扫描、风险评分、IAM 或生产安全控制的依据。

## 0. 你将拿到什么？

| 工程                            | 启动环境                                      | 是否依赖模型/外网                     | 学习内容                                                             | 验证状态                                                          |
|---------------------------------|-----------------------------------------------|---------------------------------------|----------------------------------------------------------------------|-------------------------------------------------------------------|
| `offline-lab/`                  | JDK 21 + Maven 3.9+                           | 首次构建需下载依赖；运行无需模型/外网 | API、业务数据、Tool Dispatcher、知识检索、报告、工单和审批           | ✅ 本环境编译、自测和 HTTP 验证                                   |
| `offline-lab` 的 `llm` 模式     | JDK 21 + Maven + 支持 Tool Calling 的模型服务 | 本地 Ollama 或兼容 API                | **真正模型 Tool Calling**：模型发出工具请求，Java 执行，结果返回模型 | ✅ fake LLM 两轮协议、HTTP 错误路径已验证；真实模型效果另行实测   |
| `spring-ai-workshop/agent-app`  | JDK 21 + Maven + Ollama                       | 需要模型和 Maven 依赖                 | Spring AI `ChatClient`、`@Tool`、请求级工具范围校验                  | ✅ 构建、5 项回归及 local/MCP fake 模型调用链通过；真实模型未评测 |
| `spring-ai-workshop/mcp-server` | JDK 21 + Maven                                | 需要 Maven 依赖                       | 通过 Spring AI MCP Starter 暴露 `tools/list`/`tools/call`            | ✅ 构建及 3 项 MCP HTTP 初始化、发现、调用和越权集成测试通过      |

> 为什么先使用嵌入式 H2，暂不引入 MySQL、Redis、Docker 和向量库？为了把第一次学习的注意力集中在 Agent 机制与持久化流程。基础跑通后，在
> `docs/07-升级路线.md` 中按步骤替换为真正企业级组件。

## 1. 5 分钟跑通离线版

**要求：JDK 21 + Maven 3.9+**，`JAVA_HOME` 应指向 JDK 21。Linux / macOS / Windows 的 Git Bash / WSL：

```bash
mvn -version
bash ./scripts/test-offline.sh
bash ./scripts/run-offline.sh
```

启动后打开： **http://127.0.0.1:8080**。

默认资产：

- `192.0.2.10`：模拟生产 Web，2 条虚构漏洞，示例高风险；
- `192.0.2.20`：模拟测试数据库，1 条虚构漏洞；
- `198.51.100.9`： **另一租户资产**，用于验证越权查询会被拒绝。

前端点击「开始安全分析」→ 查看风险与证据 →「生成工单草稿」→ 预览并编辑标题、整改建议和备注 → 保存修改 → 输入审批码
`local-approval-only` →「人工审批当前草稿」。审批后内容锁定，历史记录可重新打开。

> 如果要换 HTTP 端口：`LAB_PORT=18080 bash ./scripts/run-offline.sh`。端口默认仅监听 `127.0.0.1`。前端实验口令默认
> `learn-agent-local`；如自行修改 `LAB_API_TOKEN`，请在页面口令输入框里同步修改。开发数据默认保存在根目录 `data/`，重启保留。

Windows PowerShell：先将当前终端的 `JAVA_HOME` 指向 JDK 21：

```powershell
# 仅修改当前终端；替换为实际 JDK 21 路径
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn -version
./scripts/test-offline.ps1
./scripts/run-offline.ps1
```

也可以从仓库根目录直接用 Maven 构建，再运行可执行 JAR：

```bash
mvn clean verify
java -jar offline-lab/target/offline-lab.jar
```

首次构建需要下载 Maven 插件、应用和测试依赖；依赖缓存后可使用 `mvn -o clean verify` 离线构建。运行时依赖已包含在可执行 JAR
中，只需 JDK，无需额外下载；规则模式不需要模型或外网。

IDEA 中右键根目录 `pom.xml` → **Add as Maven Project**，将项目 SDK 和 Maven 的运行 JDK 设置为 21，将运行配置的工作目录设为仓库根目录，再运行
`lab.LabServer.main()`。Maven 导入会自动识别 `src/main/resources` 并复制首页资源；浏览器访问
`http://127.0.0.1:8080/`，不要直接访问 `/index.html`。API 仍需正确的 `X-Lab-Token`。

## 2. 核心 HTTP API

```bash
# 健康检查
curl -H 'X-Lab-Token: learn-agent-local' http://127.0.0.1:8080/api/health

# 读取授权资产
curl -H 'X-Lab-Token: learn-agent-local' 'http://127.0.0.1:8080/api/assets?ip=192.0.2.10'

# 离线模拟分析
curl -X POST 'http://127.0.0.1:8080/api/analyze' \
  -H 'Content-Type: application/json' -H 'X-Lab-Token: learn-agent-local' \
  -d '{"ip":"192.0.2.10","mode":"demo","question":"分析风险，列出证据"}'
```

响应外层为 `{"success": true, "data": ...}`，其中 `data.id` 为 `analysisId`。完整工单接口请看 `docs/03-API与运行示例.md`。

## 3. 真的使用大模型的 Tool Calling（不依赖 Spring AI）

离线版除了 `demo`，还支持 `mode=llm`。这是使用 JDK `HttpClient` 手动实现的 **OpenAI Chat Completions 兼容工具调用循环**
，目的就是让你看到框架替你隐藏了什么。

先启动支持工具调用的 Ollama 模型（模型名称及内存要求由你的环境决定）：

```bash
ollama pull qwen2.5:7b
ollama serve
```

另一个终端运行实验平台（大模型接口 **不是**必须在启动前可用）：

```bash
export LLM_API_URL=http://127.0.0.1:11434/v1/chat/completions
export LLM_MODEL=qwen2.5:7b
bash ./scripts/run-offline.sh
```

在浏览器模式选择 `llm`，或使用：

```bash
curl -X POST 'http://127.0.0.1:8080/api/analyze' \
  -H 'Content-Type: application/json' -H 'X-Lab-Token: learn-agent-local' \
  -d '{"ip":"192.0.2.10","mode":"llm","question":"请查询资产、漏洞、知识库，然后解释风险"}'
```

可选使用其他支持 OpenAI Chat Completions `tools`/`tool_calls` 的服务：配置 `LLM_API_URL`、`LLM_MODEL`、`LLM_API_KEY`
。该模式会把模拟训练数据与用户问题发送给配置的模型服务，因此不要拿它处理真实的敏感资产数据。

**与真实 Agent 的关系**：

- `ModelClient.java` 向模型发送 `tools` 定义，并执行模型返回的 `tool_calls`，不是仅输出固定的工具调用文字。
- `ToolSession.java` 强制限制可调用工具、绑定当前 IP 和租户，禁止写入业务数据。
- `AnalysisService.java` 中的评分是确定性的 Java 教学规则；模型负责辅助解释，不具备修改评分和越权调用的权限。
- `toolTrace.origin` 区分 `java-orchestrator` 与 `llm-request`；前者是 Java 的固定预查询，只有后者才是模型请求执行的工具。模型可能不调用全部工具，也可能生成错误解释。因此需要审查 `toolTrace`、引文与输出，不能只相信自然语言回答。

## 4. Spring AI：从手动工具循环到框架封装

在 `offline-lab` 运行状态下，另开终端：

```bash
mvn -DskipTests package
cd spring-ai-workshop
java -jar agent-app/target/agent-app-1.0.0.jar
```

访问 `http://127.0.0.1:8083/ai/analyze`：

```bash
curl -X POST http://127.0.0.1:8083/ai/analyze \
  -H 'Content-Type: application/json' \
  -d '{"ip":"192.0.2.10","question":"调用工具分析虚构漏洞并引用资料"}'
```

要求 Ollama 已安装、模型已拉取且服务运行。可通过 `OLLAMA_BASE_URL`、`OLLAMA_MODEL` 配置。

> 初次 Maven 构建需联网下载依赖。Spring AI 版本固定为 **1.1.0**、Spring Boot **3.5.7**、Java **21**，避免混用 2.x 的
> API。依赖请从 Maven Central 或可信内部制品库下载。

### Spring AI 真正帮你省掉了什么？

`ModelClient.java` 手写了工具 Schema、模型请求、解析 `tool_calls`、执行 Java 方法、拼装 Tool 结果、再请求模型。Spring AI 的
`ChatClient` + `.tools(new RequestScopedAssetTools(...))` 负责完成这些框架级集成。MCP 的 `ToolCallbackProvider` 则通过 `.toolCallbacks(provider)` 传入，不能当作带 `@Tool` 的普通对象传给 `.tools(...)`。

但 **权限、租户隔离、工具白名单、审批** 仍是你自己的业务责任。

Spring 示例 `/ai/analyze` 返回 `backend`、`assetIp`、`answer`、`warning`，用于演示工具接入。它不调用离线版的 `/api/analyze`，不会保存分析报告、计算结构化评分或创建工单；完整报告和审批演练仍通过 8080 的离线核心服务完成。Spring Agent 与 MCP Server 默认均只允许 `192.0.2.10`。

## 5. MCP：把已稳定的工具复用给其他 Agent

先运行上述 `offline-lab`，再运行：

```bash
cd spring-ai-workshop
java -jar mcp-server/target/mcp-server-1.0.0.jar
```

服务监听 `127.0.0.1:8082`，本项目通过 `protocol: STREAMABLE` 启用 Streamable HTTP，端点默认为 `/mcp`。它只提供只读资产/漏洞/知识工具，并将查询转发到
8080 的既有 REST API， **并没有重写业务服务**。

然后使用 `mcp` 配置启动 Agent（需退出之前的 8083 Agent，避免端口冲突）：

```bash
mvn -DskipTests package
cd spring-ai-workshop
java -jar agent-app/target/agent-app-1.0.0.jar --spring.profiles.active=mcp
```

仍然调用 8083 的同一个 `/ai/analyze` 接口，就能比较：

```
本地模式：LLM -> Spring AI @Tool -> REST 网关 -> 离线核心服务
MCP  模式：LLM -> MCP Client -> MCP Server -> REST 网关 -> 离线核心服务
```

⚠️ **MCP 示例只监听本机且无可用于生产的 IAM**，不能暴露至互联网或公司网络。默认固定允许 `192.0.2.10`
；生产环境需要在每次工具执行中验证可靠的调用方身份和细粒度资产授权。MCP 的协议端点默认不自动实施业务级权限控制。

## 6. 阅读源码顺序（非常重要）

1. `lab-contract/src/main/java/lab/contract/`：record DTO、有限状态枚举和 API 契约。
2. `offline-lab/src/main/resources/db/migration/`：H2 表结构、约束和虚构初始化数据。
3. `offline-lab/src/main/java/lab/asset/`、`vulnerability/`、`knowledge/`：PO、Mapper、类型化查询与租户检查。
4. `offline-lab/src/main/java/lab/tool/application/`：工具 Schema 构建、严格参数、任务范围与轨迹。
5. `offline-lab/src/main/java/lab/analysis/`：分析编排、确定性评分与不可变报告快照。
6. `offline-lab/src/main/java/lab/model/`：类型化 OpenAI 工具协议、响应容量和调用预算。
7. `offline-lab/src/main/java/lab/ticket/`：草稿幂等、编辑、版本冲突与审批事务。
8. `offline-lab/src/main/resources/static/assets/`：API、报告、草稿编辑与历史记录脚本。
9. `spring-ai-workshop/`：按功能分包的 Spring AI 与 MCP 适配层。

## 7. 配套文档

| 文档                                                                 | 学习目标                                 |
|----------------------------------------------------------------------|------------------------------------------|
| [`docs/01-学习地图.md`](docs/01-学习地图.md)                         | 4 周可执行学习计划                       |
| [`docs/02-架构与执行链路.md`](docs/02-架构与执行链路.md)             | 每一层解决什么问题、完整时序             |
| [`docs/03-API与运行示例.md`](docs/03-API与运行示例.md)               | 全部 API + 错误路径 + 人工审批演练       |
| [`docs/04-ToolCalling与MCP讲解.md`](docs/04-ToolCalling与MCP讲解.md) | 两者区别、迁移策略、图解                 |
| [`docs/05-RAG设计与练习.md`](docs/05-RAG设计与练习.md)               | 当前检索实现与 pgvector 升级练习         |
| [`docs/06-安全与测试.md`](docs/06-安全与测试.md)                     | 测试清单、提示词注入、租户隔离、安全边界 |
| [`docs/07-升级路线.md`](docs/07-升级路线.md)                         | 生产化路线和技术取舍                     |
| [`docs/08-面试与简历.md`](docs/08-面试与简历.md)                     | 演示步骤、常见追问、真实履历写法         |
| [`docs/09-核验记录.md`](docs/09-核验记录.md)                         | 已核验结论、修正项、复现命令与验证范围   |

工程结构、数据库设计、审批版本兼容例外和浏览器测试见 [10-工程化重构.md](docs/10-工程化重构.md)。

## 8. 已知边界

- 资产、漏洞与知识由 Flyway 初始化为虚构训练数据，不发起扫描、不调用 Nmap。
- 默认使用 H2 文件库，报告快照、工单内容与模拟审批信息在重启后保留；未接入 MySQL、Redis、RabbitMQ。
- 检索目前是 **关键词检索与来源标识的基础练习**（`demo` 模式不生成模型回答），没有 Embedding、向量数据库或复杂重排。练习在文档中提供升级步骤。
- 风险打分为 **演示启发式规则**，并非真实 CVSS/EPSS 或资产风险计算标准。
- `X-Lab-Token` 和本地审批码不是企业 IAM；没有真实 RBAC/ABAC、持久审计、正式工单同步。
- 2026-10-13 已使用 JDK 21.0.4、Maven 3.9.6 完成根工程 clean verify：25 项 Java 测试通过，另有 4 项浏览器回归通过；真实
  Ollama 模型效果未评测。验证范围和复现命令见 `docs/09-核验记录.md`。
- 不包含 API 密钥，别在源码中硬编码密钥。

## 9. 参考官方资料

- [Spring AI Tool Calling](https://docs.spring.io/spring-ai/reference/1.1/api/tools.html)
- [Spring AI Ollama](https://docs.spring.io/spring-ai/reference/1.1/api/chat/ollama-chat.html)
- [Spring AI MCP Server](https://docs.spring.io/spring-ai/reference/1.1/api/mcp/mcp-server-boot-starter-docs.html)
- [Spring AI MCP Client](https://docs.spring.io/spring-ai/reference/1.1/api/mcp/mcp-client-boot-starter-docs.html)
- [Model Context Protocol 传输规范（2025-06-18）](https://modelcontextprotocol.io/specification/2025-06-18/basic/transports)
