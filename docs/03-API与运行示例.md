# 第 3 课：API 运行与审批全链路

所有离线 API 默认监听 `http://127.0.0.1:8080`，除首页以外，请求头必须带 `X-Lab-Token: learn-agent-local`
。这仅是本地教学口令，没有正式身份认证能力。

## 1. API 清单

| 方法 | 路径                   | 请求参数                 | 说明                              |
|------|------------------------|--------------------------|-----------------------------------|
| GET  | `/`                    | 无                       | 教学工作台                        |
| GET  | `/api/health`          | 无                       | 进程探针                          |
| GET  | `/api/assets`          | `ip` 可选                | 列出当前模拟租户资产 / 查询某资产 |
| GET  | `/api/vulnerabilities` | `ip` 必选                | 目标资产模拟扫描记录              |
| GET  | `/api/knowledge`       | `q` 必选                 | 关键词检索知识文档                |
| GET  | `/api/tools`           | 无                       | 给 LLM 的工具 Schema              |
| POST | `/api/analyze`         | `ip, mode, question`     | 分析资产（`mode`: demo/llm）      |
| GET  | `/api/analyses`        | 无                       | 分析报告列表                      |
| POST | `/api/tickets/draft`   | `analysisId`             | 创建草稿；同一分析幂等            |
| POST | `/api/tickets/approve` | `ticketId, approvalCode` | 模拟人工审批                      |
| GET  | `/api/tickets`         | 无                       | 工单列表                          |

## 2. 从分析到审批

```bash
# STEP A: 获取一份演示风险报告，复制 data.id
curl -s -X POST http://127.0.0.1:8080/api/analyze \
  -H 'X-Lab-Token: learn-agent-local' -H 'Content-Type: application/json' \
  -d '{"ip":"192.0.2.10","mode":"demo","question":"查询扫描漏洞及修复建议"}'

# STEP B: 将上一步的 data.id 换到这里
curl -s -X POST http://127.0.0.1:8080/api/tickets/draft \
  -H 'X-Lab-Token: learn-agent-local' -H 'Content-Type: application/json' \
  -d '{"analysisId":"<替换成第一步 data.id>"}'

# STEP C: 将上一步返回的 data.id 作为 ticketId
curl -s -X POST http://127.0.0.1:8080/api/tickets/approve \
  -H 'X-Lab-Token: learn-agent-local' -H 'Content-Type: application/json' \
  -d '{"ticketId":"TICKET-0001","approvalCode":"local-approval-only"}'

# STEP D: 查看结果
curl -s -H 'X-Lab-Token: learn-agent-local' http://127.0.0.1:8080/api/tickets
```

**预期**：工单 `DRAFT -> APPROVED`，重复草稿返回同一 ID；重复审批失败。任何步骤都不会调用真实漏洞扫描器或外部工单平台。

## 3. 故意触发安全失败

```bash
# 无 token：应返回 403
curl -i http://127.0.0.1:8080/api/assets

# 跨租户 IP：应返回 403
curl -i 'http://127.0.0.1:8080/api/assets?ip=198.51.100.9' \
  -H 'X-Lab-Token: learn-agent-local'

# 不存在的 IP：应返回 400
curl -i 'http://127.0.0.1:8080/api/assets?ip=192.0.2.123' \
  -H 'X-Lab-Token: learn-agent-local'

# 错误审批码：应返回 403（请先创建草稿）
```

## 4. 观察模型执行

分析响应包含以下字段：

```json
{
  "id": "随机 UUID",
  "mode": "demo",
  "riskLevel": "HIGH",
  "riskScore": 100,
  "asset": {
    "ip": "192.0.2.10",
    "hostname": "web-prod-01"
  },
  "vulnerabilities": [
    "实际响应为漏洞对象数组"
  ],
  "knowledgeEvidence": [
    "实际响应为带 source 的文档对象数组"
  ],
  "toolTrace": [
    {
      "step": 1,
      "tool": "get_asset",
      "arguments": {
        "ip": "192.0.2.10"
      },
      "resultCount": 1
    }
  ]
}
```

上述为 **结构示意**，实际响应包括更多字段与 trace 项。示例的风险 100/100 仅是自定义教学计算，不是安全业界标准评分。

## 5. 原理调试建议

在 `ModelToolAgent.reason()` 断点关注：

- `messages`：每轮完整对话，其中包含 role=assistant/tool 的消息；
- `calls`：模型提出的工具调用请求；
- `ScopedTools.call()`：模型请求与 Java 真正执行之间的安全边界；
- `trace`：实际执行成功的只读工具名单；
- `report.summary`：模型解释或本地规则输出，不能作为可靠的独立安全证据。

注意示例中模型 API 错误被转换为 502，不会默默伪装成“模型已分析”。这样便于你区分成功、降级和未验证输出。
