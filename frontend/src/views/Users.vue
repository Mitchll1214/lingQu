<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="按用户名检索" style="width: 240px" clearable @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <div style="flex: 1"></div>
        <el-button type="success" @click="openCreate">新建用户</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.role === 'ADMIN' ? 'danger' : 'info'">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <template v-if="row.role !== 'ADMIN'">
              <el-button link type="primary" @click="openProjects(row)">绑定项目</el-button>
              <el-button link type="warning" @click="resetPassword(row)">重置密码</el-button>
              <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
            </template>
            <span v-else style="color: #909399; font-size: 12px">管理员（环境变量固定）</span>
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

    <!-- 新建用户 -->
    <el-dialog v-model="createVisible" title="新建用户" width="460px" destroy-on-close>
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="用户名" required>
          <el-input v-model="createForm.username" maxlength="50" placeholder="登录账号，唯一" />
        </el-form-item>
        <el-form-item label="初始密码">
          <el-input v-model="createForm.password" placeholder="留空则使用默认密码 88888888" maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createUser">创建</el-button>
      </template>
    </el-dialog>

    <!-- 绑定项目 -->
    <el-dialog v-model="projectsVisible" :title="`绑定项目 - ${currentUser?.username || ''}`" width="520px" destroy-on-close>
      <el-alert type="info" :closable="false" style="margin-bottom: 10px"
        title="普通用户登录后仅能查看/维护此处勾选的项目（接口、日志、调试等）。" />
      <el-select v-model="selectedProjects" multiple filterable style="width: 100%" placeholder="选择可访问的项目">
        <el-option v-for="p in projectOptions" :key="p.id" :label="`${p.name}（${p.routePrefix}）`" :value="p.id" />
      </el-select>
      <template #footer>
        <el-button @click="projectsVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingProjects" @click="saveProjects">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi, projectApi } from '../api/modules'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })
const createVisible = ref(false)
const creating = ref(false)
const createForm = ref({ username: '', password: '' })
const projectsVisible = ref(false)
const savingProjects = ref(false)
const currentUser = ref(null)
const selectedProjects = ref([])
const projectOptions = ref([])

function formatTime(v) {
  return v ? String(v).replace('T', ' ').slice(0, 19) : '-'
}

async function load() {
  loading.value = true
  try {
    const data = await userApi.page(query)
    rows.value = data.records || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  createForm.value = { username: '', password: '' }
  createVisible.value = true
}

async function createUser() {
  if (!createForm.value.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  creating.value = true
  try {
    await userApi.create(createForm.value)
    ElMessage.success('用户创建成功（默认密码 88888888，请及时绑定项目权限）')
    createVisible.value = false
    load()
  } finally {
    creating.value = false
  }
}

async function openProjects(row) {
  currentUser.value = row
  selectedProjects.value = await userApi.userProjects(row.id)
  if (!projectOptions.value.length) {
    projectOptions.value = await projectApi.options()
  }
  projectsVisible.value = true
}

async function saveProjects() {
  savingProjects.value = true
  try {
    await userApi.updateProjects(currentUser.value.id, selectedProjects.value)
    ElMessage.success('项目权限已更新')
    projectsVisible.value = false
  } finally {
    savingProjects.value = false
  }
}

async function resetPassword(row) {
  await ElMessageBox.confirm(
    `确定将用户「${row.username}」的密码重置为默认密码 88888888 吗？`,
    '重置密码',
    { type: 'warning' }
  )
  await userApi.resetPassword(row.id)
  ElMessage.success('已重置为 88888888')
}

async function toggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确定${target === 1 ? '启用' : '禁用'}用户「${row.username}」吗？`, '提示', { type: 'warning' })
  await userApi.updateStatus(row.id, target)
  ElMessage.success(target === 1 ? '已启用' : '已禁用')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; align-items: center; }
</style>
