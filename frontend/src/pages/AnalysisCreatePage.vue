<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { assetsApi } from '../features/assets/api'
import { analysesApi } from '../features/analyses/api'
import type { AnalysisMode } from '../generated/contracts'
import { constraints } from '../generated/contracts'
import { useResource } from '../shared/composables/useResource'
import { useCommand } from '../shared/composables/useCommand'
import PageHeader from '../shared/ui/PageHeader.vue'
import AsyncState from '../shared/ui/AsyncState.vue'
import Icon from '../shared/ui/Icon.vue'
const route = useRoute(),
  router = useRouter()
const ip = ref(String(route.query.ip || '')),
  mode = ref<AnalysisMode>('demo'),
  question = ref('请分析这台资产的安全风险，引用相关知识库证据并给出整改建议。')
const search = ref(String(route.query.ip || '')),
  query = ref(search.value),
  assetPage = ref(1)
const { data, pending, error, reload } = useResource(
  (signal) => assetsApi.page({ page: assetPage.value, size: 20, q: query.value }, signal),
  [query, assetPage],
)
watch(data, (value) => {
  if (value && !value.items.some((a) => a.ip === ip.value)) ip.value = value.items[0]?.ip || ''
})
const { busy, error: runError, execute } = useCommand()
async function run() {
  if (!ip.value || !question.value.trim()) return
  const result = await execute((signal) =>
    analysesApi.run({ ip: ip.value, question: question.value, mode: mode.value }, signal),
  )
  // useCommand 只返回当前有效请求的结果，离开页面或切换口令后不再自动跳转。
  if (result) await router.push('/reports/' + result.id)
}
function findAssets() {
  const unchanged = assetPage.value === 1 && query.value === search.value.trim()
  assetPage.value = 1
  query.value = search.value.trim()
  if (unchanged) reload()
}
function resetAssets() {
  search.value = ''
  findAssets()
}
</script>
<template>
  <PageHeader title="新建安全分析" description="选择一项授权资产，让工具、知识和人工判断形成可核查的证据链。"
    ><RouterLink to="/reports" class="button secondary">查看历史报告</RouterLink></PageHeader
  >
  <div class="content-aside">
    <section class="panel">
      <div class="panel-header">
        <h2>分析任务</h2>
        <span class="muted">完成后自动保存报告</span>
      </div>
      <div class="panel-body">
        <form @submit.prevent="findAssets" class="asset-picker-search">
          <div class="field">
            <label for="assetPickerSearch">查找授权资产</label
            ><input
              id="assetPickerSearch"
              v-model="search"
              :maxlength="constraints.MAX_QUERY_LENGTH"
              placeholder="IP、主机名称或业务"
              :disabled="busy"
            />
          </div>
          <button class="button secondary" type="submit" :disabled="busy">查找资产</button>
        </form>
        <AsyncState
          :pending="pending"
          :error="error"
          :empty="data?.items.length === 0"
          empty-title="没有匹配的授权资产"
          empty-hint="调整搜索内容后重新查找。"
          @retry="reload"
          ><template #empty-action
            ><button class="button secondary" @click="resetAssets">显示授权资产</button></template
          >
          <form @submit.prevent="run" id="analysisForm">
            <fieldset class="form-fields" :disabled="busy">
              <div class="asset-picker">
                <div class="field">
                  <label for="ip">分析对象</label
                  ><select id="ip" v-model="ip" required>
                    <option v-for="asset in data?.items" :value="asset.ip" :key="asset.id">
                      {{ asset.ip }} · {{ asset.hostname }} · {{ asset.business }}
                    </option></select
                  ><span class="helper">只显示当前租户授权的资产。</span>
                </div>
                <div v-if="data && data.total > 20" class="actions asset-picker-pagination">
                  <button
                    class="button secondary"
                    type="button"
                    :disabled="assetPage === 1"
                    @click="assetPage--"
                  >
                    上一组资产</button
                  ><span class="muted">第 {{ assetPage }} 组</span
                  ><button
                    class="button secondary"
                    type="button"
                    :disabled="assetPage * 20 >= data.total"
                    @click="assetPage++"
                  >
                    下一组资产
                  </button>
                </div>
              </div>
              <div class="field">
                <span class="field-label" id="modeLabel">执行模式</span>
                <div class="mode-options" role="radiogroup" aria-labelledby="modeLabel">
                  <label class="mode-option"
                    ><input type="radio" name="mode" v-model="mode" value="demo" /><span
                      ><strong>规则分析</strong><small>无需模型 · 确定性评分与工具追踪</small></span
                    ></label
                  ><label class="mode-option"
                    ><input type="radio" name="mode" v-model="mode" value="llm" /><span
                      ><strong>模型 Tool Calling</strong><small>调用已配置模型 · 辅助解释与检索</small></span
                    ></label
                  >
                </div>
              </div>
              <div class="field">
                <label for="question">分析目标</label
                ><textarea
                  id="question"
                  v-model="question"
                  required
                  :maxlength="constraints.MAX_QUESTION_LENGTH"
                  rows="5"
                ></textarea
                ><span class="helper"
                  >{{ question.length }} / {{ constraints.MAX_QUESTION_LENGTH }} ·
                  描述需要核查的风险、证据或整改问题。</span
                >
              </div>
            </fieldset>
            <p v-if="runError" class="status-message error" role="alert">{{ runError }}</p>
            <div v-if="busy" class="notice" role="status">
              <Icon name="clock" />
              <p>
                正在执行分析，请等待服务返回。模型模式可能耗时较长；离开页面后可从报告列表确认是否已生成报告。
              </p>
            </div>
            <div class="form-footer">
              <span class="muted">报告保留资产、漏洞和证据快照。</span
              ><button id="run" class="button" type="submit" :disabled="busy || !ip">
                <Icon name="scan" />{{ busy ? '正在分析…' : '开始安全分析' }}
              </button>
            </div>
          </form></AsyncState
        >
      </div>
    </section>
    <aside class="stack">
      <section class="panel panel-body help-card">
        <div class="help-card-heading">
          <Icon name="shield" :size="28" />
          <h2>先理解，再判断</h2>
        </div>
        <ol class="help-list">
          <li>工具在当前资产和租户范围内读取数据。</li>
          <li>知识检索提供可核对的正文和来源。</li>
          <li>Java 规则计算教学风险分数。</li>
          <li>模型解释需要人工核查，工单审批由人完成。</li>
        </ol>
      </section>
      <div class="notice warning">
        <Icon name="alert" />
        <p>模型模式需要已配置的模型服务。当前使用虚构训练资料，不执行真实漏洞扫描。</p>
      </div>
      <RouterLink class="text-link" to="/settings"
        >查看模式与连接说明<Icon name="arrow" :size="16"
      /></RouterLink>
    </aside>
  </div>
</template>
