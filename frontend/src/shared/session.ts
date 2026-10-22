import { ref } from 'vue'
import { config } from './config'
function readToken() {
  try {
    return sessionStorage.getItem('lab-token') ?? config.defaultToken
  } catch {
    return config.defaultToken
  }
}
export const token = ref(readToken())
// 会话代次通知所有资源/命令失效；即使设置相同口令也会触发重新校验。
export const sessionRevision = ref(0)
export function setToken(value: string) {
  token.value = value.trim()
  try {
    sessionStorage.setItem('lab-token', token.value)
  } catch {
    /* Private browser mode may disable storage. */
  }
  sessionRevision.value++
}
