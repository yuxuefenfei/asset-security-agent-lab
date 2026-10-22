<script setup lang="ts">
defineProps<{ title: string; description?: string; count?: number }>()
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="page-title-row">
        <h1>{{ title }}</h1>
        <span v-if="count !== undefined" class="page-count">{{ count }} 项</span>
      </div>
      <p v-if="description">{{ description }}</p>
    </div>
    <div v-if="$slots.default" class="heading-actions"><slot /></div>
  </div>
</template>
