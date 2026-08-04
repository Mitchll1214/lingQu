<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <span style="color: #606266">选择项目：</span>
        <el-select v-model="projectId" placeholder="选择项目查看文档" style="width: 300px" clearable filterable @change="onProjectChange">
          <el-option v-for="p in projects" :key="p.id" :label="`${p.name}（${p.routePrefix}）`" :value="p.id" />
        </el-select>
        <el-alert v-if="!projectId" style="flex: 1" type="info" :closable="false"
          title="选择一个项目后，自动生成该项目的接口文档（路径、方法、入参、出参、curl 示例）。" />
      </div>
    </el-card>

    <el-row v-if="projectId" :gutter="16" style="margin-top: 16px">
      <el-col :span="8">
        <el-card shadow="never" style="height: 100%">
          <template #header>
            <b>接口列表（{{ apis.length }}）</b>
          </template>
          <div v-for="a in apis" :key="a.id" class="api-item" :class="{ active: current?.id === a.id }" @click="current = a">
            <el-tag size="small" :type="methodTag(a.method)" style="width: 56px; text-align: center">{{ a.method }}</el-tag>
            <div class="api-item-body">
              <div class="api-item-name">{{ a.apiName }}</div>
              <div class="api-item-path">{{ a.apiPath }}</div>
            </div>
            <el-tag size="small" :type="statusTag(a.status)" style="flex-shrink: 0">{{ statusText(a.status) }}</el-tag>
          </div>
          <el-empty v-if="apis.length === 0" description="该项目暂无接口" />
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card v-if="current" shadow="never">
          <template #header>
            <div class="doc-header">
              <b>{{ current.apiName }}</b>
              <span style="margin-left: 12px">
                <el-tag size="small" :type="methodTag(current.method)">{{ current.method }}</el-tag>
                <el-tag size="small" style="margin-left: 6px">{{ current.apiPath }}</el-tag>
                <el-tag size="small" type="info" style="margin-left: 6px">v{{ current.version }}</el-tag>
              </span>
            </div>
          </template>

          <el-descriptions :column="2" border size="small" style="margin-bottom: 16px">
            <el-descriptions-item label="完整调用地址">
              <code class="code-text">http://{host}:8080{{ currentProject?.routePrefix }}{{ current.apiPath }}</code>
            </el-descriptions-item>
            <el-descriptions-item label="认证方式">
              <el-tag size="small" :type="authTag(currentProject?.authType)">{{ authText(currentProject?.authType) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="脚本类型">{{ current.sqlType }}</el-descriptions-item>
            <el-descriptions-item label="日志开关">{{ current.logEnabled === 1 ? '开启' : '关闭' }}</el-descriptions-item>
            <el-descriptions-item label="QPS 限制">{{ Number(current.rateLimitQps) > 0 ? current.rateLimitQps : '不限' }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatTime(current.updatedAt) }}</el-descriptions-item>
          </el-descriptions>

          <h4>入参定义</h4>
          <el-table :data="paramRows" border size="small" empty-text="无入参定义">
            <el-table-column prop="name" label="参数名" width="140" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="required" label="必填" width="70">
              <template #default="{ row }">{{ row.required ? '是' : '否' }}</template>
            </el-table-column>
            <el-table-column prop="defaultValue" label="默认值" width="110">
              <template #default="{ row }">{{ row.defaultValue ?? '-' }}</template>
            </el-table-column>
            <el-table-column prop="description" label="说明" />
          </el-table>

          <h4 v-if="formatRows.length">出参映射</h4>
          <el-table v-if="formatRows.length" :data="formatRows" border size="small" style="margin-bottom: 8px">
            <el-table-column prop="source" label="原字段" />
            <el-table-column prop="target" label="输出字段" />
            <el-table-column prop="format" label="格式化">
              <template #default="{ row }">{{ row.format || '-' }}</template>
            </el-table-column>
          </el-table>

          <h4>请求示例（curl）</h4>
          <el-input :model-value="curlExample" type="textarea" :rows="6" readonly
            style="font-family: Consolas, Menlo, monospace; margin-bottom: 16px" />

          <h4>SQL / 脚本预览</h4>
          <el-input :model-value="current.sqlContent" type="textarea" :rows="6" readonly
            style="font-family: Consolas, Menlo, monospace" />
        </el-card>
        <el-card v-else shadow="never">
          <el-empty description="从左侧选择一个接口查看文档" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { projectApi, apiApi } from '../api/modules'

const projects = ref([])
const projectId = ref('')
const apis = ref([])
const current = ref(null)

const currentProject = computed(() => projects.value.find((p) => p.id === projectId.value))

const AUTH_MAP = { none: ['info', '不鉴权'], token: ['warning', 'Bearer Token'], apikey: ['primary', 'API Key'] }
const authText = (t) => (AUTH_MAP[t] || AUTH_MAP.none)[1]
const authTag = (t) => (AUTH_MAP[t] || AUTH_MAP.none)[0]
const STATUS_MAP = { 0: ['info', '草稿'], 1: ['success', '已上线'], 2: ['warning', '已下线'] }
const statusText = (s) => (STATUS_MAP[s] || STATUS_MAP[0])[1]
const statusTag = (s) => (STATUS_MAP[s] || STATUS_MAP[0])[0]
const methodTag = (m) => ({ GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }[m] || 'info')

const paramRows = computed(() => {
  if (!current.value?.params) return []
  try {
    const arr = JSON.parse(current.value.params)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
})

const formatRows = computed(() => {
  if (!current.value?.responseFormat) return []
  try {
    const arr = JSON.parse(current.value.responseFormat)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
})

const curlExample = computed(() => {
  if (!current.value) return ''
  const method = current.value.method
  const url = `http://localhost:8080${currentProject.value?.routePrefix || ''}${current.value.apiPath}`
  const lines = [`curl -X ${method} '${url}'`, `  -H 'Content-Type: application/json'`]
  const authType = currentProject.value?.authType
  if (authType === 'token') lines.push(`  -H 'Authorization: Bearer <你的Token>'`)
  if (authType === 'apikey') lines.push(`  -H 'X-API-Key: <你的ApiKey>'`)
  if ((method === 'POST' || method === 'PUT') && paramRows.value.length) {
    const sample = {}
    paramRows.value.forEach((p) => {
      if (p.defaultValue !== undefined && p.defaultValue !== null) sample[p.name] = p.defaultValue
      else sample[p.name] = p.type === 'Integer' || p.type === 'Float' ? 0 : ''
    })
    lines.push(`  -d '${JSON.stringify(sample)}'`)
  }
  return lines.join(' \\\n')
})

function formatTime(v) {
  return v ? String(v).replace('T', ' ').slice(0, 19) : '-'
}

async function onProjectChange() {
  current.value = null
  if (!projectId.value) {
    apis.value = []
    return
  }
  const data = await apiApi.page({ page: 1, size: 200, projectId: projectId.value })
  apis.value = data.records || []
}

onMounted(async () => {
  projects.value = await projectApi.options()
})
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; align-items: center; }
.api-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 6px; cursor: pointer; margin-bottom: 6px;
  border: 1px solid #ebeef5;
}
.api-item:hover { background: #f5f7fa; }
.api-item.active { background: #ecf5ff; border-color: #409eff; }
.api-item-body { flex: 1; min-width: 0; }
.api-item-name { font-size: 14px; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.api-item-path { font-size: 12px; color: #909399; }
.code-text { background: #f5f7fa; padding: 2px 6px; border-radius: 4px; font-size: 12px; }
.doc-header { display: flex; align-items: center; }
h4 { margin: 12px 0 8px; color: #303133; }
</style>
