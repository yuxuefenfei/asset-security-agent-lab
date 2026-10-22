# 资产安全 Agent 实验室

Java / Spring AI / MCP 与 Vue / TypeScript 的教学项目。通过「资产 → 分析 → 证据 → 工单草稿 → 人工审批」学习只读工具、关键词检索、确定性评分与持久化。

本文按 2026-10-22 的代码和配置更新。资产、漏洞及知识均为虚构训练数据，不执行真实扫描；生产身份认证、向量 RAG、外部工单和真实模型质量评测尚未实现。

## 工程与运行边界

| 工程                            | 默认地址                           | 当前职责                                                             |
| ------------------------------- | ---------------------------------- | -------------------------------------------------------------------- |
| `lab-contract`                  | 无独立服务                         | Java record、有限状态、容量与协议契约                                |
| `offline-lab`                   | `http://127.0.0.1:8080`            | Spring MVC、MyBatis-Flex、H2 / Flyway；分析报告、工单与 Vue 静态资源 |
| `frontend`                      | `pnpm dev` 输出地址                | Vue 3 / TypeScript / Vite；开发时代理核心 API，发布时随核心 JAR 提供 |
| `spring-ai-workshop/agent-app`  | `http://127.0.0.1:8083/ai/analyze` | Spring AI OpenAI 兼容模型 + local 或 MCP 只读工具                    |
| `spring-ai-workshop/mcp-server` | `http://127.0.0.1:8082/mcp`        | Streamable HTTP 工具适配，转发核心 REST API                          |

Spring Agent 只返回模型答复，不计算核心结构化评分、不保存报告或创建工单。工作台调用 `/api/*`，并不调用 `/ai/analyze`。三个 Java 服务均默认绑定回环地址。

## 构建与启动

构建要求：**JDK 21、Maven 3.9+、Node ^22.18.0 或 >=24.12.0、pnpm 10.32.1**。版本依据为 [pom.xml](pom.xml) 和 [package.json](package.json)，不代表当前最新版本。运行已构建 JAR 只需 Java 21。

从仓库根目录执行。Windows PowerShell：

```powershell
# 替换为本机实际安装路径；仅影响当前终端
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
mvn -version
node --version
pnpm --version
./scripts/test-offline.ps1
./scripts/run-offline.ps1
```

Linux / macOS / Git Bash：先让 `JAVA_HOME` 和 `PATH` 指向 JDK 21，再执行：

```bash
bash ./scripts/test-offline.sh
bash ./scripts/run-offline.sh
```

测试脚本只运行共享契约与核心的 `test` 阶段；运行脚本先 `package -DskipTests`，再启动核心。两者都会经过前端资源构建，不等同于全工程验证。完整构建：

```bash
mvn verify
java -jar offline-lab/target/offline-lab.jar
```

Maven 的 `generate-resources` 自动冻结安装 pnpm 依赖、校验 Java / TS 契约、类型检查并构建前端。首次构建需要联网；`mvn -o` 仅限制 Maven 下载，不能保证 Node 依赖也已缓存。`mvn clean verify` 适用于已停止相关运行 JAR 的环境，Windows 上运行中的 JAR 可能阻止清理或重新打包。

打开 `http://127.0.0.1:8080/#/`。默认实验口令为 `learn-agent-local`，审批码为 `local-approval-only`。通过 `LAB_PORT`、`LAB_API_TOKEN`、`LAB_APPROVAL_CODE` 可修改核心设置；口令变更后在「实验设置」同步更新当前标签页。

## 数据与操作流程

初始化后合计 **38 项资产、100 条漏洞、107 条端口记录、16 份共享知识资料**；默认 `tenant-blue` 可见 **32 项资产、89 条漏洞**，另一个租户用于隔离测试。

