<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Icon from '../shared/ui/Icon.vue'
const route = useRoute(),
  menuOpen = ref(false)
const nav = [
  { to: '/', name: '工作台', icon: 'grid', caption: '概览与待办' },
  { to: '/assets', name: '资产中心', icon: 'server', caption: '资产与漏洞' },
  { to: '/analysis/new', name: '安全分析', icon: 'scan', caption: '创建分析' },
  { to: '/reports', name: '分析报告', icon: 'file', caption: '结果与证据' },
  { to: '/tickets', name: '整改工单', icon: 'ticket', caption: '审核与审批' },
  { to: '/knowledge', name: '知识库', icon: 'book', caption: '检索与阅读' },
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
  <div class="app-layout" @keydown.esc="menuOpen = false">
    <button v-if="menuOpen" class="nav-backdrop" aria-label="关闭导航" @click="menuOpen = false"></button>
    <aside class="sidebar" :class="{ 'is-open': menuOpen }">
      <RouterLink class="brand" to="/"
        ><span class="brand-mark"><Icon name="shield" :size="26" /></span
        ><span>资产安全<span class="brand-sub">AGENT 实验室</span></span></RouterLink
      >
      <div class="workspace-label">
        <span class="status-dot"></span>本地教学工作空间<span class="workspace-tag">LAB</span>
      </div>
      <nav id="main-navigation" aria-label="主导航">
        <RouterLink
          v-for="item in nav"
          :key="item.to"
          :to="item.to"
          :class="['nav-link', { 'is-active': route.meta.section === item.name }]"
          :aria-current="route.meta.section === item.name ? 'page' : undefined"
          ><Icon :name="item.icon" /><span
            >{{ item.name }}<small>{{ item.caption }}</small></span
          ><Icon class="nav-chevron" name="chevron" :size="15"
        /></RouterLink>
      </nav>
      <div class="sidebar-bottom">
        <div class="lab-note">
          <Icon name="code" /><strong>从工具调用到人工审批</strong>
          <p>用可追踪的证据理解 Agent 的每一步。</p>
        </div>
        <RouterLink
          class="nav-link settings-link"
          :class="{ 'is-active': route.path === '/settings' }"
          to="/settings"
          ><Icon name="settings" /><span>实验设置</span></RouterLink
        >
        <div class="sidebar-footer">虚构训练数据 · 本地实验环境</div>
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
            <Icon :name="menuOpen ? 'close' : 'menu'" /></button
          ><span class="breadcrumb"
            >工作空间 <Icon name="chevron" :size="14" />
            <strong>{{ route.meta.section || '页面导航' }}</strong></span
          >
        </div>
        <div class="topbar-right">
          <span class="training-label">教学演练</span
          ><RouterLink to="/settings" class="connection-link"
            ><span class="status-dot"></span>连接设置</RouterLink
          ><span class="avatar" aria-label="本地实验用户">研</span>
        </div>
      </header>
      <main id="main-content" tabindex="-1">
        <!-- 路径变更重建详情实例；仅查询参数变化时保留列表实例，由路由查询驱动重读。 -->
        <RouterView v-slot="{ Component }"><component :is="Component" :key="route.path" /></RouterView>
      </main>
      <footer class="app-footer">
        <span>资产安全 Agent 实验室</span><span>模拟评分与审批，仅用于学习 · 时间显示为北京时间</span>
      </footer>
    </div>
  </div>
</template>
