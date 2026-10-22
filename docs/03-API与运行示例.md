# 第 3 课：API、配置与审批示例

核对日期：2026-10-22。核心默认 `http://127.0.0.1:8080`；以下 curl 示例使用 Bash 语法，Windows PowerShell 应使用 `curl.exe` 并按终端规则传 JSON，或使用 `Invoke-RestMethod`。

## 核心接口清单

所有 `/api/*` 包括 health 都需要 `X-Lab-Token: learn-agent-local`。首页、favicon 和明确放行的编译静态资源可匿名读取。

| 方法 | 路径                   | 参数 / 用途                                               |
| ---- | ---------------------- | --------------------------------------------------------- |
| GET  | `/api/health`          | 核心可访问状态，不代表模型可用                            |
| GET  | `/api/dashboard`       | 当前租户统计与共享知识数量                                |
| GET  | `/api/assets`          | 可选 `ip`；单资产或兼容数组                               |
| GET  | `/api/assets/page`     | `page,size,q,criticality,exposed`                         |
| GET  | `/api/vulnerabilities` | 必填 `ip`；校验资产租户后返回扫描记录                     |
| GET  | `/api/knowledge`       | `q` 加权检索，空查询返回空数组，默认最多 5 条             |
| GET  | `/api/knowledge/page`  | `page,size,q`；空查询为完整目录当前页                     |
| GET  | `/api/knowledge/{id}`  | 当前知识正文、关键词及来源                                |
| POST | `/api/analyze`         | `ip`；可选 `question,mode`，默认 demo                     |
| GET  | `/api/analyses`        | 兼容报告数组，默认最多 100 条                             |
| GET  | `/api/analyses/page`   | `page,size,q,riskLevel,mode`，报告摘要                    |
| GET  | `/api/analyses/{id}`   | 分析时的完整快照                                          |
| POST | `/api/tickets/draft`   | `analysisId`；同一分析返回同一工单                        |
| PUT  | `/api/tickets/{id}`    | `title,remediation,note,version`；完整替换草稿可编辑内容  |
| POST | `/api/tickets/approve` | `ticketId,approvalCode,version`；审批并锁定               |
| GET  | `/api/tickets`         | 兼容工单数组，默认最多 100 条                             |
| GET  | `/api/tickets/page`    | `page,size,q,status,riskLevel,analysisId`                 |
| GET  | `/api/tickets/{id}`    | 完整内容与当前版本                                        |
| GET  | `/api/tools`           | 3 项核心工具定义；当前 parameters 的 Schema 缺陷见第 4 课 |

`/api/dashboard` 返回 `assetCount,exposedAssetCount,reportCount,highRiskReportCount,pendingTicketCount,approvedTicketCount,knowledgeCount`。高风险数统计 HIGH 报告，待审批统计 DRAFT 工单；这些不是实时漏洞风险聚合。

## 查询与响应契约

- API 分页默认 `page=1,size=20`，最大 size 默认 100；Vue 通用列表默认每页 10 条，资产选择器每组 20 条。
- data 含 `items,page,size,total`。资产和知识按 ID 升序；报告和工单按创建时间降序、ID 升序。未开放任意 sort 参数。
- 资产 q 匹配 IP、主机、业务、负责人；报告 q 匹配 IP / 编号；工单 q 匹配标题 / IP / 编号；知识目录 q 匹配标题 / 关键词 / 正文。
- 目录搜索 q 最长默认 200 个 Java 字符，转义 %、_、反斜杠后按字面子串匹配。原知识工具检索是另一条加权检索路径。
- 查询参数枚举使用 Java 名称：`criticality/riskLevel=HIGH|MEDIUM|LOW`、`mode=DEMO|LLM`、`status=DRAFT|APPROVED`。分析 POST 与 JSON 响应模式是小写 `demo|llm`。`exposed=true|false`。
- 成功为 `{"success":true,"data":...}`；错误为 `{"success":false,"error":"说明","code":"错误码","traceId":"编号"}`，HTTP 状态仍区分失败。
- 时间为 ISO-8601 UTC，前端用 Asia/Shanghai 展示；漏洞 lastSeen 为日期字符串。
- JSON POST / PUT 使用 application/json，默认最多 16 KiB，拒绝非法类型与额外字段。问题最长 1000、标题 200、整改建议 4000、备注 2000；审批 / 编辑必须提供非负整数 version。
- 原资产、分析、工单数组接口受 legacy-list-limit 限制，完整浏览请用 page 接口。

列表中跨租户记录被过滤；直接查询其他租户资产是 403。缺失资产、报告、工单或知识详情当前按参数 / 业务错误返回 400。认证通过后，未知路径与不支持的方法返回 404；没有有效口令时 Filter 会先返回 403。

## 从分析到审批

