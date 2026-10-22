<script setup lang="ts">
import PageHeader from '../shared/ui/PageHeader.vue'
</script>
<template>
  <PageHeader title="页面不存在" description="该地址可能已变更，请从导航或工作台继续。" />
  <section class="panel empty-state">
    <h2>找不到这个页面</h2>
    <RouterLink class="button" to="/">返回工作台</RouterLink>
  </section>
</template>
