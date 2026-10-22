<script setup lang="ts">
import { useRoute } from 'vue-router'
import { knowledgeApi } from '../features/knowledge/api'
import { useResource } from '../shared/composables/useResource'
import PageHeader from '../shared/ui/PageHeader.vue'
import AsyncState from '../shared/ui/AsyncState.vue'
import Icon from '../shared/ui/Icon.vue'
const id = String(useRoute().params.id)
const { data, pending, error, reload } = useResource((signal) => knowledgeApi.get(id, signal))
</script>
<template>
  <PageHeader :title="data?.title || '知识资料'" :description="id"
    ><RouterLink class="button secondary" to="/knowledge">返回知识库</RouterLink></PageHeader
  ><AsyncState :pending="pending" :error="error" @retry="reload"
    ><article v-if="data" class="panel panel-body">
      <div class="notice">
        <Icon name="book" />
        <p>来源：{{ data.source }}</p>
      </div>
      <div class="keywords">
        <span v-for="word in data.keywords.split(/[,，;\s]+/).filter(Boolean)" :key="word">{{ word }}</span>
      </div>
      <h2 style="margin: var(--space-lg) 0 var(--space-md)">资料正文</h2>
      <p class="prose">{{ data.body }}</p>
    </article></AsyncState
  >
</template>
