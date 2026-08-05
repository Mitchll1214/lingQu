<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="按名称/JDBC URL 检索" style="width: 260px" clearable @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <div style="flex: 1"></div>
        <el-button type="success" @click="openCreate">新建数据源</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="name" label="名称" min-width="130" show-overflow-tooltip />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ row.dbType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="jdbcUrl" label="JDBC URL" min-width="240" show-overflow-tooltip />
        <el-table-column prop="username" label="用户名" width="110" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '可用' : '不可用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" :loading="testingId === row.id" @click="test(row)">测试连接</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑数据源' : '新建数据源'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="110px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="数据库类型" required>
          <el-select v-model="form.dbType" style="width: 100%" @change="onTypeChange">
            <el-option label="MySQL" value="mysql" />
            <el-option label="PostgreSQL" value="postgresql" />
            <el-option label="其他（Oracle/SQLServer/达梦等，走高级配置）" value="other" />
          </el-select>
        </el-form-item>

        <!-- 基础模式：MySQL / PostgreSQL -->
        <template v-if="isSimpleType">
          <el-row :gutter="12">
            <el-col :span="16">
              <el-form-item label="主机地址" required>
                <el-input v-model="form.host" placeholder="如 192.168.1.10 或 mysql.example.com" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="端口" required>
                <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="数据库名称" required>
            <el-input v-model="form.dbName" placeholder="如 mydb（JDBC URL 会自动生成）" />
          </el-form-item>
        </template>

        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码" :required="!form.id">
          <el-input v-model="form.password" type="password" show-password :placeholder="form.id ? '留空表示不修改' : ''" />
        </el-form-item>

        <el-collapse style="border: none">
          <el-collapse-item title="高级配置（驱动类名 / JDBC URL 覆盖 / 连接池）" name="adv">
            <el-form-item v-if="isSimpleType" label="JDBC URL">
              <el-input v-model="form.jdbcUrl" @input="urlTouched = true" />
              <div style="color: #909399; font-size: 12px; line-height: 1.5; margin-top: 4px">
                根据上方 主机/端口/库名 自动生成，如需追加连接参数（如字符集）可直接修改此处。
              </div>
            </el-form-item>
            <el-form-item v-else label="JDBC URL" required>
              <el-input v-model="form.jdbcUrl" placeholder="jdbc:oracle:thin:@host:1521:xe" />
            </el-form-item>
            <el-form-item label="驱动类名">
              <el-input v-model="form.driverClass" :placeholder="isSimpleType ? 'mysql/postgresql 自动识别，可不填' : '必填，如 oracle.jdbc.OracleDriver'" />
            </el-form-item>
            <el-form-item label="连接池大小">
              <el-input-number v-model="poolSize" :min="1" :max="100" />
            </el-form-item>
          </el-collapse-item>
        </el-collapse>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { datasourceApi } from '../api/modules'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })
const dialogVisible = ref(false)
const saving = ref(false)
const testingId = ref(null)
const form = reactive({})
const poolSize = ref(10)
const urlTouched = ref(false)

const SIMPLE_TYPES = ['mysql', 'postgresql']
const isSimpleType = computed(() => SIMPLE_TYPES.includes(form.dbType))

const DEFAULT_PORTS = { mysql: 3306, postgresql: 5432 }

function buildJdbcUrl() {
  if (!isSimpleType.value) return form.jdbcUrl || ''
  const host = (form.host || '').trim()
  const port = form.port || DEFAULT_PORTS[form.dbType] || 3306
  const db = (form.dbName || '').trim()
  if (form.dbType === 'postgresql') {
    return `jdbc:postgresql://${host}:${port}/${db}`
  }
  return `jdbc:mysql://${host}:${port}/${db}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true`
}

// 基础字段变化时自动同步 JDBC URL（除非用户手动改过高级 URL）
watch(
  [() => form.host, () => form.port, () => form.dbName, () => form.dbType],
  () => {
    if (!urlTouched.value) {
      form.jdbcUrl = buildJdbcUrl()
    }
  }
)

function onTypeChange() {
  urlTouched.value = false
  form.port = DEFAULT_PORTS[form.dbType] || 3306
  if (!isSimpleType.value) {
    form.driverClass = ''
  }
}

/** 从已有 JDBC URL 解析出 主机/端口/库名，便于回填基础表单 */
function parseJdbcUrl(url) {
  const m = String(url || '').match(/^jdbc:(\w+):\/\/([^:/]+):?(\d*)\/([^?]+)/)
  if (!m) return null
  const type = m[1] === 'postgresql' || m[1] === 'postgres' ? 'postgresql' : m[1] === 'mysql' ? 'mysql' : null
  if (!type) return null
  return { dbType: type, host: m[2], port: Number(m[3]) || DEFAULT_PORTS[type], dbName: m[4] }
}

async function load() {
  loading.value = true
  try {
    const data = await datasourceApi.page(query)
    rows.value = data.records || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  form.dbType = 'mysql'
  form.port = 3306
  form.status = 1
  urlTouched.value = false
  poolSize.value = 10
  form.jdbcUrl = buildJdbcUrl()
  dialogVisible.value = true
}

function openEdit(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row)
  form.password = ''
  poolSize.value = 10
  try {
    const cfg = row.poolConfig ? JSON.parse(row.poolConfig) : {}
    if (cfg.poolSize) poolSize.value = Number(cfg.poolSize)
  } catch (e) { /* 忽略 */ }

  const parsed = parseJdbcUrl(row.jdbcUrl)
  if (parsed) {
    form.dbType = parsed.dbType
    form.host = parsed.host
    form.port = parsed.port
    form.dbName = parsed.dbName
    urlTouched.value = false
    form.jdbcUrl = buildJdbcUrl()
  } else {
    form.dbType = 'other'
    urlTouched.value = true
  }
  dialogVisible.value = true
}

async function save() {
  if (isSimpleType.value) {
    if (!form.host || !form.port || !form.dbName) {
      ElMessage.warning('请填写主机地址、端口和数据库名称')
      return
    }
  } else if (!form.jdbcUrl) {
    ElMessage.warning('请填写 JDBC URL（其他类型需手工填写）')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, poolConfig: JSON.stringify({ poolSize: poolSize.value }) }
    if (form.id) {
      await datasourceApi.update(form.id, payload)
    } else {
      await datasourceApi.create(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function test(row) {
  testingId.value = row.id
  try {
    const msg = await datasourceApi.test(row.id)
    ElMessageBox.alert(msg, '连接测试', { type: 'success' })
  } finally {
    testingId.value = null
  }
}

async function remove(row) {
  await ElMessageBox.confirm(
    `确定删除数据源「${row.name}」吗？若有项目绑定将无法删除。`,
    '删除确认',
    { type: 'warning' }
  )
  await datasourceApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; align-items: center; }
:deep(.el-collapse-item__header) { border: none; color: #409eff; font-size: 13px; }
:deep(.el-collapse-item__wrap) { border: none; }
</style>
