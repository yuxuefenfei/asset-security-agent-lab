import { computed, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { PageResponse } from '../../generated/contracts'
import type { Query } from '../api/client'
import { useResource } from './useResource'
export function usePagedQuery<T>(
  loader: (query: Query, signal: AbortSignal) => Promise<PageResponse<T>>,
  keys: string[] = [],
) {
  // 已提交查询以路由为准，本地 filters 只是待提交表单，保证后退/前进可恢复列表。
  const route = useRoute(),
    router = useRouter()
  const page = computed(() => Math.max(1, Number(route.query.page) || 1))
  const size = computed(() =>
    [10, 20, 50].includes(Number(route.query.size)) ? Number(route.query.size) : 10,
  )
  const filters = reactive<Record<string, string>>(
    Object.fromEntries(keys.map((k) => [k, String(route.query[k] || '')])),
  )
  watch(
    () => route.query,
    () => {
      keys.forEach((k) => {
        filters[k] = String(route.query[k] || '')
      })
    },
  )
  const resource = useResource(
    (signal) =>
      loader(
        {
          page: page.value,
          size: size.value,
          ...Object.fromEntries(keys.map((k) => [k, String(route.query[k] || '')])),
        },
        signal,
      ),
    [() => route.fullPath],
  )
  // 更换筛选时不沿用旧 page，防止新条件停在超出范围的页码。
  function apply() {
    return router.push({ query: { ...filters, size: String(size.value) } })
  }
  function reset() {
    keys.forEach((k) => {
      filters[k] = ''
    })
    return apply()
  }
  function turn(next: number, nextSize = size.value) {
    return router.push({ query: { ...route.query, page: String(next), size: String(nextSize) } })
  }
  return { ...resource, page, size, filters, apply, reset, turn }
}
