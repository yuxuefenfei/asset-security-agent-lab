<script setup lang="ts">
import { constraints } from '../generated/contracts'
import { ticketsApi } from '../features/tickets/api'
import { usePagedQuery } from '../shared/composables/usePagedQuery'
import PageHeader from '../shared/ui/PageHeader.vue'
import AsyncState from '../shared/ui/AsyncState.vue'
import Pagination from '../shared/ui/Pagination.vue'
import Badge from '../shared/ui/Badge.vue'
import Icon from '../shared/ui/Icon.vue'
import { formatTime, shortId } from '../shared/format'
const { data, pending, error, reload, filters, apply, reset, page, size, turn } = usePagedQuery(
  ticketsApi.page,
  ['q', 'status', 'riskLevel'],
)
</script>
<template>
  <PageHeader title="整改工单" description="核查报告与整改建议，保存草稿后对当前版本进行人工审批。"
    ><button class="button secondary" :disabled="pending" @click="reload"><Icon name="refresh" />刷新</button
    ><RouterLink class="button" to="/reports">从报告生成工单<Icon name="arrow" :size="16" /></RouterLink
  ></PageHeader>
  <section class="panel">
    <form class="filter-bar" @submit.prevent="apply">
      <div class="field">
        <label for="ticketSearch">搜索工单</label
        ><input
          id="ticketSearch"
          v-model="filters.q"
          :maxlength="constraints.MAX_QUERY_LENGTH"
          placeholder="工单标题、编号或资产 IP"
        />
      </div>
      <div class="field filter-select">
        <label for="statusFilter">工单状态</label
        ><select id="statusFilter" v-model="filters.status">
          <option value="">全部状态</option>
          <option value="DRAFT">待审批</option>
          <option value="APPROVED">已审批</option>
        </select>
      </div>
      <div class="field filter-select">
        <label for="ticketRisk">风险等级</label
        ><select id="ticketRisk" v-model="filters.riskLevel">
          <option value="">全部风险</option>
          <option value="HIGH">高风险</option>
          <option value="MEDIUM">中风险</option>
          <option value="LOW">低风险</option>
        </select>
      </div>
      <button class="button" type="submit"><Icon name="search" :size="16" />查询</button
      ><button type="button" class="button secondary" @click="reset">重置</button>
    </form>
    <AsyncState
      :pending="pending"
      :error="error"
      :empty="data?.items.length === 0"
      empty-title="当前没有符合条件的工单"
      empty-hint="在分析报告中生成整改草稿，再回到这里进行审核。"
      @retry="reload"
      ><template #empty-action
        ><RouterLink class="button" to="/reports">前往分析报告</RouterLink
        ><button class="button secondary" @click="reset">清除筛选</button></template
      >
      <div class="table-scroll">
        <table>
          <thead>
            <tr>
              <th>工单 / 资产</th>
              <th>风险</th>
              <th>状态</th>
              <th>当前版本</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="ticket in data?.items" :key="ticket.id">
              <td>
                <strong>{{ ticket.title }}</strong
                ><span class="cell-sub mono">#{{ shortId(ticket.id) }} · {{ ticket.assetIp }}</span>
              </td>
              <td><Badge :value="ticket.riskLevel" /></td>
              <td><Badge :value="ticket.status" /></td>
              <td class="mono">v{{ ticket.version }}</td>
              <td>{{ formatTime(ticket.createdAt) }}</td>
              <td>
                <RouterLink class="text-link" :to="'/tickets/' + ticket.id">{{
                  ticket.status === 'DRAFT' ? '审核草稿' : '查看详情'
                }}</RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div></AsyncState
    >
    <Pagination v-if="data" :page="page" :size="size" :total="data.total" @change="turn" />
  </section>
</template>
