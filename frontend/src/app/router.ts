import { createRouter, createWebHashHistory } from 'vue-router'
export const router = createRouter({
  // Hash 片段由浏览器处理，直接刷新详情页无需核心服务配置任意路径的 SPA 转发。
  history: createWebHashHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/',
      component: () => import('../pages/WorkbenchPage.vue'),
      meta: { title: '工作台', section: '工作台' },
    },
    {
      path: '/assets',
      component: () => import('../pages/AssetsPage.vue'),
      meta: { title: '资产中心', section: '资产中心' },
    },
    {
      path: '/assets/:ip',
      component: () => import('../pages/AssetDetailPage.vue'),
      meta: { title: '资产详情', section: '资产中心' },
    },
    {
      path: '/analysis/new',
      component: () => import('../pages/AnalysisCreatePage.vue'),
      meta: { title: '安全分析', section: '安全分析' },
    },
    {
      path: '/reports',
      component: () => import('../pages/ReportsPage.vue'),
      meta: { title: '分析报告', section: '分析报告' },
    },
    {
      path: '/reports/:id',
      component: () => import('../pages/ReportDetailPage.vue'),
      meta: { title: '报告详情', section: '分析报告' },
    },
    {
      path: '/tickets',
      component: () => import('../pages/TicketsPage.vue'),
      meta: { title: '整改工单', section: '整改工单' },
    },
    {
      path: '/tickets/:id',
      component: () => import('../pages/TicketDetailPage.vue'),
      meta: { title: '工单详情', section: '整改工单' },
    },
    {
      path: '/knowledge',
      component: () => import('../pages/KnowledgePage.vue'),
      meta: { title: '知识库', section: '知识库' },
    },
    {
      path: '/knowledge/:id',
      component: () => import('../pages/KnowledgeDetailPage.vue'),
      meta: { title: '知识详情', section: '知识库' },
    },
    {
      path: '/settings',
      component: () => import('../pages/SettingsPage.vue'),
      meta: { title: '实验设置', section: '实验设置' },
    },
    {
      path: '/:pathMatch(.*)*',
      component: () => import('../pages/NotFoundPage.vue'),
      meta: { title: '页面不存在' },
    },
  ],
})
router.afterEach((to) => {
  document.title = String(to.meta.title || '工作台') + ' · 资产安全 Agent 实验室'
  requestAnimationFrame(() =>
    document.querySelector<HTMLElement>('#main-content')?.focus({ preventScroll: true }),
  )
})
