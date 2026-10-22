<script setup lang="ts">
import PageHeader from '../shared/ui/PageHeader.vue'
import AsyncState from '../shared/ui/AsyncState.vue'
import Icon from '../shared/ui/Icon.vue'
import Badge from '../shared/ui/Badge.vue'
import { useResource } from '../shared/composables/useResource'
import { workbenchApi } from '../features/workbench/api'
import { analysesApi } from '../features/analyses/api'
import { ticketsApi } from '../features/tickets/api'
import { formatTime, shortId } from '../shared/format'
const { data, pending, error, reload } = useResource(async (signal) => {
  // 统计按当前租户的报告/工单计数，不将“高风险报告数”误当成高风险资产数。
  const [stats, reports, tickets] = await Promise.all([
    workbenchApi.dashboard(signal),
    analysesApi.page({ page: 1, size: 5 }, signal),
    ticketsApi.page({ page: 1, size: 4, status: 'DRAFT' }, signal),
  ])
  return { stats, reports, tickets }
})
</script>
<template>
  <PageHeader title="工作台"
    ><button class="button secondary" :disabled="pending" @click="reload"><Icon name="refresh" />刷新</button
    ><RouterLink class="button" to="/analysis/new"><Icon name="plus" />新建安全分析</RouterLink></PageHeader
  >
  <AsyncState :pending="pending" :error="error" @retry="reload"
    ><template v-if="data">
      <div class="metrics">
        <RouterLink to="/assets" class="panel metric"
          ><div class="metric-heading">授权资产</div>
          <div class="metric-value">{{ data.stats.assetCount }}</div>
          <div class="metric-note">{{ data.stats.exposedAssetCount }} 项存在公网暴露</div></RouterLink
        >
        <RouterLink to="/reports" class="panel metric"
          ><div class="metric-heading">分析报告</div>
          <div class="metric-value">{{ data.stats.reportCount }}</div>
        </RouterLink>
        <RouterLink to="/reports?riskLevel=HIGH" class="panel metric"
          ><div class="metric-heading">高风险报告</div>
          <div class="metric-value">{{ data.stats.highRiskReportCount }}</div>
        </RouterLink>
        <RouterLink to="/tickets?status=DRAFT" class="panel metric"
          ><div class="metric-heading">待审批工单</div>
          <div class="metric-value">{{ data.stats.pendingTicketCount }}</div>
          <div class="metric-note">{{ data.stats.approvedTicketCount }} 份工单已完成模拟审批</div></RouterLink
        >
      </div>
      <div class="stack">
        <div class="content-aside">
          <section class="panel">
            <div class="panel-header">
              <h2>最近分析报告</h2>
              <RouterLink to="/reports" class="text-link"
                >全部报告<Icon name="arrow" :size="14"
              /></RouterLink>
            </div>
            <AsyncState
              :pending="false"
              :empty="!data.reports.items.length"
              empty-title="从第一份分析报告开始"
              empty-hint="选择一项授权资产，运行规则模式即可查看风险、证据和工具轨迹。"
              ><template #empty-action
                ><RouterLink class="button" to="/analysis/new">创建第一份报告</RouterLink></template
              >
              <div class="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>分析对象</th>
                      <th>风险等级</th>
                      <th>分析模式</th>
                      <th>创建时间</th>
                      <th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="report in data.reports.items" :key="report.id">
                      <td>
                        <strong class="mono">{{ report.assetIp }}</strong
                        ><span class="cell-sub mono">#{{ shortId(report.id) }}</span>
                      </td>
                      <td>
                        <Badge :value="report.riskLevel" /><span class="cell-sub"
                          >{{ report.riskScore }} / 100</span
                        >
                      </td>
                      <td><Badge :value="report.mode" /></td>
                      <td>{{ formatTime(report.time) }}</td>
                      <td>
                        <RouterLink class="text-link" :to="'/reports/' + report.id">查看报告</RouterLink>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div></AsyncState
            >
          </section>
          <section class="panel">
            <div class="panel-header">
              <h2>待办工单</h2>
              <Badge
                v-if="data.stats.pendingTicketCount > 0"
                value="DRAFT"
                :text="data.stats.pendingTicketCount + ' 份待审批'"
              />
            </div>
            <AsyncState
              :pending="false"
              :empty="!data.tickets.items.length"
              empty-title="当前没有待审批工单"
              empty-hint="在报告详情生成草稿后，可在这里继续审核。"
              ><template #empty-action
                ><RouterLink to="/reports" class="text-link">前往分析报告</RouterLink></template
              >
              <ul class="record-list">
                <li v-for="ticket in data.tickets.items" :key="ticket.id">
                  <RouterLink :to="'/tickets/' + ticket.id"
                    ><h3>{{ ticket.title }}</h3>
                    <div class="record-meta">
                      <span class="mono muted">{{ ticket.assetIp }}</span
                      ><Badge :value="ticket.riskLevel" />
                    </div>
                    <span class="cell-sub">{{ formatTime(ticket.createdAt) }}</span></RouterLink
                  >
                </li>
              </ul></AsyncState
            >
          </section>
        </div>
      </div>
    </template></AsyncState
  >
</template>
