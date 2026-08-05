<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <span style="color: #606266">项目：</span>
        <el-select v-model="projectId" placeholder="选择项目" style="width: 260px" filterable @change="onProjectChange">
          <el-option v-for="p in projects" :key="p.id" :label="`${p.name}（${p.routePrefix}）`" :value="p.id" />
        </el-select>
        <span style="color: #606266; margin-left: 8px">接口：</span>
        <el-select v-model="apiId" placeholder="选择已上线接口" style="width: 260px" filterable :disabled="!projectId" @change="onApiChange">
          <el-option v-for="a in apis" :key="a.id" :label="`${a.method} ${a.apiPath}（${a.apiName}）`" :value="a.id" />
        </el-select>
        <el-alert v-if="projectId && !apis.length" type="info" :closable="false" style="flex: 1"
          title="该项目暂无已上线接口：请先在「接口管理」中创建并上线接口（上线后最多 30 秒生效）。" />
      </div>
    </el-card>

    <el-row v-if="currentApi" :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <b>请求配置</b>
          </template>
          <div class="url-bar">
            <el-tag :type="methodTag(currentApi.method)" size="small">{{ currentApi.method }}</el-tag>
            <code class="code-text">http://localhost:8080{{ currentProject?.routePrefix }}{{ currentApi.apiPath }}</code>
          </div>

          <el-alert v-if="currentProject?.authType !== 'none'" style="margin: 10px 0" type="warning" :closable="false"
            :title="`该项目开启了${authText(currentProject.authType)}认证，调试时需要在下方请求头中携带认证信息，否则会返回 401。`" />

          <h4>请求参数（JSON）</h4>
          <el-input v-model="paramsJson" type="textarea" :rows="10"
            style="font-family: Consolas, Menlo, monospace"
            placeholder='{"orderId": 1001}' />
          <div style="color: #909399; font-size: 12px; margin-top: 4px">
            {{ currentApi.method === 'GET' || currentApi.method === 'DELETE' ? '将作为 query 参数发送' : '将作为 JSON body 发送' }}
          </div>

          <h4>请求头（JSON，可选）</h4>
          <el-input v-model="headersJson" type="textarea" :rows="3"
            style="font-family: Consolas, Menlo, monospace"
            placeholder='{"Authorization": "Bearer xxx", "X-API-Key": "xxx"}' />

          <el-button type="primary" style="margin-top: 12px; width: 100%" :loading="sending" @click="send">
            发送请求
          </el-button>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <div class="resp-header">
              <b>响应结果</b>
              <span v-if="result" style="margin-left: auto; display: flex; gap: 8px; align-items: center">
                <el-tag size="small" :type="statusColor(result.status)">HTTP {{ result.status }}</el-tag>
                <span style="color: #909399; font-size: 12px">耗时 {{ result.costTime }} ms</span>
              </span>
            </div>
          </template>
          <el-empty v-if="!result" description="填写参数后点击「发送请求」" />
          <template v-else>
            <pre class="resp-body">{{ prettyBody }}</pre>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { projectApi, apiApi, debugApi } from '../api/modules'

const projects = ref([])
const projectId = ref('')
const apis = ref([])
const apiId = ref('')
const currentApi = ref(null)
const paramsJson = ref('{}')
const headersJson = ref('{}')
const sending = ref(false)
const result = ref(null)

const currentProject = computed(() => projects.value.find((p) => p.id === projectId.value))
const AUTH_MAP = { none: ['info', '不鉴权'], token: ['warning', 'Bearer'], apikey: ['primary', 'API Key'] }
const authText = (t) => (AUTH_MAP[t] || AUTH_MAP.none)[1]
const methodTag = (m) => ({ GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }[m] || 'info')

const prettyBody = computed(() => {
  if (!result.value) return ''
  try {
    return JSON.stringify(JSON.parse(result.value.body), null, 2)
  } catch {
    return result.value.body || ''
  }
})

function statusColor(code) {
  if (code >= 200 && code < 300) return 'success'
  if (code >= 400 && code < 500) return 'warning'
  if (code >= 500) return 'danger'
  return 'info'
}

async function onProjectChange() {
  apiId.value = ''
  currentApi.value = null
  result.value = null
  if (!projectId.value) {
    apis.value = []
    return
  }
  const data = await apiApi.page({ page: 1, size: 200, projectId: projectId.value })
  // 只列出已上线接口（草稿/下线无法被 Executor 调用）
  apis.value = (data.records || []).filter((a) => a.status === 1)
}

function onApiChange() {
  result.value = null
  currentApi.value = apis.value.find((a) => a.id === apiId.value) || null
  if (!currentApi.value) return
  // 根据入参定义生成参数示例
  try {
    const arr = JSON.parse(currentApi.value.params || '[]')
    const sample = {}
    if (Array.isArray(arr)) {
      arr.forEach((p) => {
        if (p.defaultValue !== undefined && p.defaultValue !== null) {
          sample[p.name] = p.defaultValue
        } else if (p.type === 'Integer') sample[p.name] = 0
        else if (p.type === 'Float') sample[p.name] = 0
        else if (p.type === 'Boolean') sample[p.name] = false
        else sample[p.name] = ''
      })
    }
    paramsJson.value = JSON.stringify(sample, null, 2)
  } catch {
    paramsJson.value = '{}'
  }
}

async function send() {
  if (!projectId.value || !apiId.value) {
    ElMessage.warning('请先选择项目和接口')
    return
  }
  let params = {}
  let headers = {}
  try {
    params = paramsJson.value.trim() ? JSON.parse(paramsJson.value) : {}
  } catch {
    ElMessage.error('请求参数不是合法 JSON')
    return
  }
  try {
    headers = headersJson.value.trim() ? JSON.parse(headersJson.value) : {}
  } catch {
    ElMessage.error('请求头不是合法 JSON')
    return
  }
  sending.value = true
  try {
    result.value = await debugApi.execute({
      projectId: projectId.value,
      apiId: apiId.value,
      params,
      headers
    })
  } finally {
    sending.value = false
  }
}

onMounted(async () => {
  projects.value = await projectApi.options()
})
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 4px; }
.url-bar { display: flex; align-items: center; gap: 10px; }
.code-text { background: #f5f7fa; padding: 4px 8px; border-radius: 4px; font-size: 13px; }
.resp-header { display: flex; align-items: center; }
h4 { margin: 14px 0 8px; color: #303133; }
.resp-body {
  background: #1f2d3d; color: #d3dce6; padding: 14px; border-radius: 6px;
  font-family: Consolas, Menlo, monospace; font-size: 13px;
  max-height: 480px; overflow: auto; margin: 0; white-space: pre-wrap; word-break: break-all;
}
</style>
