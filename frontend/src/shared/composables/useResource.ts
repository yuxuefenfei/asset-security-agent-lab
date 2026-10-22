import { shallowRef, ref, watch, onScopeDispose, type WatchSource } from 'vue'
import { sessionRevision } from '../session'
import { errorText } from '../api/client'
/** 页面只读数据：依赖或实验会话变化时重读，取消旧请求并隔离迟到结果。 */
export function useResource<T>(loader: (signal: AbortSignal) => Promise<T>, sources: WatchSource[] = []) {
  const data = shallowRef<T | null>(null)
  const pending = ref(false),
    error = ref('')
  let controller: AbortController | undefined,
    generation = 0
  async function reload() {
    // abort 不能保证所有异步阶段立刻停止，代次校验再阻止旧结果覆盖新状态。
    const current = ++generation
    controller?.abort()
    controller = new AbortController()
    pending.value = true
    error.value = ''
    // 切换口令或筛选时先清空，避免新会话加载期间仍展示旧会话的数据。
    data.value = null
    try {
      const result = await loader(controller.signal)
      if (current === generation) data.value = result
    } catch (e) {
      if (current === generation && !(e instanceof DOMException && e.name === 'AbortError'))
        error.value = errorText(e)
    } finally {
      if (current === generation) pending.value = false
    }
  }
  watch([sessionRevision, ...sources], reload, { immediate: true })
  // 页面销毁后连错误和 loading 状态也不再接受旧请求写入。
  onScopeDispose(() => {
    generation++
    controller?.abort()
  })
  return { data, pending, error, reload }
}
