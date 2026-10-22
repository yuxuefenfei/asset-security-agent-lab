<script setup lang="ts">
import { constraints } from '../generated/contracts'
import { assetsApi } from '../features/assets/api'
import { usePagedQuery } from '../shared/composables/usePagedQuery'
import PageHeader from '../shared/ui/PageHeader.vue'
import AsyncState from '../shared/ui/AsyncState.vue'
import Pagination from '../shared/ui/Pagination.vue'
import Icon from '../shared/ui/Icon.vue'
import Badge from '../shared/ui/Badge.vue'
const { data, pending, error, reload, filters, apply, reset, page, size, turn } = usePagedQuery(
  assetsApi.page,
  ['q', 'criticality', 'exposed'],
)
</script>
<template>
  <PageHeader title="资产中心" :count="data?.total"
    ><RouterLink class="button" to="/analysis/new"><Icon name="plus" />新建安全分析</RouterLink></PageHeader
  >
  <section class="panel">
    <form class="filter-bar" @submit.prevent="apply">
      <div class="field">
        <label for="assetSearch">搜索资产</label
        ><input
          id="assetSearch"
          v-model="filters.q"
          :maxlength="constraints.MAX_QUERY_LENGTH"
          placeholder="IP、主机名称、业务或负责人"
        />
      </div>
      <div class="field filter-select">
        <label for="criticality">业务重要性</label
        ><select id="criticality" v-model="filters.criticality">
          <option value="">全部重要性</option>
          <option value="HIGH">高</option>
          <option value="MEDIUM">中</option>
          <option value="LOW">低</option>
        </select>
      </div>
      <div class="field filter-select">
        <label for="exposed">暴露范围</label
        ><select id="exposed" v-model="filters.exposed">
          <option value="">全部范围</option>
          <option value="true">公网暴露</option>
          <option value="false">仅内网</option>
        </select>
      </div>
      <button class="button" type="submit"><Icon name="search" :size="16" />查询</button
      ><button class="button secondary" type="button" @click="reset">重置</button>
    </form>
    <AsyncState
      :pending="pending"
      :error="error"
      :empty="data?.items.length === 0"
      empty-title="没有符合条件的资产"
      empty-hint="调整搜索条件，查看当前租户授权的训练资产。"
      @retry="reload"
      ><template #empty-action><button class="button secondary" @click="reset">清除筛选</button></template>
      <div class="table-scroll">
        <table>
          <thead>
            <tr>
              <th>资产 / 主机名称</th>
              <th>所属业务</th>
              <th>重要性</th>
              <th>暴露范围</th>
              <th>负责人</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="asset in data?.items" :key="asset.id">
              <td>
                <RouterLink class="text-link mono" :to="'/assets/' + asset.ip">{{ asset.ip }}</RouterLink
                ><span class="cell-sub">{{ asset.hostname }}</span>
              </td>
              <td>
                {{ asset.business }}<span class="cell-sub">{{ asset.os }}</span>
              </td>
              <td>
                <Badge
                  :value="asset.criticality"
                  :text="{ HIGH: '高', MEDIUM: '中', LOW: '低' }[asset.criticality]"
                />
              </td>
              <td>
                <Badge
                  :value="asset.internetExposed ? 'DRAFT' : 'LOW'"
                  :text="asset.internetExposed ? '公网暴露' : '仅内网'"
                />
              </td>
              <td>{{ asset.owner }}</td>
              <td class="cell-action">
                <RouterLink class="text-link" :to="'/assets/' + asset.ip">查看详情</RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div></AsyncState
    >
    <Pagination v-if="data" :page="page" :size="size" :total="data.total" @change="turn" />
  </section>
</template>
