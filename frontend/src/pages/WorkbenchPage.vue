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
const steps = [
  { title: '确认分析对象', hint: '查看授权资产、业务与暴露端口', to: '/assets', action: '浏览资产' },
  { title: '执行安全分析', hint: '选择规则或模型，提出分析目标', to: '/analysis/new', action: '新建分析' },
  { title: '核查报告证据', hint: '对照漏洞、引文与工具调用', to: '/reports', action: '查看报告' },
  {
    title: '审核整改工单',
    hint: '编辑建议，确认版本后人工审批',
    to: '/tickets?status=DRAFT',
    action: '处理待办',
  },
]
</script>
<template>
  <PageHeader title="安全运营工作台" description="从资产风险到证据核查，在一个清晰的流程中完成教学演练。"
    ><button class="button secondary" :disabled="pending" @click="reload"><Icon name="refresh" />刷新</button
    ><RouterLink class="button" to="/analysis/new"><Icon name="plus" />新建安全分析</RouterLink></PageHeader
  >
  <AsyncState :pending="pending" :error="error" @retry="reload"
    ><template v-if="data">
      <div class="metrics">
        <RouterLink to="/assets" class="panel metric"
          ><div class="metric-heading">
            授权资产<span class="metric-icon"><Icon name="server" /></span>
          </div>
          <div class="metric-value">{{ data.stats.assetCount }}</div>
          <div class="metric-note">{{ data.stats.exposedAssetCount }} 项存在公网暴露</div></RouterLink
        >
        <RouterLink to="/reports" class="panel metric"
          ><div class="metric-heading">
            分析报告<span class="metric-icon"><Icon name="file" /></span>
          </div>
          <div class="metric-value">{{ data.stats.reportCount }}</div>
          <div class="metric-note">已完成并保存的分析快照</div></RouterLink
        >
        <RouterLink to="/reports?riskLevel=HIGH" class="panel metric"
          ><div class="metric-heading">
            高风险报告<span class="metric-icon"><Icon name="alert" /></span>
          </div>
          <div class="metric-value">{{ data.stats.highRiskReportCount }}</div>
          <div class="metric-note">按报告计数，采用教学评分规则</div></RouterLink
        >
        <RouterLink to="/tickets?status=DRAFT" class="panel metric"
          ><div class="metric-heading">
            待审批工单<span class="metric-icon"><Icon name="ticket" /></span>
          </div>
          <div class="metric-value">{{ data.stats.pendingTicketCount }}</div>
          <div class="metric-note">{{ data.stats.approvedTicketCount }} 份工单已完成模拟审批</div></RouterLink
        >
      </div>
      <div class="stack">
        <section class="panel">
          <div class="panel-header">
            <h2>分析到审批，一条完整的证据链</h2>
            <span class="muted">本地实验流程</span>
          </div>
          <div class="workflow">
            <div class="workflow-step" v-for="(step, i) in steps" :key="step.title">
              <span class="workflow-number">{{ i + 1 }}</span>
              <h3>{{ step.title }}</h3>
              <p>{{ step.hint }}</p>
              <RouterLink class="text-link" :to="step.to"
                >{{ step.action }}<Icon name="arrow" :size="14"
              /></RouterLink>
            </div>
          </div>
        </section>
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
              <Badge value="DRAFT" :text="data.stats.pendingTicketCount + ' 份待审批'" />
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
        <div class="notice">
          <Icon name="book" />
          <p>
            这是一个教学工作空间，使用虚构资产与漏洞数据。规则评分、知识关键词检索和本地审批用于理解 Agent
            机制。知识库现有 {{ data.stats.knowledgeCount }} 份资料，可在报告中核对引用来源。
          </p>
        </div>
      </div>
    </template></AsyncState
  >
</template>
