<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { analysesApi } from '../features/analyses/api'
import { ticketsApi } from '../features/tickets/api'
import AssetFacts from '../features/assets/AssetFacts.vue'
import VulnerabilityTable from '../features/assets/VulnerabilityTable.vue'
import EvidenceList from '../features/analyses/EvidenceList.vue'
import TraceList from '../features/analyses/TraceList.vue'
import { useResource } from '../shared/composables/useResource'
import { useCommand } from '../shared/composables/useCommand'
import PageHeader from '../shared/ui/PageHeader.vue'
import AsyncState from '../shared/ui/AsyncState.vue'
import Badge from '../shared/ui/Badge.vue'
import Icon from '../shared/ui/Icon.vue'
import { formatTime, shortId } from '../shared/format'
const route = useRoute(),
  router = useRouter(),
  id = String(route.params.id)
// 报告按保存时快照展示；关联工单由 analysisId 查询，不能用当前资产数据重建历史报告。
const { data, pending, error, reload } = useResource(async (signal) => {
  const [report, tickets] = await Promise.all([
    analysesApi.get(id, signal),
    ticketsApi.page({ page: 1, size: 1, analysisId: id }, signal),
  ])
  return { report, ticket: tickets.items[0] }
})
const { busy, error: commandError, execute } = useCommand()
const tabs = [
  { key: 'overview', name: '风险概要' },
  { key: 'vulnerabilities', name: '漏洞记录' },
  { key: 'evidence', name: '知识证据' },
  { key: 'trace', name: '工具轨迹' },
]
const tab = computed(() =>
  tabs.some((t) => t.key === route.query.tab) ? String(route.query.tab) : 'overview',
)
async function openTicket() {
  // 已存在工单直接打开，包括已审批工单；重复点击不应创建新的审核链。
  if (data.value?.ticket) {
    await router.push('/tickets/' + data.value.ticket.id)
    return
  }
  const result = await execute((signal) => ticketsApi.draft(id, signal))
  if (result) await router.push('/tickets/' + result.id)
}
</script>
<template>
  <PageHeader
    :title="'分析报告 #' + shortId(id)"
    description="核查风险、漏洞与知识引文，确认依据后再创建整改工单。"
    ><RouterLink class="button secondary" to="/reports">返回报告列表</RouterLink
    ><button class="button" :disabled="pending || busy || !data" @click="openTicket">
      <Icon name="ticket" />{{ busy ? '正在生成草稿…' : data?.ticket ? '打开关联工单' : '生成工单草稿' }}
    </button></PageHeader
  >
  <p v-if="commandError" class="status-message error" role="alert">{{ commandError }}</p>
  <AsyncState :pending="pending" :error="error" @retry="reload"
    ><div v-if="data" class="stack">
      <section class="panel summary-strip">
        <div class="risk-score" :class="data.report.riskLevel.toLowerCase()">
          <strong>{{ data.report.riskScore }}</strong
          ><small>教学风险评分 / 100</small>
        </div>
        <div class="summary-info">
          <h2>
            {{ data.report.asset.hostname }} <span class="mono">{{ data.report.asset.ip }}</span>
          </h2>
          <div class="actions">
            <Badge :value="data.report.riskLevel" /><Badge :value="data.report.mode" /><Badge
              :value="data.report.status"
            />
          </div>
          <div class="summary-meta">
            <span>{{ data.report.asset.business }}</span
            ><span>{{ data.report.vulnerabilities.length }} 条漏洞记录</span
            ><span>{{ data.report.knowledgeEvidence.length }} 份知识证据</span
            ><span>{{ formatTime(data.report.time) }}</span>
          </div>
        </div>
        <RouterLink :to="'/assets/' + data.report.asset.ip" class="text-link"
          >查看当前资产<Icon name="arrow" :size="16"
        /></RouterLink>
      </section>
      <div class="notice warning">
        <Icon name="alert" />
        <p>{{ data.report.warning }}</p>
      </div>
      <section class="panel">
        <nav class="tabs" aria-label="报告内容">
          <RouterLink
            v-for="item in tabs"
            :key="item.key"
            :to="{ query: { tab: item.key } }"
            :class="{ active: tab === item.key }"
            :aria-current="tab === item.key ? 'page' : undefined"
            >{{ item.name
            }}<span v-if="item.key === 'trace'">（{{ data.report.toolTrace.length }}）</span></RouterLink
          >
        </nav>
        <div class="panel-body">
          <template v-if="tab === 'overview'"
            ><h2 class="form-section-title">分析说明</h2>
            <p id="analysisSummary" class="prose">{{ data.report.summary }}</p>
            <h2 class="form-section-title" style="margin-top: var(--space-lg)">分析时的资产快照</h2>
            <AssetFacts :asset="data.report.asset" />
            <details>
              <summary>高级详情：结构化报告 JSON</summary>
              <pre>{{ JSON.stringify(data.report, null, 2) }}</pre>
            </details></template
          >
          <VulnerabilityTable v-else-if="tab === 'vulnerabilities'" :items="data.report.vulnerabilities" />
          <EvidenceList v-else-if="tab === 'evidence'" :items="data.report.knowledgeEvidence" />
          <TraceList v-else :items="data.report.toolTrace" />
        </div>
      </section>
      <div class="notice">
        <Icon name="clock" />
        <p>以上内容为分析时保存的快照，后续资产与知识资料变化不会覆盖历史报告。</p>
      </div>
    </div></AsyncState
  >
</template>
