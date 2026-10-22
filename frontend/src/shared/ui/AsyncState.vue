<script setup lang="ts">
import Icon from './Icon.vue'
defineProps<{ pending: boolean; error?: string; empty?: boolean; emptyTitle?: string; emptyHint?: string }>()
defineEmits<{ retry: [] }>()
</script>
<template>
  <div v-if="pending" class="skeleton-wrap" role="status" aria-live="polite">
    <span class="sr-only">正在加载…</span>
    <div class="skeleton" v-for="n in 4" :key="n"></div>
  </div>
  <div v-else-if="error" class="empty-state error-state" role="alert">
    <Icon name="alert" :size="32" />
    <h3>暂时无法加载</h3>
    <p>{{ error }}</p>
    <div class="actions">
      <button class="button secondary" @click="$emit('retry')"><Icon name="refresh" />重新加载</button
      ><RouterLink class="text-link" to="/settings">检查实验连接</RouterLink>
    </div>
  </div>
  <div v-else-if="empty" class="empty-state">
    <Icon name="layers" :size="32" />
    <h3>{{ emptyTitle || '暂无记录' }}</h3>
    <p>{{ emptyHint || '调整筛选条件后重新查询。' }}</p>
    <slot name="empty-action" />
  </div>
  <slot v-else />
</template>
