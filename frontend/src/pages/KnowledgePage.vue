<script setup lang="ts">
import { constraints } from '../generated/contracts'
import { knowledgeApi } from '../features/knowledge/api'
import { usePagedQuery } from '../shared/composables/usePagedQuery'
import PageHeader from '../shared/ui/PageHeader.vue'
import AsyncState from '../shared/ui/AsyncState.vue'
import Pagination from '../shared/ui/Pagination.vue'
import Icon from '../shared/ui/Icon.vue'
const { data, pending, error, reload, filters, apply, reset, page, size, turn } = usePagedQuery(
  knowledgeApi.page,
  ['q'],
)
</script>
<template>
  <PageHeader title="知识库" :count="data?.total" />
  <section class="panel">
    <form class="filter-bar" @submit.prevent="apply">
      <div class="field">
        <label for="knowledgeSearch">搜索知识资料</label
        ><input
          id="knowledgeSearch"
          v-model="filters.q"
          :maxlength="constraints.MAX_QUERY_LENGTH"
          placeholder="标题、关键词或正文内容"
        />
      </div>
      <button class="button" type="submit"><Icon name="search" :size="16" />检索知识</button
      ><button class="button secondary" type="button" @click="reset">全部资料</button>
    </form>
    <AsyncState
      :pending="pending"
      :error="error"
      :empty="data?.items.length === 0"
      empty-title="没有找到相关资料"
      empty-hint="尝试公告编号、组件名称或更简短的关键词。"
      @retry="reload"
      ><template #empty-action
        ><button class="button secondary" @click="reset">浏览全部资料</button></template
      >
      <div class="knowledge-grid">
        <article class="knowledge-card" v-for="item in data?.items" :key="item.id">
          <code class="muted">{{ item.id }}</code>
          <h2>{{ item.title }}</h2>
          <p>{{ item.body }}</p>

          <RouterLink class="text-link" :to="'/knowledge/' + item.id"
            >阅读全文<Icon name="arrow" :size="14"
          /></RouterLink>
        </article></div
    ></AsyncState>
    <Pagination v-if="data" :page="page" :size="size" :total="data.total" @change="turn" />
  </section>
</template>
