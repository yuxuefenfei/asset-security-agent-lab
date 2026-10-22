<script setup lang="ts">
import { constraints } from '../generated/contracts'
import { analysesApi } from '../features/analyses/api'
import { usePagedQuery } from '../shared/composables/usePagedQuery'
import PageHeader from '../shared/ui/PageHeader.vue'
import AsyncState from '../shared/ui/AsyncState.vue'
import Pagination from '../shared/ui/Pagination.vue'
import Badge from '../shared/ui/Badge.vue'
import Icon from '../shared/ui/Icon.vue'
import { formatTime, shortId } from '../shared/format'
const { data, pending, error, reload, filters, apply, reset, page, size, turn } = usePagedQuery(
  analysesApi.page,
  ['q', 'riskLevel', 'mode'],
)
</script>
<template>
  <PageHeader title="分析报告" :count="data?.total"
    ><RouterLink class="button" to="/analysis/new"><Icon name="plus" />新建安全分析</RouterLink></PageHeader
  >
  <section class="panel">
    <form class="filter-bar" @submit.prevent="apply">
      <div class="field">
        <label for="reportSearch">搜索报告</label
        ><input
          id="reportSearch"
          v-model="filters.q"
          :maxlength="constraints.MAX_QUERY_LENGTH"
          placeholder="资产 IP 或报告编号"
        />
      </div>
      <div class="field filter-select">
        <label for="riskFilter">风险等级</label
        ><select id="riskFilter" v-model="filters.riskLevel">
          <option value="">全部风险</option>
          <option value="HIGH">高风险</option>
          <option value="MEDIUM">中风险</option>
          <option value="LOW">低风险</option>
        </select>
      </div>
      <div class="field filter-select">
        <label for="modeFilter">分析模式</label
        ><select id="modeFilter" v-model="filters.mode">
          <option value="">全部模式</option>
          <option value="DEMO">规则分析</option>
          <option value="LLM">模型分析</option>
        </select>
      </div>
      <button class="button" type="submit"><Icon name="search" :size="16" />查询</button
      ><button class="button secondary" type="button" @click="reset">重置</button>
    </form>
    <AsyncState
      :pending="pending"
      :error="error"
      :empty="data?.items.length === 0"
      empty-title="当前没有符合条件的报告"
      empty-hint="可以调整筛选条件，或选择资产创建一份新的分析报告。"
      @retry="reload"
      ><template #empty-action
        ><RouterLink class="button" to="/analysis/new">新建安全分析</RouterLink
        ><button class="button secondary" @click="reset">清除筛选</button></template
      >
      <div class="table-scroll">
        <table>
          <thead>
            <tr>
              <th>报告 / 分析对象</th>
              <th>风险等级</th>
              <th>风险评分</th>
              <th>模式</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="report in data?.items" :key="report.id">
              <td>
                <strong class="mono">#{{ shortId(report.id) }}</strong
                ><span class="cell-sub mono">{{ report.assetIp }}</span>
              </td>
              <td><Badge :value="report.riskLevel" /></td>
              <td class="mono">{{ report.riskScore }} / 100</td>
              <td><Badge :value="report.mode" /></td>
              <td>{{ formatTime(report.time) }}</td>
              <td><RouterLink class="text-link" :to="'/reports/' + report.id">查看报告</RouterLink></td>
            </tr>
          </tbody>
        </table>
      </div></AsyncState
    >
    <Pagination v-if="data" :page="page" :size="size" :total="data.total" @change="turn" />
  </section>
</template>
