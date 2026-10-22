<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Icon from '../shared/ui/Icon.vue'
const route = useRoute(),
  menuOpen = ref(false)
const nav = [
  { to: '/', name: '工作台', icon: 'grid' },
  { to: '/assets', name: '资产中心', icon: 'server' },
  { to: '/analysis/new', name: '安全分析', icon: 'scan' },
  { to: '/reports', name: '分析报告', icon: 'file' },
  { to: '/tickets', name: '整改工单', icon: 'ticket' },
  { to: '/knowledge', name: '知识库', icon: 'book' },
]
watch(
  () => route.fullPath,
  () => {
    menuOpen.value = false
  },
)
</script>
<template>
  <a class="skip-link" href="#main-content">跳到主要内容</a>
  <div class="app-layout compact-workbench" @keydown.esc="menuOpen = false">
    <button v-if="menuOpen" class="nav-backdrop" aria-label="关闭导航" @click="menuOpen = false"></button>
    <aside class="sidebar" :class="{ 'is-open': menuOpen }">
      <RouterLink class="brand" to="/">
        <span class="brand-mark"><Icon name="shield" :size="21" /></span>
        <span>资产安全</span>
      </RouterLink>
      <nav id="main-navigation" aria-label="主导航">
        <RouterLink
          v-for="item in nav"
          :key="item.to"
          :to="item.to"
          :class="['nav-link', { 'is-active': route.meta.section === item.name }]"
          :aria-current="route.meta.section === item.name ? 'page' : undefined"
        >
          <Icon :name="item.icon" :size="18" /><span>{{ item.name }}</span>
        </RouterLink>
      </nav>
      <div class="sidebar-bottom">
        <RouterLink
          class="nav-link settings-link"
          :class="{ 'is-active': route.path === '/settings' }"
          :aria-current="route.path === '/settings' ? 'page' : undefined"
          to="/settings"
        >
          <Icon name="settings" :size="18" /><span>实验设置</span>
        </RouterLink>
      </div>
    </aside>
    <div class="app-main">
      <header class="topbar">
        <div class="topbar-left">
          <button
            class="icon-button menu-button"
            :aria-expanded="menuOpen"
            aria-label="切换导航"
            aria-controls="main-navigation"
            @click="menuOpen = !menuOpen"
          >
            <Icon :name="menuOpen ? 'close' : 'menu'" :size="19" />
          </button>
          <span class="shell-label">安全运营工作台</span>
        </div>
        <span class="environment-label">本地演练 · 模拟数据</span>
      </header>
      <main id="main-content" tabindex="-1">
        <!-- 路径变更重建详情实例；仅查询参数变化时保留列表实例，由路由查询驱动重读。 -->
        <RouterView v-slot="{ Component }"><component :is="Component" :key="route.path" /></RouterView>
      </main>
    </div>
  </div>
</template>
