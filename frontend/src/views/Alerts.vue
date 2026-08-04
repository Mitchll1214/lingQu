<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="按规则名称检索" style="width: 240px" clearable @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <div style="flex: 1"></div>
        <el-button type="success" @click="openCreate">新建规则</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="name" label="规则名称" min-width="130" show-overflow-tooltip />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.alertType === 'timeout' ? 'warning' : 'danger'">
              {{ row.alertType === 'timeout' ? '响应超时' : '错误率' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="阈值" width="110">
          <template #default="{ row }">
            {{ row.threshold }}{{ row.alertType === 'timeout' ? ' 秒' : '%' }}
          </template>
        </el-table-column>
        <el-table-column label="窗口" width="90">
          <template #default="{ row }">{{ row.windowMinutes }} 分钟</template>
        </el-table-column>
        <el-table-column label="静默" width="90">
          <template #default="{ row }">{{ row.silenceMinutes }} 分钟</template>
        </el-table-column>
        <el-table-column label="作用范围" width="110">
          <template #default="{ row }">{{ projectName(row.projectId) }}</template>
        </el-table-column>
        <el-table-column prop="mailTo" label="收件人" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.mailTo || '（全局默认）' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上次告警" width="160">
          <template #default="{ row }">{{ row.lastAlertAt ? formatTime(row.lastAlertAt) : '从未' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑规则' : '新建规则'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="110px">
        <el-form-item label="规则名称" required>
          <el-input v-model="form.name" maxlength="100" placeholder="如：订单接口超时告警" />
        </el-form-item>
        <el-form-item label="规则类型" required>
          <el-radio-group v-model="form.alertType">
            <el-radio value="timeout">响应超时（平均耗时 > 阈值）</el-radio>
            <el-radio value="error_rate">错误率（失败占比 > 阈值）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="form.alertType === 'timeout' ? '耗时阈值(秒)' : '错误率阈值(%)'" required>
          <el-input-number v-model="form.threshold" :min="0.1" :max="3600" :precision="2" />
        </el-form-item>
        <el-form-item label="统计窗口(分钟)">
          <el-input-number v-model="form.windowMinutes" :min="1" :max="1440" />
        </el-form-item>
        <el-form-item label="静默时间(分钟)">
          <el-input-number v-model="form.silenceMinutes" :min="1" :max="10080" />
          <span style="margin-left: 6px; color: #909399">静默期内同一规则不重复告警</span>
        </el-form-item>
        <el-form-item label="作用范围">
          <el-select v-model="form.projectId" placeholder="全局（所有项目）" style="width: 100%" clearable>
            <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="收件人">
          <el-input v-model="form.mailTo" placeholder="多个用逗号分隔，留空用全局默认（ALERT_MAIL_TO）" />
        </el-form-item>
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
import { alertApi, projectApi } from '../api/modules'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({})
const projects = ref([])

function formatTime(v) {
  return v ? String(v).replace('T', ' ').slice(0, 19) : '-'
}
function projectName(id) {
  if (!id) return '全局'
  const p = projects.value.find((x) => x.id === id)
  return p ? p.name : id
}

async function load() {
  loading.value = true
  try {
    const data = await alertApi.page(query)
    rows.value = data.records || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  form.alertType = 'timeout'
  form.threshold = 3
  form.windowMinutes = 5
  form.silenceMinutes = 10
  form.status = 1
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
      await alertApi.update(form.id, form)
    } else {
      await alertApi.create(form)
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
  await alertApi.updateStatus(row.id, target)
  ElMessage.success(target === 1 ? '已启用' : '已禁用')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除告警规则「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await alertApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  load()
  projects.value = await projectApi.options()
})
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; align-items: center; }
</style>
