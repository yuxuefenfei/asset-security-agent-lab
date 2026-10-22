<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ page: number; size: number; total: number }>()
defineEmits<{ change: [page: number, size?: number] }>()
const pages = computed(() => Math.max(1, Math.ceil(props.total / props.size)))
</script>
<template>
  <div v-if="total > 10 || page > 1" class="pagination">
    <span>共 {{ total }} 条记录</span>
    <div class="pagination-controls">
      <label class="sr-only" for="pageSize">每页条数</label
      ><select
        id="pageSize"
        :value="size"
        @change="$emit('change', 1, Number(($event.target as HTMLSelectElement).value))"
      >
        <option :value="10">10 条 / 页</option>
        <option :value="20">20 条 / 页</option>
        <option :value="50">50 条 / 页</option></select
      ><button class="button secondary compact" :disabled="page <= 1" @click="$emit('change', page - 1)">
        上一页</button
      ><span>{{ page }} / {{ pages }}</span
      ><button class="button secondary compact" :disabled="page >= pages" @click="$emit('change', page + 1)">
        下一页
      </button>
    </div>
  </div>
</template>
