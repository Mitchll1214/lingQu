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
            <code class="code-text">{{ executorBaseUrl || 'http://localhost:8080' }}{{ currentProject?.routePrefix }}{{ currentApi.apiPath }}</code>
          </div>
          <div style="color: #909399; font-size: 12px; margin-top: 4px">
            调试请求由管理端转发到 Executor（{{ executorBaseUrl || 'http://localhost:8080' }}），每次调试前自动刷新 Executor 配置。
          </div>

          <el-alert v-if="currentProject?.authType !== 'none'" style="margin: 10px 0" :closable="false"
            :type="authFilled ? 'success' : 'warning'"
            :title="authFilled
              ? `已自动填入 ${authText(currentProject.authType)} 认证头（取该项目第一个有效密钥），可直接发送。`
              : `该项目开启了${authText(currentProject.authType)}认证：下方请求头已留空，请在「项目管理 → ${currentProject.authType === 'apikey' ? 'API Key' : 'Token'}」中生成密钥后在此填写，否则会返回 401。`" />

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
          <div v-if="result && result.url" class="resp-target">
            实际转发地址：<code>{{ result.url }}</code>
          </div>
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
import { projectApi, apiApi, debugApi, tokenApi } from '../api/modules'

const projects = ref([])
const projectId = ref('')
const apis = ref([])
const apiId = ref('')
const currentApi = ref(null)
const paramsJson = ref('{}')
const headersJson = ref('{}')
const authFilled = ref(false)
const sending = ref(false)
const result = ref(null)
const executorBaseUrl = ref('')

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
  authFilled.value = false
  headersJson.value = '{}'
  if (!projectId.value) {
    apis.value = []
    return
  }
  const data = await apiApi.page({ page: 1, size: 200, projectId: projectId.value })
  // 只列出已上线接口（草稿/下线无法被 Executor 调用）
  apis.value = (data.records || []).filter((a) => a.status === 1)
  // 项目有鉴权时，自动填充认证请求头（取第一个有效 Token/API Key 的明文）
  if (currentProject.value?.authType !== 'none') {
    await autoFillAuthHeader()
  }
}

/** 自动填充鉴权头：拉取该项目有效密钥（status=1 且在起止时间内），解密后填入 headersJson */
async function autoFillAuthHeader() {
  const project = currentProject.value
  if (!project) return
  const list = await tokenApi.list(projectId.value)
  const now = Date.now()
  const valid = (list || []).filter((t) => {
    if (t.status !== 1) return false
    if (t.startAt && new Date(String(t.startAt).replace(' ', 'T')).getTime() > now) return false
    if (t.expireAt && new Date(String(t.expireAt).replace(' ', 'T')).getTime() < now) return false
    return true
  })
  if (!valid.length) return
  try {
    const plain = await tokenApi.reveal(valid[0].id)
    let headers = {}
    try {
      headers = headersJson.value.trim() ? JSON.parse(headersJson.value) : {}
    } catch (e) {
      headers = {}
    }
    if (project.authType === 'apikey') {
      headers['X-API-Key'] = plain
    } else {
      headers['Authorization'] = `Bearer ${plain}`
    }
    headersJson.value = JSON.stringify(headers, null, 2)
    authFilled.value = true
  } catch (e) {
    authFilled.value = false
  }
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
  try {
    executorBaseUrl.value = (await debugApi.executorUrl()) || ''
  } catch (e) {
    executorBaseUrl.value = ''
  }
})
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 4px; }
.url-bar { display: flex; align-items: center; gap: 10px; }
.code-text { background: #f5f7fa; padding: 4px 8px; border-radius: 4px; font-size: 13px; }
.resp-header { display: flex; align-items: center; }
.resp-target { background: #f5f7fa; border-radius: 4px; padding: 6px 10px; font-size: 12px; color: #606266; margin-bottom: 8px; }
.resp-target code { font-family: Consolas, Menlo, monospace; color: #409eff; }
h4 { margin: 14px 0 8px; color: #303133; }
.resp-body {
  background: #1f2d3d; color: #d3dce6; padding: 14px; border-radius: 6px;
  font-family: Consolas, Menlo, monospace; font-size: 13px;
  max-height: 480px; overflow: auto; margin: 0; white-space: pre-wrap; word-break: break-all;
}
</style>
