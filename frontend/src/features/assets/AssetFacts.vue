<script setup lang="ts">
import type { AssetResponse } from '../../generated/contracts'
import Badge from '../../shared/ui/Badge.vue'
defineProps<{ asset: AssetResponse }>()
</script>
<template>
  <dl class="facts-grid">
    <div>
      <dt>IP 地址</dt>
      <dd class="mono">{{ asset.ip }}</dd>
    </div>
    <div>
      <dt>主机名称</dt>
      <dd>{{ asset.hostname }}</dd>
    </div>
    <div>
      <dt>所属业务</dt>
      <dd>{{ asset.business }}</dd>
    </div>
    <div>
      <dt>业务负责人</dt>
      <dd>{{ asset.owner }}</dd>
    </div>
    <div>
      <dt>操作系统</dt>
      <dd>{{ asset.os }}</dd>
    </div>
    <div>
      <dt>业务重要性</dt>
      <dd>
        <Badge
          :value="asset.criticality"
          :text="{ HIGH: '高', MEDIUM: '中', LOW: '低' }[asset.criticality]"
        />
      </dd>
    </div>
    <div>
      <dt>互联网暴露</dt>
      <dd>{{ asset.internetExposed ? '存在公网暴露' : '仅内网访问' }}</dd>
    </div>
    <div>
      <dt>开放端口</dt>
      <dd class="port-list">
        <code v-for="port in asset.ports" :key="port">{{ port }}</code>
      </dd>
    </div>
  </dl>
</template>
