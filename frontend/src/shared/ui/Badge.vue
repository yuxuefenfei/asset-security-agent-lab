<script setup lang="ts">
import { label } from '../format'
defineProps<{ value: string; text?: string }>()
</script>
<template>
  <span class="badge" :class="'badge-' + value.toLowerCase()"
    ><span class="badge-dot"></span>{{ text || label(value) }}</span
  >
</template>
