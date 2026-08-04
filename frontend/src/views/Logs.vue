<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.projectId" placeholder="项目" style="width: 200px" clearable filterable @change="onProjectChange">
          <el-option v-for="p in projects" :key="p.id" :label="`${p.name}（${p.code}）`" :value="p.id" />
        </el-select>
        <el-select v-model="query.apiId" placeholder="接口" style="width: 220px" clearable filterable :disabled="!query.projectId">
          <el-option v-for="a in apiOptions" :key="a.id" :label="`${a.method} ${a.apiPath}（${a.apiName}）`" :value="a.id" />
        </el-select>
        <el-date-picker
          v-model="timeRange" type="datetimerange" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间"
          style="width: 360px" value-format="YYYY-MM-DD HH:mm:ss"
        />
        <el-select v-model="query.success" placeholder="状态" style="width: 120px" clearable>
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <el-table :data="rows" v-loading="loading" border stripe size="small">
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="projectCode" label="项目编码" width="100" />
        <el-table-column prop="apiName" label="接口名称" min-width="110" show-overflow-tooltip />
        <el-table-column label="方法" width="70">
          <template #default="{ row }">
            <el-tag size="small" :type="methodTag(row.requestMethod)">{{ row.requestMethod }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestPath" label="路径" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态码" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusColor(row.statusCode)">{{ row.statusCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="90" />
        <el-table-column prop="clientIp" label="客户端IP" width="120" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 12px; justify-content: flex-end"
        layout="total, prev, pager, next, sizes"
        :total="total"
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :page-sizes="[10, 20, 50]"
        @current-change="load"
        @size-change="load"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="调用日志详情" width="720px" top="5vh">
      <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
        <el-descriptions-item label="项目编码">{{ detail?.projectCode }}</el-descriptions-item>
        <el-descriptions-item label="接口名称">{{ detail?.apiName }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ detail?.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="HTTP 状态码">{{ detail?.statusCode }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail?.costTime }} ms</el-descriptions-item>
        <el-descriptions-item label="客户端 IP">{{ detail?.clientIp }}</el-descriptions-item>
        <el-descriptions-item label="请求路径" :span="2">{{ detail?.requestPath }}</el-descriptions-item>
        <el-descriptions-item label="请求时间" :span="2">{{ formatTime(detail?.createdAt) }}</el-descriptions-item>
      </el-descriptions>
      <h4>请求参数</h4>
      <pre class="detail-pre">{{ pretty(detail?.requestParams) }}</pre>
      <h4>响应数据</h4>
      <pre class="detail-pre">{{ pretty(detail?.responseData) }}</pre>
      <template v-if="detail?.errorMsg">
        <h4>错误信息</h4>
        <pre class="detail-pre error">{{ detail.errorMsg }}</pre>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { projectApi, apiApi, logApi } from '../api/modules'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const projects = ref([])
const apiOptions = ref([])
const timeRange = ref(null)
const query = reactive({ page: 1, size: 10, projectId: '', apiId: '', success: null })
const detailVisible = ref(false)
const detail = ref(null)

const methodTag = (m) => ({ GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }[m] || 'info')

function statusColor(code) {
  if (code >= 200 && code < 300) return 'success'
  if (code >= 400 && code < 500) return 'warning'
  if (code >= 500) return 'danger'
  return 'info'
}

function formatTime(v) {
  return v ? String(v).replace('T', ' ').slice(0, 19) : '-'
}

function pretty(s) {
  if (!s) return '-'
  try {
    return JSON.stringify(JSON.parse(s), null, 2)
  } catch {
    return s
  }
}

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (!params.projectId) delete params.projectId
    if (!params.apiId) delete params.apiId
    if (params.success === null || params.success === '') delete params.success
    if (timeRange.value && timeRange.value.length === 2) {
      params.start = timeRange.value[0]
      params.end = timeRange.value[1]
    }
    const data = await logApi.page(params)
    rows.value = data.records || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.assign(query, { page: 1, size: 10, projectId: '', apiId: '', success: null })
  timeRange.value = null
  load()
}

async function onProjectChange() {
  query.apiId = ''
  apiOptions.value = []
  if (query.projectId) {
    const data = await apiApi.page({ page: 1, size: 200, projectId: query.projectId })
    apiOptions.value = data.records || []
  }
}

function showDetail(row) {
  detail.value = row
  detailVisible.value = true
}

onMounted(async () => {
  projects.value = await projectApi.options()
  load()
})
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
h4 { margin: 12px 0 6px; color: #303133; }
.detail-pre {
  background: #f5f7fa; padding: 10px; border-radius: 6px; font-size: 12px;
  font-family: Consolas, Menlo, monospace; max-height: 220px; overflow: auto;
  white-space: pre-wrap; word-break: break-all; margin: 0 0 8px;
}
.detail-pre.error { color: #f56c6c; }
</style>
