<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.projectId" placeholder="选择项目" style="width: 220px" clearable @change="load">
          <el-option v-for="p in projectOptions" :key="p.id" :label="`${p.name}（${p.routePrefix}）`" :value="p.id" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="按名称/路径检索" style="width: 200px" clearable @keyup.enter="load" />
        <el-select v-model="query.status" placeholder="状态" style="width: 130px" clearable>
          <el-option label="草稿" :value="0" />
          <el-option label="已上线" :value="1" />
          <el-option label="已下线" :value="2" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <div style="flex: 1"></div>
        <el-button type="success" :disabled="!query.projectId" @click="openCreate">新建接口</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="apiName" label="接口名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="apiPath" label="接口路径" min-width="120" show-overflow-tooltip />
        <el-table-column label="方法" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="methodTag(row.method)">{{ row.method }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sqlType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.sqlType === 'groovy' ? 'warning' : 'info'">{{ row.sqlType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="日志" width="70">
          <template #default="{ row }">
            <el-tag size="small" :type="row.logEnabled === 1 ? 'success' : 'info'">{{ row.logEnabled === 1 ? '开' : '关' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="QPS" width="80">
          <template #default="{ row }">{{ Number(row.rateLimitQps) > 0 ? row.rateLimitQps : '不限' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status !== 1" link type="success" @click="changeStatus(row, 1)">上线</el-button>
            <el-button v-else link type="warning" @click="changeStatus(row, 2)">下线</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑接口' : '新建接口'" width="760px" top="5vh" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="接口名称" required>
              <el-input v-model="form.apiName" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="接口路径" required>
              <el-input v-model="form.apiPath" placeholder="/getDetail（项目内唯一）" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="请求方法" required>
              <el-select v-model="form.method" style="width: 100%">
                <el-option v-for="m in ['GET', 'POST', 'PUT', 'DELETE']" :key="m" :label="m" :value="m" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="脚本类型">
          <el-radio-group v-model="form.sqlType">
            <el-radio value="sql">MyBatis 动态 SQL</el-radio>
            <el-radio value="groovy">Groovy 脚本</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="SQL / 脚本" required>
          <el-input
            v-model="form.sqlContent"
            type="textarea"
            :rows="9"
            placeholder="SELECT * FROM t_user WHERE id = #{id}  支持 <if>/<where>/<foreach>/<choose>/<set>/<trim>"
            style="font-family: Consolas, Menlo, monospace"
          />
        </el-form-item>
        <el-form-item label="入参定义">
          <el-input
            v-model="form.params"
            type="textarea"
            :rows="3"
            placeholder='[{"name":"id","type":"Integer","required":true,"defaultValue":null,"description":"主键"}]'
            style="font-family: Consolas, Menlo, monospace"
          />
        </el-form-item>
        <el-form-item label="出参映射">
          <el-input
            v-model="form.responseFormat"
            type="textarea"
            :rows="2"
            placeholder='[{"source":"user_name","target":"userName"}]  （可选）'
            style="font-family: Consolas, Menlo, monospace"
          />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="日志开关">
              <el-switch v-model="logEnabled" active-value="1" inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="QPS 限制">
              <el-input-number v-model="form.rateLimitQps" :min="0" :max="100000" :precision="2" />
              <span style="margin-left: 6px; color: #909399">0=不限</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-tag :type="statusTag(form.status)">{{ statusText(form.status) }}</el-tag>
              <span v-if="form.id && form.status === 1" style="margin-left: 6px; color: #e6a23c; font-size: 12px">编辑后需重新上线</span>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiApi, projectApi } from '../api/modules'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, projectId: '', keyword: '', status: null })
const projectOptions = ref([])
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({})
const logEnabled = ref('0')

const STATUS_MAP = { 0: ['info', '草稿'], 1: ['success', '已上线'], 2: ['warning', '已下线'] }
const statusText = (s) => (STATUS_MAP[s] || STATUS_MAP[0])[1]
const statusTag = (s) => (STATUS_MAP[s] || STATUS_MAP[0])[0]
const methodTag = (m) => ({ GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }[m] || 'info')

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (!params.projectId) delete params.projectId
    if (params.status === null || params.status === '') delete params.status
    const data = await apiApi.page(params)
    rows.value = data.records || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  form.projectId = query.projectId
  form.method = 'GET'
  form.sqlType = 'sql'
  form.status = 0
  form.logEnabled = 0
  form.rateLimitQps = 0
  logEnabled.value = '0'
  dialogVisible.value = true
}

function openEdit(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row)
  logEnabled.value = String(row.logEnabled === 1 ? '1' : '0')
  dialogVisible.value = true
}

async function save() {
  if (form.params && form.params.trim()) {
    try {
      JSON.parse(form.params)
    } catch (e) {
      ElMessage.error('入参定义不是合法 JSON')
      return
    }
  }
  if (form.responseFormat && form.responseFormat.trim()) {
    try {
      JSON.parse(form.responseFormat)
    } catch (e) {
      ElMessage.error('出参映射不是合法 JSON')
      return
    }
  }
  saving.value = true
  try {
    const payload = { ...form, logEnabled: Number(logEnabled.value) }
    if (form.id) {
      await apiApi.update(form.id, payload)
    } else {
      await apiApi.create(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function changeStatus(row, target) {
  const text = target === 1 ? '上线' : '下线'
  if (target === 2) {
    await ElMessageBox.confirm(`确定下线接口「${row.apiName}」吗？下线后外部将无法访问。`, '下线确认', { type: 'warning' })
  }
  await apiApi.updateStatus(row.id, target)
  ElMessage.success(`已${text}`)
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(
    `确定删除接口「${row.apiName}」吗？仅草稿/下线状态可删除。`,
    '删除确认',
    { type: 'warning' }
  )
  await apiApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  projectOptions.value = await projectApi.options()
  load()
})
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; align-items: center; }
</style>
