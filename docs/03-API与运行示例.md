# 第 3 课：API 运行与审批全链路

核心默认监听 `http://127.0.0.1:8080`。首页与列出的静态资源无需口令，API 请求头必须带 `X-Lab-Token: learn-agent-local`
；默认值可通过 `LAB_API_TOKEN` 修改。

## 1. 核心 API 清单

| 方法 | 路径                   | 参数                             | 说明                               |
|------|------------------------|----------------------------------|------------------------------------|
| GET  | `/`                    | 无                               | 教学工作台                         |
| GET  | `/api/health`          | 无                               | 需要实验口令的健康检查             |
| GET  | `/api/assets`          | 可选 `ip`                        | 单个授权资产或兼容数组列表         |
| GET  | `/api/assets/page`     | `page=1,size=20`                 | 数据库分页                         |
| GET  | `/api/vulnerabilities` | `ip`                             | 先校验资产租户再查询扫描记录       |
| GET  | `/api/knowledge`       | `q`，最长 200 字                 | 空白查询返回空数组，数据库加权排序 |
| POST | `/api/analyze`         | `ip`，可选 `question,mode`       | 模式 demo/llm；问题最长 1000 字    |
| GET  | `/api/analyses`        | 无                               | 兼容报告数组，最多默认 100 条      |
| GET  | `/api/analyses/page`   | `page,size`                      | 报告摘要与分页元数据               |
| GET  | `/api/analyses/{id}`   | 报告 ID                          | 分析时的不可变快照                 |
| POST | `/api/tickets/draft`   | `analysisId`                     | 同一分析唯一草稿，重试返回已有记录 |
| PUT  | `/api/tickets/{id}`    | `title,remediation,note,version` | 编辑未审批草稿，成功后版本递增     |
| POST | `/api/tickets/approve` | `ticketId,approvalCode,version`  | 审批当前已审核版本并锁定内容       |
| GET  | `/api/tickets`         | 无                               | 兼容工单数组，最多默认 100 条      |
| GET  | `/api/tickets/page`    | `page,size`                      | 数据库分页与稳定排序               |
| GET  | `/api/tickets/{id}`    | 工单 ID                          | 获取完整内容与当前版本             |
| GET  | `/api/tools`           | 无                               | 只读工具定义                       |

成功响应保留 `{success:true,data:...}`。错误响应为 `{success:false,error:"说明",code:"错误码",traceId:"请求编号"}`。JSON
POST/PUT 使用 `Content-Type: application/json`，默认最多 16 KiB；非法类型、额外字段或越界参数返回 400。未知路径及不支持的方法返回
404。

页码从 1 开始，最大页大小默认 100。分页响应的 data 含 `items,page,size,total`，排序有稳定 ID 作为相同时间的次序键。旧数组接口最多返回
`lab.policy.legacy-list-limit` 条，完整历史请使用分页接口。

报告、工单和健康时间保留 ISO-8601 UTC 格式。开发使用 H2 文件库，数据重启后保留；测试库与开发库隔离。

## 2. 从分析到审批

```bash
# A：创建分析，复制 data.id
curl -s -X POST http://127.0.0.1:8080/api/analyze \
  -H 'X-Lab-Token: learn-agent-local' -H 'Content-Type: application/json' \
  -d '{"ip":"192.0.2.10","mode":"demo","question":"查询模拟漏洞及修复建议"}'

# B：生成草稿，复制返回的 data.id 和 data.version
curl -s -X POST http://127.0.0.1:8080/api/tickets/draft \
  -H 'X-Lab-Token: learn-agent-local' -H 'Content-Type: application/json' \
  -d '{"analysisId":"<分析 data.id>"}'

# C：可选编辑；使用 B 返回的当前 version，成功后复制新的 version
curl -s -X PUT 'http://127.0.0.1:8080/api/tickets/<工单 data.id>' \
  -H 'X-Lab-Token: learn-agent-local' -H 'Content-Type: application/json' \
  -d '{"title":"人工审核的标题","remediation":"确认维护窗口，修复后复测","note":"已审核证据","version":0}'

# D：审核最新内容后审批；示例 version=1 对应刚完成一次编辑
curl -s -X POST http://127.0.0.1:8080/api/tickets/approve \
  -H 'X-Lab-Token: learn-agent-local' -H 'Content-Type: application/json' \
  -d '{"ticketId":"<工单 data.id>","approvalCode":"local-approval-only","version":1}'

curl -s -H 'X-Lab-Token: learn-agent-local' 'http://127.0.0.1:8080/api/tickets/page?page=1&size=20'
```

