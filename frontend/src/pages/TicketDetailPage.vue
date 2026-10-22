<script setup lang="ts">
import { computed, reactive, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, onBeforeRouteLeave } from 'vue-router'
import { ticketsApi } from '../features/tickets/api'
import { analysesApi } from '../features/analyses/api'
import EvidenceList from '../features/analyses/EvidenceList.vue'
import { constraints, type TicketResponse } from '../generated/contracts'
import { useResource } from '../shared/composables/useResource'
import { useCommand } from '../shared/composables/useCommand'
import { ApiError } from '../shared/api/client'
import PageHeader from '../shared/ui/PageHeader.vue'
import AsyncState from '../shared/ui/AsyncState.vue'
import Badge from '../shared/ui/Badge.vue'
import Icon from '../shared/ui/Icon.vue'
import { formatTime, shortId } from '../shared/format'
const id = String(useRoute().params.id)
const { data, pending, error, reload } = useResource(async (signal) => {
  const ticket = await ticketsApi.get(id, signal)
  const report = await analysesApi.get(ticket.analysisId, signal)
  return { ticket, report }
})
const form = reactive({ title: '', remediation: '', note: '' }),
  code = ref(''),
  notice = ref(''),
  conflicted = ref(false)
const { busy, error: commandError, execute } = useCommand()
// 仅服务端新快照重置表单与审批码；编辑中的本地内容单独保留并计算 dirty。
watch(
  data,
  (value) => {
    if (!value) return
    Object.assign(form, {
      title: value.ticket.title,
      remediation: value.ticket.remediation,
      note: value.ticket.note || '',
    })
    code.value = ''
    conflicted.value = false
    notice.value = ''
  },
  { flush: 'sync' },
)
// 与已加载版本比较，确保审批针对已保存内容，不能跳过本地未保存的修改。
const dirty = computed(
  () =>
    !!data.value &&
    (form.title !== data.value.ticket.title ||
      form.remediation !== data.value.ticket.remediation ||
      form.note !== (data.value.ticket.note || '')),
)
const editable = computed(() => data.value?.ticket.status === 'DRAFT' && !conflicted.value)
const locked = computed(() => !editable.value || busy.value)
async function perform(operation: (signal: AbortSignal) => Promise<TicketResponse>, message: string) {
  notice.value = ''
  const result = await execute(async (signal) => {
    try {
      return await operation(signal)
    } catch (e) {
      // 冲突后冻结编辑与审批，必须主动重载并重新审核，不能自动覆盖别人的修改。
      if (e instanceof ApiError && e.status === 409) conflicted.value = true
      throw e
    }
  })
  if (result && data.value) {
    data.value = { ...data.value, ticket: result }
    notice.value = message
  }
}
function save() {
  if (!data.value || !editable.value || !dirty.value || busy.value) return
  // 固定提交时用户看到的版本；保存成功后才采用服务端返回的新版本。
  const version = data.value.ticket.version
  return perform(
    (signal) => ticketsApi.edit(id, { ...form, version }, signal),
    '草稿修改已保存，请核查当前版本后审批。',
  )
}
function approve() {
  if (!data.value || !editable.value || dirty.value || busy.value || !code.value) return
  const payload = { ticketId: id, approvalCode: code.value, version: data.value.ticket.version }
  return perform((signal) => ticketsApi.approve(payload, signal), '人工审批已完成，工单内容已锁定（模拟）。')
}
function refresh() {
  if (dirty.value && !window.confirm('重新加载会丢弃未保存的修改，是否继续？')) return
  return reload()
}
// SPA 跳转与浏览器刷新分别拦截；处理中离开也需提醒用户重新确认服务端结果。
onBeforeRouteLeave(
  () =>
    !(dirty.value || busy.value) ||
    window.confirm(
      busy.value
        ? '操作仍在处理中，离开后请重新打开工单确认结果。是否离开？'
        : '工单有未保存的修改，是否放弃修改并离开？',
    ),
)
function beforeUnload(event: BeforeUnloadEvent) {
  if (dirty.value || busy.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}
onMounted(() => window.addEventListener('beforeunload', beforeUnload))
onBeforeUnmount(() => window.removeEventListener('beforeunload', beforeUnload))
</script>
<template>
  <PageHeader :title="'工单 #' + shortId(id)" description="先核查证据与整改内容，再审批当前草稿版本。"
    ><RouterLink class="button secondary" to="/tickets">返回工单列表</RouterLink
    ><button class="button secondary" :disabled="pending || busy" @click="refresh">
      <Icon name="refresh" />重新加载草稿
    </button></PageHeader
  >
  <AsyncState :pending="pending" :error="error" @retry="reload"
    ><div v-if="data" class="stack">
      <section class="panel panel-body">
        <div class="section-heading">
          <h2>{{ data.ticket.title }}</h2>
          <Badge :value="data.ticket.status" />
        </div>
        <div class="summary-meta">
          <span class="mono">{{ data.ticket.assetIp }}</span
          ><Badge :value="data.ticket.riskLevel" /><span>版本 v{{ data.ticket.version }}</span
          ><span>创建于 {{ formatTime(data.ticket.createdAt) }}</span
          ><RouterLink class="text-link" :to="'/reports/' + data.ticket.analysisId"
            >查看关联报告<Icon name="arrow" :size="14"
          /></RouterLink>
        </div>
      </section>
      <div v-if="conflicted" class="notice error" role="alert">
        <Icon name="alert" />
        <div>
          <strong>草稿已更新，请重新加载后审核</strong>
          <p>当前版本已被其他操作修改。编辑和审批已暂停，请重新加载最新草稿。</p>
          <button class="button secondary" @click="refresh">重新加载最新草稿</button>
        </div>
      </div>
      <div v-if="data.ticket.status === 'APPROVED'" class="notice success">
        <Icon name="check" />
        <p>
          {{ data.ticket.approvedBy }} 于 {{ formatTime(data.ticket.approvedAt) }} 完成审批。当前内容已锁定。
        </p>
      </div>
      <div class="content-aside">
        <div class="stack">
          <section class="panel">
            <div class="panel-header">
              <h2>整改内容</h2>
              <span class="muted">{{ dirty ? '存在未保存修改' : '已与当前版本同步' }}</span>
            </div>
            <form class="panel-body" @submit.prevent="save">
              <fieldset :disabled="locked">
                <div class="field">
                  <label for="ticketTitle">工单标题</label
                  ><input
                    id="ticketTitle"
                    v-model="form.title"
                    required
                    :maxlength="constraints.MAX_TICKET_TITLE_LENGTH"
                  />
                </div>
                <div class="field">
                  <label for="remediation">整改建议</label
                  ><textarea
                    id="remediation"
                    v-model="form.remediation"
                    required
                    :maxlength="constraints.MAX_REMEDIATION_LENGTH"
                    rows="10"
                  ></textarea
                  ><span class="helper"
                    >{{ form.remediation.length }} / {{ constraints.MAX_REMEDIATION_LENGTH }}</span
                  >
                </div>
                <div class="field">
                  <label for="ticketNote">审核备注</label
                  ><textarea
                    id="ticketNote"
                    v-model="form.note"
                    :maxlength="constraints.MAX_NOTE_LENGTH"
                    rows="3"
                  ></textarea>
                </div>
              </fieldset>
              <div class="form-footer">
                <span class="muted" aria-live="polite">{{
                  data.ticket.status === 'APPROVED'
                    ? '已审批内容只读'
                    : conflicted
                      ? '请先重新加载最新草稿'
                      : dirty
                        ? '请先保存修改，再进行审批'
                        : '审批将确认当前展示的草稿版本'
                }}</span
                ><button id="saveTicket" class="button" type="submit" :disabled="locked || !dirty">
                  {{ busy ? '正在处理…' : '保存草稿修改' }}
                </button>
              </div>
            </form>
          </section>
          <section class="panel">
            <div class="panel-header">
              <h2>关联报告的知识证据</h2>
              <span class="muted">报告快照</span>
            </div>
            <div class="panel-body"><EvidenceList :items="data.report.knowledgeEvidence" /></div>
          </section>
        </div>
        <aside class="stack">
          <section class="panel">
            <div class="panel-header">
              <h2>人工审批</h2>
              <Icon name="shield" />
            </div>
            <form class="panel-body" @submit.prevent="approve">
              <p class="muted">请确认已核对报告证据、整改建议和维护窗口。审批后将锁定当前内容。</p>
              <div class="field" style="margin-top: var(--space-md)">
                <label for="code">本地审批码</label
                ><input
                  id="code"
                  v-model="code"
                  type="password"
                  autocomplete="off"
                  required
                  :disabled="locked || dirty"
                  aria-describedby="approvalHint"
                /><span id="approvalHint" class="helper">本地模拟审批；审批码不会保存在浏览器。</span>
              </div>
              <button id="approve" class="button" type="submit" :disabled="locked || dirty || !code">
                {{
                  busy ? '正在处理…' : data.ticket.status === 'APPROVED' ? '工单已审批' : '人工审批当前草稿'
                }}
              </button>
              <p v-if="dirty" class="muted" style="margin-top: var(--space-xs)">
                请先保存草稿修改，审批码输入与审批操作将恢复。
              </p>
            </form>
          </section>
          <section class="panel panel-body help-card">
            <h3>审核要点</h3>
            <ul class="help-list">
              <li>确认目标资产与业务负责人。</li>
              <li>核查漏洞和引用资料的适用范围。</li>
              <li>将模型建议改为可执行的整改步骤。</li>
              <li>重新加载后再次核对版本。</li>
            </ul>
          </section>
        </aside>
      </div>
      <p v-if="commandError" class="status-message error" role="alert">{{ commandError }}</p>
      <p v-if="notice" class="status-message" role="status">{{ notice }}</p>
    </div></AsyncState
  >
</template>
