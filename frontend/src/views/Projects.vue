<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="按名称/编码检索" style="width: 240px" clearable @keyup.enter="load" />
        <el-select v-model="query.status" placeholder="状态" style="width: 130px" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <div style="flex: 1"></div>
        <el-button type="success" @click="openCreate">新建项目</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="name" label="项目名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="code" label="编码" width="110" />
        <el-table-column prop="routePrefix" label="路由前缀" width="130" />
        <el-table-column prop="department" label="部门" width="90" show-overflow-tooltip />
        <el-table-column label="数据源" width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ datasourceName(row.datasourceId) }}</template>
        </el-table-column>
        <el-table-column label="认证" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="authTag(row.authType)">{{ authText(row.authType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="160">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openTokens(row)">Token</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
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

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑项目' : '新建项目'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="项目名称" required>
          <el-input v-model="form.name" placeholder="如：订单服务" maxlength="100" />
        </el-form-item>
        <el-form-item label="项目编码" required>
          <el-input v-model="form.code" placeholder="全局唯一，如 order" maxlength="50" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="路由前缀" required>
          <el-input v-model="form.routePrefix" placeholder="如 /api/order，全局唯一" maxlength="100" />
        </el-form-item>
        <el-form-item label="所属部门">
          <el-input v-model="form.department" maxlength="100" />
        </el-form-item>
        <el-form-item label="绑定数据源" required>
          <el-select v-model="form.datasourceId" placeholder="选择一个已有数据源" style="width: 100%">
            <el-option v-for="d in datasourceOptions" :key="d.id" :label="`${d.name}（${d.dbType}）`" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="认证方式">
          <el-radio-group v-model="form.authType">
            <el-radio value="none">不鉴权</el-radio>
            <el-radio value="token">Bearer Token</el-radio>
            <el-radio value="apikey">API Key</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- Token 管理 -->
    <el-dialog v-model="tokenVisible" :title="`Token 管理 - ${current?.name || ''}`" width="640px" destroy-on-close>
      <el-form inline>
        <el-form-item label="标识名称">
          <el-input v-model="tokenForm.tokenName" placeholder="如：生产环境" style="width: 160px" />
        </el-form-item>
        <el-form-item label="过期天数">
          <el-input-number v-model="tokenForm.expireDays" :min="0" :max="3650" style="width: 130px" />
          <span style="margin-left: 6px; color: #909399">0 表示永不过期</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="createToken">生成 Token</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="tokens" size="small" border>
        <el-table-column prop="tokenName" label="标识" min-width="120" />
        <el-table-column prop="expireAt" label="过期时间" width="170">
          <template #default="{ row }">{{ row.expireAt ? formatTime(row.expireAt) : '永不过期' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '有效' : '已吊销' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" link type="danger" @click="revokeToken(row)">吊销</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-alert style="margin-top: 10px" type="info" :closable="false"
        title="Token 明文仅在生成时显示一次，请立即复制保存；库中存储的是加密值。" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { projectApi, datasourceApi, tokenApi } from '../api/modules'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', status: null })
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({})
const datasourceOptions = ref([])

const tokenVisible = ref(false)
const current = ref(null)
const tokens = ref([])
const tokenForm = reactive({ tokenName: '', expireDays: 0 })

const AUTH_MAP = { none: ['info', '不鉴权'], token: ['warning', 'Bearer'], apikey: ['primary', 'API Key'] }
const authText = (t) => (AUTH_MAP[t] || AUTH_MAP.none)[1]
const authTag = (t) => (AUTH_MAP[t] || AUTH_MAP.none)[0]

function formatTime(v) {
  return v ? String(v).replace('T', ' ').slice(0, 19) : '-'
}
function datasourceName(id) {
  const d = datasourceOptions.value.find((x) => x.id === id)
  return d ? d.name : '-'
}

async function load() {
  loading.value = true
  try {
    const data = await projectApi.page(query)
    rows.value = data.records || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  form.status = 1
  form.authType = 'none'
  dialogVisible.value = true
}

function openEdit(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row)
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.id) {
      await projectApi.update(form.id, form)
    } else {
      await projectApi.create(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  await projectApi.updateStatus(row.id, target)
  ElMessage.success(target === 1 ? '已启用' : '已禁用')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(
    `确定删除项目「${row.name}」吗？将归档该项目（软删除），且要求其下无已上线接口。`,
    '删除确认',
    { type: 'warning' }
  )
  await projectApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

async function openTokens(row) {
  current.value = row
  tokenVisible.value = true
  tokens.value = await tokenApi.list(row.id)
  tokenForm.tokenName = ''
  tokenForm.expireDays = 0
}

async function createToken() {
  const created = await tokenApi.create({
    projectId: current.value.id,
    tokenName: tokenForm.tokenName,
    expireDays: tokenForm.expireDays
  })
  await ElMessageBox.alert(
    `Token：<b style="color:#e6a23c">${created.token}</b>`,
    '生成成功（仅显示一次）',
    { dangerouslyUseHTMLString: true, confirmButtonText: '我已复制' }
  )
  tokens.value = await tokenApi.list(current.value.id)
}

async function revokeToken(row) {
  await ElMessageBox.confirm(`确定吊销 Token「${row.tokenName}」吗？吊销后立即失效。`, '吊销确认', { type: 'warning' })
  await tokenApi.revoke(row.id)
  ElMessage.success('已吊销')
  tokens.value = await tokenApi.list(current.value.id)
}

onMounted(async () => {
  load()
  datasourceOptions.value = await datasourceApi.options()
})
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; align-items: center; }
</style>
