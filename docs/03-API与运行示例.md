# 第 3 课：API 运行与审批全链路

离线核心默认监听 `http://127.0.0.1:8080`。除首页外，请求头必须带 `X-Lab-Token: learn-agent-local`；默认值可通过 `LAB_API_TOKEN` 修改。这是本地实验口令，不提供正式身份认证。

## 1. 离线 API 清单

| 方法 | 路径 | 参数与默认值 | 说明 |
|------|------|--------------|------|
| GET | `/` | 无 | 教学工作台，无需实验口令 |
| GET | `/api/health` | 无 | 进程探针，仍需实验口令 |
| GET | `/api/assets` | `ip` 可选 | 不传时返回固定租户资产；传入时校验租户 |
| GET | `/api/vulnerabilities` | `ip` 必选 | 授权资产的虚构扫描记录 |
| GET | `/api/knowledge` | `q` 可选，最多 200 字符 | 空查询返回 `[]`，最多返回 5 条共享教学资料 |
| GET | `/api/tools` | 无 | 3 个只读工具的 OpenAI 兼容 Schema |
| POST | `/api/analyze` | `ip` 必选；`mode=demo`；`question=分析资产安全风险` | `mode` 仅支持 demo / llm |
| GET | `/api/analyses` | 无 | 当前 JVM 中的全部报告，按时间降序 |
| POST | `/api/tickets/draft` | `analysisId` 必选 | 同一分析只创建一份内存草稿 |
| POST | `/api/tickets/approve` | `ticketId, approvalCode` 必选 | 记录模拟人工审批 |
| GET | `/api/tickets` | 无 | 当前 JVM 中的工单，无分页及排序保证 |

成功响应为 `{"success":true,"data":...}`；错误响应为 `{"success":false,"error":"说明"}`。JSON POST 必须使用 `Content-Type: application/json`，请求体最多 16 KiB。带有效口令访问未知路径或不支持的方法返回 404。

报告时间、工单时间和健康检查时间采用 `Instant` 的 ISO-8601 UTC 格式，例如 `2026-10-12T00:00:00Z`。所有业务数据仅在内存中，重启丢失；报告和工单列表未设置数量上限。

## 2. 从分析到审批

```bash
# STEP A: 获取演示风险报告，复制 data.id
curl -s -X POST http://127.0.0.1:8080/api/analyze \
  -H 'X-Lab-Token: learn-agent-local' -H 'Content-Type: application/json' \
  -d '{"ip":"192.0.2.10","mode":"demo","question":"查询模拟漏洞及修复建议"}'

# STEP B: 替换上一步返回的 data.id
curl -s -X POST http://127.0.0.1:8080/api/tickets/draft \
  -H 'X-Lab-Token: learn-agent-local' -H 'Content-Type: application/json' \
  -d '{"analysisId":"<替换成第一步 data.id>"}'

# STEP C: 替换草稿响应的 data.id，不要假定它一定是 TICKET-0001
curl -s -X POST http://127.0.0.1:8080/api/tickets/approve \
  -H 'X-Lab-Token: learn-agent-local' -H 'Content-Type: application/json' \
  -d '{"ticketId":"<替换成草稿 data.id>","approvalCode":"local-approval-only"}'

# STEP D: 查看结果
curl -s -H 'X-Lab-Token: learn-agent-local' http://127.0.0.1:8080/api/tickets
```

预期工单 `DRAFT -> APPROVED`。同一分析重复请求草稿返回已有工单；重复审批返回 400。审批码可通过 `LAB_APPROVAL_CODE` 修改，`approvedBy` 固定为 `local-lab-reviewer`，不代表已认证的真实操作者。不会调用扫描器或外部工单平台。

## 3. 故意触发失败

```bash
# 无口令：403
curl -i http://127.0.0.1:8080/api/assets

# 跨租户：403
curl -i 'http://127.0.0.1:8080/api/assets?ip=198.51.100.9' \
  -H 'X-Lab-Token: learn-agent-local'

# 不存在的资产：400
curl -i 'http://127.0.0.1:8080/api/assets?ip=192.0.2.123' \
  -H 'X-Lab-Token: learn-agent-local'

# 错误审批码：403，需先创建草稿
curl -i -X POST http://127.0.0.1:8080/api/tickets/approve \
  -H 'X-Lab-Token: learn-agent-local' -H 'Content-Type: application/json' \
  -d '{"ticketId":"<替换成草稿 data.id>","approvalCode":"wrong-code"}'
```

`llm` 模式中，模型连接失败、HTTP 错误、格式错误、空 `choices`、超过循环上限统一返回 502。错误响应不透传模型服务的原始响应内容。单个工具因越权或参数错误而被拒绝时，调用错误会返回模型；模型仍可能继续并给出最终解释。

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

此接口没有 `success/data` 外层，不保存报告、评分或工单。默认目标白名单仅允许 `192.0.2.10`，越权请求在联系模型前返回 403；参数不合法返回 400，已映射的模型/工具服务异常返回 502，错误体为 `{"error":"说明"}`。Spring 示例没有正式身份认证，也不要求 `X-Lab-Token`；工具网关向核心 API 转发时才添加配置的实验口令。

## 6. 断点调试建议

- `AgentService.analyze()`：观察模型调用前已计算的评分和证据。
- `ModelToolAgent.reason()`：观察每轮 `messages` 和 `tool_calls`。
- `ScopedTools.call()`：观察工具白名单、参数和资产范围校验。
- `toolTrace.origin`：区分固定编排与模型请求。
- `report.summary`：辅助解释，不是可靠的独立安全证据。
