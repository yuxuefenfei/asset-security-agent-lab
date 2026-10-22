<script setup lang="ts">
import type { ToolTrace } from '../../generated/contracts'
defineProps<{ items: ToolTrace[] }>()
const names: Record<string, string> = {
  get_asset: '查询资产',
  get_vulnerabilities: '查询漏洞',
  search_knowledge: '检索知识',
}
</script>
<template>
  <p class="muted">区分 Java 固定编排与模型主动请求，逐项核查工具参数和返回数量。</p>
  <ol class="trace-list">
    <li v-for="trace in items" :key="trace.step">
      <span class="trace-step">{{ trace.step }}</span>
      <div>
        <div class="section-heading">
          <div>
            <h3>{{ names[trace.tool] || trace.tool }}</h3>
            <code>{{ trace.tool }}</code>
          </div>
          <span class="badge" :class="trace.origin === 'llm-request' ? 'badge-llm' : ''">{{
            trace.origin === 'llm-request'
              ? '模型请求'
              : trace.origin === 'java-orchestrator'
                ? 'Java 编排'
                : trace.origin
          }}</span>
        </div>
        <div class="trace-meta">返回 {{ trace.resultCount }} 条记录</div>
        <details>
          <summary>查看调用参数</summary>
          <pre>{{ JSON.stringify(trace.arguments, null, 2) }}</pre>
        </details>
      </div>
    </li>
  </ol>
</template>