```bash
# A：复制返回的 data.id
curl -s -X POST http://127.0.0.1:8080/api/analyze \
  -H 'X-Lab-Token: learn-agent-local' -H 'Content-Type: application/json' \
  -d '{"ip":"192.0.2.10","mode":"demo","question":"查询模拟漏洞及修复建议"}'

# B：用 A 的真实 ID；复制 data.id 和 data.version
curl -s -X POST http://127.0.0.1:8080/api/tickets/draft \
  -H 'X-Lab-Token: learn-agent-local' -H 'Content-Type: application/json' \
  -d '{"analysisId":"<分析 ID>"}'

# C：用 B 的真实工单 ID 和版本；成功后取得新版本
curl -s -X PUT 'http://127.0.0.1:8080/api/tickets/<工单 ID>' \
  -H 'X-Lab-Token: learn-agent-local' -H 'Content-Type: application/json' \
  -d '{"title":"人工审核标题","remediation":"确认维护窗口并在修复后复测","note":"已核对证据","version":0}'

# D：以下 version=1 仅对应新草稿完成一次编辑的情形
curl -s -X POST http://127.0.0.1:8080/api/tickets/approve \
  -H 'X-Lab-Token: learn-agent-local' -H 'Content-Type: application/json' \
  -d '{"ticketId":"<工单 ID>","approvalCode":"local-approval-only","version":1}'
```

必须使用真实响应中的 version。`note:null` 清空备注；标题和整改建议不能空。审批后为 APPROVED，内容只读；重复创建草稿返回原记录。过期版本 409，已审批当前版本重复处理 400，错误审批码 403。

PowerShell 可用对象自动生成 JSON：

```powershell
$headers = @{'X-Lab-Token'='learn-agent-local'}
$body = @{ip='192.0.2.30'; mode='demo'; question='核查模拟风险与知识证据'} | ConvertTo-Json
$report = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/analyze' -Method Post -Headers $headers -ContentType 'application/json; charset=utf-8' -Body $body
$report.data.id
```

## 配置项与预算

| 范围              | 环境变量 / Spring 属性                     | 默认 / 行为                                          |
| ----------------- | ------------------------------------------ | ---------------------------------------------------- |
| 核心 HTTP         | LAB_PORT                                   | 8080，地址为 127.0.0.1                               |
| 核心鉴权          | LAB_API_TOKEN / LAB_APPROVAL_CODE          | learn-agent-local / local-approval-only              |
| H2                | LAB_DB_URL / LAB_DB_USER / LAB_DB_PASSWORD | jdbc:h2:file:./data/asset-security / sa / 空         |
| 固定租户 / 审批人 | lab.security.tenant-id / reviewer          | tenant-blue / local-lab-reviewer                     |
| 核心模型          | LLM_API_URL / LLM_MODEL / LLM_API_KEY      | 完整 Chat Completions URL / qwen2.5:7b / 可空        |
| Agent 模型        | LLM_BASE_URL / LLM_MODEL / LLM_API_KEY     | 无默认值；根 URL 由当前配置追加 /v1/chat/completions |
| Spring 网关       | lab.core-url / api-token / allowed-ip      | 8080 核心 / 实验口令 / 192.0.2.10                    |
| Spring 网关超时   | lab.connect-timeout / request-timeout      | 3s / 10s                                             |
| MCP Client 请求   | spring.ai.mcp.client.request-timeout       | mcp profile 中 20s                                   |

核心模型默认连接超时 8s、单次请求含下载 75s、模型循环总时限 120s、最多 6 轮和 24 次模型请求工具调用、单响应 1 MiB、最多 4 个并发分析。模型阶段预算不包含此前 Java 预查询和之后数据库保存，不等于整个 HTTP 请求硬时限；24 次也不含 Java 预查询轨迹。这些上限没有自动施加到独立 Spring Agent。

核心模型 / 工具失败映射 502，不保存成功报告；并发容量耗尽 429；内部错误 500。错误不透传原始上游响应。当前核心工具 Schema 限制见[第 4 课](04-ToolCalling与MCP讲解.md)。

## 报告与评分

报告包含 `id,time,mode,asset,vulnerabilities,riskScore,riskLevel,knowledgeEvidence,summary,warning,toolTrace,status`。知识证据含正文与 `training://` 来源标识，这不是外部公告 URL。

默认评分：`min(100, round(最高 cvss × 10) + 公网暴露加分 10 + HIGH 重要性加分 5)`，加分项只在条件满足时生效；阈值 80 / 50 对应 HIGH / MEDIUM。没有漏洞时最高项为 0，但资产加分仍可能非零；RESOLVED 记录也参与最高项计算。

`192.0.2.10` 为 100 / HIGH，`192.0.2.20` 为 59 / MEDIUM。评分只供教学，不是实际 CVSS / EPSS 风险评估。

`toolTrace.origin` 区分固定 `java-orchestrator` 和模型请求 `llm-request`；只有成功调用进入轨迹。`mode=llm` 或预查询轨迹不证明模型主动使用过工具。

## Spring Agent 接口

`POST http://127.0.0.1:8083/ai/analyze` 接收非空字符串 ip、question，问题最长 1000；不接收核心 mode。工具来源由显式 local 参数或 mcp profile 决定。启动步骤见 [README](../README.md)。

```json
{
  "backend": "local",
  "assetIp": "192.0.2.10",
  "answer": "模型生成的答复",
  "warning": "模拟数据，内容必须人工复核"
}
```

无 success/data 包装，不保存核心业务结果。默认 IP 白名单在模型调用前检查。参数错误 400、越权 403、已映射模型 / 工具服务错误 502；错误体为 `{"error":"说明","code":"错误码","traceId":"编号"}`。接口没有生产身份认证，也不要求核心的 X-Lab-Token；网关访问核心时才添加实验口令。
