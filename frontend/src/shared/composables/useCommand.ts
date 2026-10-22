import { ref, onScopeDispose, watch } from 'vue'
import { sessionRevision } from '../session'
import { errorText } from '../api/client'
/** 分析、保存和审批的提交状态；同一实例防重复提交，失效响应不交给页面继续跳转。 */
export function useCommand() {
  const busy = ref(false),
    error = ref('')
  let controller: AbortController | undefined,
    generation = 0
  // 这里只取消浏览器等待；服务端可能已提交，重新打开页面后应查询实际结果。
  function cancel() {
    generation++
    controller?.abort()
    busy.value = false
    error.value = ''
  }
  async function execute<T>(operation: (signal: AbortSignal) => Promise<T>): Promise<T | undefined> {
    if (busy.value) return
    const current = ++generation
    controller = new AbortController()
    busy.value = true
    error.value = ''
    try {
      const result = await operation(controller.signal)
      // 只向仍存活的当前操作返回结果，防止旧分析响应把用户带回旧报告。
      if (current === generation) return result
    } catch (e) {
      if (current === generation && !(e instanceof DOMException && e.name === 'AbortError'))
        error.value = errorText(e)
    } finally {
      if (current === generation) busy.value = false
    }
  }
  watch(sessionRevision, cancel)
  onScopeDispose(cancel)
  return { busy, error, execute }
}
