# 第 5 课：知识检索与 RAG 练习

本文区分当前实现与尚未实现的升级方案，依据 2026-10-22 的 `KnowledgeService`、`ToolSession` 和 H2 迁移。

## 当前实现

初始化共有 16 份共享训练文档，返回类型为 `KnowledgeResponse` record，含 `id,title,keywords,body,source`。`LAB-2026-0001` 可匹配 `KB-001`，新增 `LAB-2026-0101` 对应 `KB-101`。数据分布见[第 12 课](12-模拟数据集.md)。

| 入口                           | 行为                                                                    |
| ------------------------------ | ----------------------------------------------------------------------- |
| `GET /api/knowledge?q=...`     | 空白返回空数组；词项召回和相关性排序在数据库执行，API 默认最多 5 条     |
| `ToolSession.search_knowledge` | 同一检索实现，工具默认最多 3 条；证据按知识 ID 去重                     |
| `GET /api/knowledge/page`      | 标题 / 关键词 / 正文的字面子串筛选、ID 排序和数据库分页；空查询浏览目录 |
| `GET /api/knowledge/{id}`      | 当前文档全文                                                            |
| 历史报告 knowledgeEvidence     | 分析保存时的正文和来源快照，不随当前知识修改而变                        |

工具检索会将 query 转小写、拆成保留字母 / 数字 / 下划线 / 点 / 连字符的词项，忽略短于 2 的词项；匹配标题、关键词或正文的 `lab-` 开头词项权重 5，其余 1，相同相关性按 ID 排序。它仍是子串匹配，不是结构化漏洞编号严格相等查询。

Java 预查询会把知识附在报告中；这并不等于模型已收到内容。核心 llm 和 Spring Agent 只有在模型请求知识工具后，才把该结果写入模型交互。demo 模式不生成模型回答。核心 llm 当前另有 Schema 限制，见[第 4 课](04-ToolCalling与MCP讲解.md)。

`training://` 是虚构来源标识，不能当成可打开的官方公告 URL。知识不做文档级租户授权；模型引文没有服务端一致性验证。

## 尚未实现的能力

没有文本分块、Embedding、向量数据库、BM25、混合召回、重排和量化检索评测。当前可以称为「关键词检索与来源证据练习」，不能写成已完成高质量向量 RAG。

漏洞编号、组件与受影响版本适合结构化精确匹配；自然语言修复问题可再增加语义召回。pgvector 负责向量存储 / 相似度查询，BM25 和重排仍需单独设计，参考 [pgvector 混合检索说明](https://github.com/pgvector/pgvector#hybrid-search)。

## 后续数据模型示意

以下 PostgreSQL DDL 仅为设计练习，不属于当前 H2 Flyway 迁移。向量维度必须与实际模型一致。

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE knowledge_chunk (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL,
    document_id VARCHAR(80) NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    advisory_id VARCHAR(100),
    product VARCHAR(200),
    document_version VARCHAR(80),
    source_uri TEXT NOT NULL,
    embedding vector(768),
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_chunk_tenant_advisory
    ON knowledge_chunk (tenant_id, advisory_id);
```

## 从现有代码推进的练习

1. 在现有类型化 KnowledgeResponse 基础上区分文档与 chunk，增加 advisoryId、版本和更新时间；无需再做 List<Map> 到 record 的迁移。
2. 对漏洞编号严格匹配，验证相似编号不会混入；定义精确召回与关键词结果的优先级。
3. 使用可信公告来源，保留原文与解析版本；为缺失证据返回明确的不足信息。
4. 接入生产身份后先做文档授权过滤，再做任何检索；可信租户来自服务端身份。
5. 按实际需要接入 Embedding、pgvector、混合召回和重排。
6. 校验模型引用的证据 ID、版本和内容；资料不足不能生成伪来源。
7. 用固定样本比较命中率、错误引用率、延迟与成本，再决定复杂化程度。

文档和工具返回内容都应视为不可信数据；提示词声明不能代替工具授权和输出核验。当前没有文件、Shell、自动审批或真实扫描工具。
