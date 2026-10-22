<script setup lang="ts">
import { ref } from 'vue'
import { token, setToken } from '../shared/session'
import { workbenchApi } from '../features/workbench/api'
import { useResource } from '../shared/composables/useResource'
import { formatTime } from '../shared/format'
import PageHeader from '../shared/ui/PageHeader.vue'
import AsyncState from '../shared/ui/AsyncState.vue'
import Icon from '../shared/ui/Icon.vue'
const input = ref(token.value),
  saved = ref(false)
const { data, pending, error, reload } = useResource(async (signal) => {
  const [health, tools] = await Promise.all([workbenchApi.health(signal), workbenchApi.tools(signal)])
  return { health, tools }
})
function save() {
  setToken(input.value)
  saved.value = true
}
</script>
<template>
  <PageHeader title="实验设置" description="管理当前标签页的实验连接，了解不同模式和工具的职责。" />
  <div class="two-columns">
    <section class="panel">
      <div class="panel-header">
        <h2>本地实验连接</h2>
        <Icon name="settings" />
      </div>
      <form class="panel-body" @submit.prevent="save">
        <div class="field">
          <label for="labToken">本地实验口令</label
          ><input
            id="labToken"
            v-model="input"
            type="password"
            autocomplete="off"
            required
            @input="saved = false"
          /><span class="helper">通过 X-Lab-Token 发送，仅保留在当前标签页会话中。</span>
        </div>
        <div class="actions" style="margin-top: var(--space-sm)">
          <button class="button" type="submit">保存口令并检查连接</button
          ><button class="button secondary" type="button" :disabled="pending" @click="reload">
            重新检查
          </button>
        </div>
        <p v-if="saved" class="status-message" role="status">当前标签页的实验口令已更新。</p>
      </form>
      <AsyncState :pending="pending" :error="error" @retry="reload"
        ><div v-if="data" class="panel-body">
          <div class="notice success">
            <Icon name="check" />
            <p>
              核心服务连接正常 · {{ data.health.status }}<br />服务时间
              {{ formatTime(data.health.timestamp) }}
            </p>
          </div>
          <p class="muted" style="margin-top: var(--space-xs)">
            该检查验证核心 API 可访问，模型是否可用以实际分析结果为准。
          </p>
        </div></AsyncState
      >
    </section>
    <section class="panel panel-body help-card">
      <h2>选择适合的实验模式</h2>
      <h3 style="margin-top: var(--space-md)">规则分析 · demo</h3>
      <p class="muted">无需连接模型。Java 编排只读工具、关键词检索与确定性风险评分，生成报告和整改草稿。</p>
      <h3 style="margin-top: var(--space-md)">模型分析 · llm</h3>
      <p class="muted">
        使用服务端配置的模型进行 Tool Calling。工具调用受资产与租户范围约束，模型解释需人工核查。
      </p>
      <h3 style="margin-top: var(--space-md)">Spring AI / MCP 扩展练习</h3>
      <p class="muted">
        仓库中的独立服务用于学习工具接入与协议适配，运行方式见项目
        README。完整报告与工单流程由当前核心服务提供。
      </p>
    </section>
  </div>
  <section v-if="data" class="panel" style="margin-top: var(--space-md)">
    <div class="panel-header">
      <h2>可用只读工具</h2>
      <span class="muted">{{ data.tools.length }} 个工具</span>
    </div>
    <div class="panel-body">
      <article v-for="tool in data.tools" :key="tool.function.name">
        <div class="section-heading">
          <h3 class="mono">{{ tool.function.name }}</h3>
          <span class="badge">只读</span>
        </div>
        <p class="muted">{{ tool.function.description }}</p>
        <details>
          <summary>查看工具参数定义</summary>
          <pre>{{ JSON.stringify(tool.function.parameters, null, 2) }}</pre>
        </details>
      </article>
    </div>
  </section>
</template>
