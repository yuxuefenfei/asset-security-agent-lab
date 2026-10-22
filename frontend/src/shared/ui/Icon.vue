<script setup lang="ts">
withDefaults(defineProps<{ name: string; size?: number }>(), { size: 20 })
const paths: Record<string, string> = {
  grid: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
  server: 'M4 3h16v7H4z M4 14h16v7H4z M7 6h.01 M7 17h.01 M11 6h6 M11 17h6',
  scan: 'M4 8V4h4 M16 4h4v4 M20 16v4h-4 M8 20H4v-4 M8 12h8 M12 8v8',
  file: 'M14 2H5v20h14V7z M14 2v6h5 M8 12h8 M8 16h8',
  ticket: 'M4 3h16v18l-4-2-4 2-4-2-4 2z M8 8h8 M8 12h8',
  book: 'M3 4c4-1 6-1 9 1 3-2 5-2 9-1v16c-4-1-6-1-9 1-3-2-5-2-9-1z M12 5v16',
  settings:
    'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8 M12 2v3 M12 19v3 M2 12h3 M19 12h3 M5 5l2 2 M17 17l2 2 M5 19l2-2 M17 7l2-2',
  shield: 'm12 2 9 4v6c0 6-9 10-9 10S3 18 3 12V6z M8 12l3 3 5-6',
  arrow: 'M4 12h16 M14 6l6 6-6 6',
  chevron: 'm9 5 7 7-7 7',
  plus: 'M12 5v14 M5 12h14',
  search: 'M11 3a8 8 0 1 0 0 16 8 8 0 0 0 0-16 M17 17l5 5',
  refresh: 'M20 8a9 9 0 1 0 1 7 M20 2v6h-6',
  check: 'm4 12 5 5L20 6',
  alert: 'm12 3 10 18H2z M12 9v5 M12 17h.01',
  clock: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18 M12 7v5l3 2',
  layers: 'm12 3 10 5-10 5L2 8z M2 12l10 5 10-5 M2 16l10 5 10-5',
  menu: 'M3 6h18 M3 12h18 M3 18h18',
  close: 'm6 6 12 12 M6 18 18 6',
  code: 'm8 6-6 6 6 6 M16 6l6 6-6 6 M14 3l-4 18',
}
</script>
<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.6"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path :d="paths[name] || paths.file" />
  </svg>
</template>
