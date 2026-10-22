import { spawnSync } from 'node:child_process'
import path from 'node:path'
const root = path.resolve(import.meta.dirname, '..')
const pnpm = process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm'
// Maven 始终先按锁文件安装，再校验契约/类型并打包；失败立即阻止生成过期静态资源的 JAR。
for (const args of [['install', '--frozen-lockfile'], ['build']]) {
  const result = spawnSync(pnpm, args, {
    cwd: root,
    stdio: 'inherit',
    shell: process.platform === 'win32',
    windowsHide: true,
  })
  if (result.error) {
    console.error('Frontend build requires Node and pnpm on PATH:', result.error.message)
    process.exit(1)
  }
  if (result.status !== 0) process.exit(result.status || 1)
}