必须使用实际响应中的版本号；不编辑的新草稿通常为 0。`note:null` 表示清空备注，其余内容字段为完整替换。审批后为 `APPROVED`
，标题、整改建议和备注不可再修改。再次生成同一分析的草稿返回同一工单。

**确认的兼容例外**：审批请求现在必须带 `version`，缺少版本返回 400，过期版本返回 409；已审批工单以当前版本再次审批返回
400。审批码通过 `LAB_APPROVAL_CODE` 配置，操作者默认 `local-lab-reviewer`，仅为模拟标识，不代表真实身份认证。

## 3. 故意触发失败

```bash
# 无口令：403
curl -i http://127.0.0.1:8080/api/assets
# 跨租户：403
curl -i 'http://127.0.0.1:8080/api/assets?ip=198.51.100.9' -H 'X-Lab-Token: learn-agent-local'
# 不存在的资产：400
curl -i 'http://127.0.0.1:8080/api/assets?ip=192.0.2.123' -H 'X-Lab-Token: learn-agent-local'
# 缺少审批版本：400
curl -i -X POST http://127.0.0.1:8080/api/tickets/approve \
  -H 'X-Lab-Token: learn-agent-local' -H 'Content-Type: application/json' \
  -d '{"ticketId":"<工单 data.id>","approvalCode":"local-approval-only"}'
```

模型连接失败、上游非成功状态、格式错误、空 choices、调用轮数/工具次数/响应容量/总时间超限统一返回
502，不保存成功报告，不透传上游原始内容。并发分析容量耗尽返回 429。内部未预期错误返回 500，响应中保留 traceId 供日志定位。

## 4. 报告与工具轨迹

下面只展示 `data` 中的一部分字段；完整响应还包含 `time`、`summary`、`warning`、`status` 等。

```json
{
  "id": "随机 UUID",
  "mode": "demo",
  "riskLevel": "HIGH",
  "riskScore": 100,
  "asset": {"ip": "192.0.2.10", "hostname": "web-prod-01"},
  "toolTrace": [
    {
      "step": 1,
      "origin": "java-orchestrator",
      "tool": "get_asset",
      "arguments": {"ip": "192.0.2.10"},
      "resultCount": 1
    }
  ]
}
```

`vulnerabilities` 是漏洞对象数组，`knowledgeEvidence` 是带 `id/title/body/source` 等字段的知识对象数组。`training://...` 是虚构来源标识。

离线评分公式为：`min(100, round(最高模拟 cvss × 10) + 公网暴露加分 10 + HIGH 重要性加分 5)`；两个加分项仅在对应条件满足时生效。`>=80` 为 HIGH，`>=50` 为 MEDIUM，其余为 LOW。默认 Web 为 100/HIGH，测试数据库为 59/MEDIUM；这不是 CVSS 标准的资产评分方法。

`origin=java-orchestrator` 记录固定预查询，`origin=llm-request` 才表示执行了模型提出的工具请求。轨迹只记录成功执行，不记录失败调用或最终模型消息。模型可能没有请求任何工具，所以 `mode=llm` 本身不足以证明模型使用了工具。

## 5. Spring AI 接口

`POST http://127.0.0.1:8083/ai/analyze` 的请求包含非空 `ip`、`question`，问题最多 1000 个 Java 字符。它不接受离线版 `mode` 参数，工具来源由 local / mcp 启动配置决定。

```json
{
  "backend": "local",
  "assetIp": "192.0.2.10",
  "answer": "模型生成的中文答复",
  "warning": "模拟数据，内容必须人工复核"
}
```

此接口没有 `success/data` 外层，不保存报告、评分或工单。默认目标白名单仅允许 `192.0.2.10`，越权请求在联系模型前返回
403；参数不合法返回 400，已映射的模型/工具服务异常返回 502，错误体为 `{"error":"说明","code":"错误码","traceId":"请求编号"}`
。Spring 示例没有正式身份认证，也不要求 `X-Lab-Token`；工具网关向核心 API 转发时才添加配置的实验口令。

## 6. 断点调试建议

- `AnalysisService.analyze()`：观察模型调用前已计算的评分和证据。
- `ModelClient.reason()`：观察每轮 `messages` 和 `tool_calls`。
- `ToolSession.call()`：观察工具白名单、参数和资产范围校验。
- `toolTrace.origin`：区分固定编排与模型请求。
- `report.summary`：辅助解释，不是可靠的独立安全证据。
