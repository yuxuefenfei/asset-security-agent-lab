<script setup lang="ts">
import { useRoute } from 'vue-router'
import { assetsApi } from '../features/assets/api'
import { analysesApi } from '../features/analyses/api'
import AssetFacts from '../features/assets/AssetFacts.vue'
import VulnerabilityTable from '../features/assets/VulnerabilityTable.vue'
import { useResource } from '../shared/composables/useResource'
import PageHeader from '../shared/ui/PageHeader.vue'
import AsyncState from '../shared/ui/AsyncState.vue'
import Badge from '../shared/ui/Badge.vue'
import Icon from '../shared/ui/Icon.vue'
import { formatTime } from '../shared/format'
const route = useRoute(),
  ip = String(route.params.ip)
const { data, pending, error, reload } = useResource(async (signal) => {
  const [asset, vulnerabilities, reports] = await Promise.all([
    assetsApi.get(ip, signal),
    assetsApi.vulnerabilities(ip, signal),
    analysesApi.page({ q: ip, page: 1, size: 5 }, signal),
  ])
  return { asset, vulnerabilities, reports }
})
</script>
<template>
  <PageHeader :title="data?.asset.hostname || '资产详情'" :description="ip + ' · 当前授权资产'"
    ><RouterLink class="button secondary" to="/assets">返回资产列表</RouterLink
    ><RouterLink class="button" :to="{ path: '/analysis/new', query: { ip } }"
      ><Icon name="scan" />分析此资产</RouterLink
    ></PageHeader
  >
  <AsyncState :pending="pending" :error="error" @retry="reload"
    ><div v-if="data" class="stack">
      <section class="panel">
        <div class="panel-header">
          <h2>资产信息</h2>
          <code>{{ data.asset.id }}</code>
        </div>
        <div class="panel-body"><AssetFacts :asset="data.asset" /></div>
      </section>
      <section class="panel">
        <div class="panel-header">
          <h2>模拟漏洞记录</h2>
          <span class="muted">{{ data.vulnerabilities.length }} 条记录</span>
        </div>
        <VulnerabilityTable :items="data.vulnerabilities" />
      </section>
      <section class="panel">
        <div class="panel-header">
          <h2>此资产的最近分析</h2>
          <RouterLink class="text-link" :to="{ path: '/reports', query: { q: ip } }"
            >查看全部报告<Icon name="arrow" :size="14"
          /></RouterLink>
        </div>
        <AsyncState
          :pending="false"
          :empty="!data.reports.items.length"
          empty-title="此资产还没有分析报告"
          empty-hint="使用规则模式，无需连接模型也可完成首次演练。"
          ><template #empty-action
            ><RouterLink class="button" :to="{ path: '/analysis/new', query: { ip } }"
              >开始分析</RouterLink
            ></template
          >
          <div class="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>报告编号</th>
                  <th>风险</th>
                  <th>模式</th>
                  <th>创建时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="report in data.reports.items" :key="report.id">
                  <td class="mono">{{ report.id.slice(0, 8) }}</td>
                  <td><Badge :value="report.riskLevel" /> {{ report.riskScore }} 分</td>
                  <td><Badge :value="report.mode" /></td>
                  <td>{{ formatTime(report.time) }}</td>
                  <td><RouterLink class="text-link" :to="'/reports/' + report.id">查看报告</RouterLink></td>
                </tr>
              </tbody>
            </table>
          </div></AsyncState
        >
      </section>
    </div></AsyncState
  >
</template>