| 练习资产       | 场景                                           |
| -------------- | ---------------------------------------------- |
| `192.0.2.10`   | 原有 Web 示例，2 条漏洞，默认评分 100 / HIGH   |
| `192.0.2.20`   | 原有数据库示例，1 条漏洞，默认评分 59 / MEDIUM |
| `192.0.2.30`   | 新增网关，4 条漏洞和对应知识证据               |
| `192.0.2.38`   | 无漏洞、非公网、LOW 重要性，默认评分 0 / LOW   |
| `198.51.100.9` | 其他租户，按 IP 查询返回 403                   |

工作台包含资产中心、安全分析、报告、工单、知识库和实验设置。选择授权资产，先用「规则分析」生成报告，核查漏洞、知识和轨迹，再主动创建草稿；编辑保存后输入审批码审批。审批后内容锁定，过期版本返回 409。

默认 H2 文件库为 `jdbc:h2:file:./data/asset-security`，报告和工单重启保留。Flyway V1 建表、V2 初始数据、V3 追加训练集；已有文件库自动增量升级，不需要删库。运行脚本会切到仓库根；IDEA 或直接运行 JAR 时也应以仓库根为工作目录。详见[模拟数据集](docs/12-模拟数据集.md)。

## 前端开发

先启动核心，再从仓库根目录另开终端：

```bash
pnpm install --frozen-lockfile
pnpm dev
```

Vite 开发代理默认指向 `http://127.0.0.1:8080`。自定义核心地址要在启动 Vite 的终端设置进程环境变量，例如 PowerShell：

```powershell
$env:LAB_DEV_API_URL = 'http://127.0.0.1:18080'
pnpm dev
```

`frontend/.env.example` 说明浏览器公开配置；`VITE_*` 在构建时注入，不能保存密钥。界面、Hash 路由和工程边界见[前端工作台](docs/11-前端工作台.md)，视觉规范见 [design.md](design.md)。

```bash
pnpm contracts:check
pnpm typecheck
pnpm format:check
pnpm build
```

## 核心 API 与手写模型模式

所有核心 API 包括健康检查都需要实验口令：

```bash
curl -H 'X-Lab-Token: learn-agent-local' http://127.0.0.1:8080/api/health
curl -H 'X-Lab-Token: learn-agent-local' 'http://127.0.0.1:8080/api/assets/page?page=1&size=10'
curl -X POST http://127.0.0.1:8080/api/analyze \
  -H 'Content-Type: application/json' -H 'X-Lab-Token: learn-agent-local' \
  -d '{"ip":"192.0.2.10","mode":"demo","question":"分析风险并引用证据"}'
```

成功包装为 `{"success":true,"data":...}`。完整接口、筛选参数、审批示例和错误语义见 [API 文档](docs/03-API与运行示例.md)。

核心的 `mode=demo` 不调用模型。`mode=llm` 由 JDK HttpClient 实现 Chat Completions 工具循环，配置项是 `LLM_API_URL`（**完整** `/v1/chat/completions` 地址）、`LLM_MODEL`、可选 `LLM_API_KEY`；默认地址是本机 11434，默认模型字符串是 `qwen2.5:7b`。

**当前已知限制**：核心 `ToolSchemas.definition()` 返回的 `parameters` 实际为参数名数组，而非完整 JSON Schema；假模型测试没有检查该 Schema 的合法性。因此不能据现有测试宣称核心 `llm` 已与真实模型联通。具体观测和待验项目见[核验记录](docs/09-核验记录.md)。

## Spring AI：local 与 MCP

Agent 使用 `spring-ai-starter-model-openai`。必须设置 `LLM_BASE_URL`（服务根地址，当前配置追加 `/v1/chat/completions`）、`LLM_MODEL`、`LLM_API_KEY`；后端不会自动读取前端的 `.env`。原文档中的 `OLLAMA_BASE_URL` / `OLLAMA_MODEL` 已不被当前代码使用。

