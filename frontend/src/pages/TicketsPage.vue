<script setup lang="ts">
import { computed } from 'vue'
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
const statuses = [
  { value: '', label: '全部工单' },
  { value: 'DRAFT', label: '待审批' },
  { value: 'APPROVED', label: '已审批' },
]
const hasFilters = computed(() => Object.values(filters).some(Boolean))
function selectStatus(status: string) {
  filters.status = status
  return apply()
}
</script>
<template>
  <PageHeader title="整改工单" :count="data?.total">
    <RouterLink class="button" to="/reports"><Icon name="plus" :size="16" />从报告生成工单</RouterLink>
  </PageHeader>
  <section class="panel ticket-list">
    <div class="ticket-toolbar">
      <div class="status-switch" role="group" aria-label="工单状态">
        <button
          v-for="status in statuses"
          :key="status.value"
          type="button"
          :aria-pressed="filters.status === status.value"
          @click="selectStatus(status.value)"
        >
          {{ status.label }}
        </button>
      </div>
      <form class="filter-bar ticket-filters" @submit.prevent="apply">
        <div class="field search-field">
          <label class="sr-only" for="ticketSearch">搜索工单</label>
          <Icon name="search" :size="16" />
          <input
            id="ticketSearch"
            v-model="filters.q"
            :maxlength="constraints.MAX_QUERY_LENGTH"
            placeholder="搜索标题、编号或资产 IP"
          />
        </div>
        <div class="field filter-select">
          <label class="sr-only" for="ticketRisk">风险等级</label>
          <select id="ticketRisk" v-model="filters.riskLevel">
            <option value="">全部风险</option>
            <option value="HIGH">高风险</option>
            <option value="MEDIUM">中风险</option>
            <option value="LOW">低风险</option>
          </select>
        </div>
        <button class="button secondary" type="submit">查询</button>
        <button v-if="hasFilters" type="button" class="button quiet" @click="reset">重置</button>
      </form>
      <button
        class="icon-button refresh-list"
        :disabled="pending"
        aria-label="刷新工单"
        title="刷新"
        @click="reload"
      >
        <Icon name="refresh" :size="17" />
      </button>
    </div>
    <AsyncState
      :pending="pending"
      :error="error"
      :empty="data?.items.length === 0"
      empty-title="当前没有符合条件的工单"
      empty-hint="调整筛选条件，或从报告生成工单。"
      @retry="reload"
    >
      <template #empty-action>
        <RouterLink class="button" to="/reports">前往分析报告</RouterLink>
        <button class="button secondary" @click="reset">清除筛选</button>
      </template>
      <div class="table-scroll">
        <table class="tickets-table">
          <thead>
            <tr>
              <th>工单</th>
              <th>关联资产</th>
              <th>风险</th>
              <th>状态</th>
              <th>创建时间</th>
              <th aria-label="操作"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="ticket in data?.items" :key="ticket.id">
              <td>
                <RouterLink class="ticket-title" :title="ticket.title" :to="'/tickets/' + ticket.id">{{
                  ticket.title
                }}</RouterLink>
                <span class="cell-sub mono">#{{ shortId(ticket.id) }}</span>
              </td>
              <td>
                <RouterLink class="asset-reference mono" :to="'/assets/' + ticket.assetIp">{{
                  ticket.assetIp
                }}</RouterLink>
              </td>
              <td><Badge :value="ticket.riskLevel" /></td>
              <td><Badge :value="ticket.status" /></td>
              <td class="table-time">{{ formatTime(ticket.createdAt) }}</td>
              <td class="row-action">
                <RouterLink class="text-link" :to="'/tickets/' + ticket.id"
                  >{{ ticket.status === 'DRAFT' ? '审核草稿' : '查看详情' }}<Icon name="chevron" :size="13"
                /></RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </AsyncState>
    <Pagination v-if="data" :page="page" :size="size" :total="data.total" @change="turn" />
  </section>
</template>
