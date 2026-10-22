<script setup lang="ts">
import type { KnowledgeResponse } from '../../generated/contracts'
defineProps<{ items: KnowledgeResponse[] }>()
</script>
<template>
  <div class="evidence-list">
    <p v-if="!items.length" class="muted">本次分析没有检索到知识证据，请结合漏洞记录人工核查。</p>
    <article v-for="(item, index) in items" :key="item.id" class="evidence-item">
      <div class="evidence-number">{{ index + 1 }}</div>
      <div>
        <div class="section-heading">
          <h3>{{ item.title }}</h3>
          <code>{{ item.id }}</code>
        </div>
        <p class="prose">{{ item.body }}</p>
        <div class="source-line">来源：{{ item.source }}</div>
      </div>
    </article>
  </div>
</template>