本机 Ollama 仅作为一种 OpenAI 兼容服务示例。先安装并运行服务，确保所选模型已拉取且支持工具调用；然后在 Agent 的终端配置：

```powershell
$env:LLM_BASE_URL = 'http://127.0.0.1:11434'
$env:LLM_MODEL = 'qwen2.5:7b'
$env:LLM_API_KEY = 'ollama'
java -jar spring-ai-workshop/agent-app/target/agent-app-1.0.0.jar --lab.tool-backend=local
```

上面占位 key 仅适用于无需真实密钥的本机 Ollama；其 OpenAI 兼容接口说明见 [Ollama 文档](https://docs.ollama.com/api/openai-compatibility)。使用其他服务时配置真实服务根地址、模型及密钥，并按服务要求调整 completions-path。

**必须显式选择模式**：当前无 profile 的配置为 `tool-backend: mcp`，但 MCP Client 是关闭的；裸启动不会自动选择 local，分析请求会因无工具提供方失败。local 使用上面的参数。MCP 模式在核心启动后依次运行：

```powershell
# 终端 A：不需要模型配置
java -jar spring-ai-workshop/mcp-server/target/mcp-server-1.0.0.jar

# 终端 B：先设置上述三个 LLM 环境变量
java -jar spring-ai-workshop/agent-app/target/agent-app-1.0.0.jar --spring.profiles.active=mcp
```

`mcp` profile 位于 Agent 的同一个 `application.yml` 后半段，启用同步 Client 并连接 8082 的 `/mcp`。两个模式都请求：

```bash
curl -X POST http://127.0.0.1:8083/ai/analyze \
  -H 'Content-Type: application/json' \
  -d '{"ip":"192.0.2.10","question":"查询漏洞并引用知识资料"}'
```

local 是 `ChatClient → @Tool → 核心 REST`；MCP 是 `ChatClient → ToolCallbacks → MCP Server → 核心 REST`。Agent / MCP 默认只允许 `192.0.2.10`，修改地址或口令时同步设置各服务的 `lab.core-url`、`lab.api-token`、`lab.allowed-ip`。示例没有生产 IAM。

## 验证与文档导航

2026-10-22：根工程 `mvn verify` 包括前端构建、Spotless、28 项 Java 测试；另有 9 项浏览器回归。具体运行结果、环境和未覆盖项以[核验记录](docs/09-核验记录.md)为准。

浏览器测试使用已打包 JAR、隔离内存库及随机本机端口：

```bash
pnpm exec playwright install chromium
pnpm test:ui
```

| 文档                                                      | 内容                           |
| --------------------------------------------------------- | ------------------------------ |
| [01 学习地图](docs/01-学习地图.md)                        | 基于当前实现的四周练习         |
| [02 架构与执行链路](docs/02-架构与执行链路.md)            | 前后端、模型、数据和审批边界   |
| [03 API 与运行示例](docs/03-API与运行示例.md)             | 全部核心 API、配置和审批       |
| [04 Tool Calling 与 MCP](docs/04-ToolCalling与MCP讲解.md) | 工具定义、协议与迁移练习       |
| [05 RAG 设计与练习](docs/05-RAG设计与练习.md)             | 当前关键词检索与后续向量方案   |
| [06 安全与测试](docs/06-安全与测试.md)                    | 实际约束、预算及回归范围       |
| [07 升级路线](docs/07-升级路线.md)                        | 尚未实现的生产化工作和故障排查 |
| [08 面试与简历](docs/08-面试与简历.md)                    | 可复现演示与真实能力描述       |
| [09 核验记录](docs/09-核验记录.md)                        | 当前证据、历史与限制           |
| [10 工程化重构](docs/10-工程化重构.md)                    | 包职责、数据库和事务           |
| [11 前端工作台](docs/11-前端工作台.md)                    | 页面、构建、配置与交互         |
| [12 模拟数据集](docs/12-模拟数据集.md)                    | 租户数量、练习资产和增量迁移   |
