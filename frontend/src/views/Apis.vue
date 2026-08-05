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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑接口' : '新建接口'" width="860px" top="4vh" destroy-on-close>
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
            :rows="7"
            placeholder="SELECT * FROM t_user WHERE id = #{id}  支持 <if>/<where>/<foreach> 等动态标签"
            style="font-family: Consolas, Menlo, monospace"
            @input="onSqlInput"
          />
        </el-form-item>

        <!-- SQL 参数 ↔ 入参绑定检测 -->
        <div v-if="form.sqlType === 'sql' && sqlParams.length" class="param-bind-box">
          <div class="param-bind-title">SQL 中使用的参数（#{}）：</div>
          <el-tag v-for="p in sqlParams" :key="p" size="small" class="param-chip"
                  :type="paramDefined(p) ? 'success' : 'danger'">
            {{ p }}{{ paramDefined(p) ? '' : '（未定义）' }}
          </el-tag>
          <el-button v-if="missingParams.length" link type="warning" size="small" style="margin-left: 8px"
                     @click="fillMissingParams">
            一键补全入参（{{ missingParams.join(', ') }}）
          </el-button>
        </div>
        <el-alert v-else-if="form.sqlType === 'sql'" type="info" :closable="false" style="margin-bottom: 4px"
          title="在 SQL 中用 #{参数名} 引用入参（如 WHERE id = #{id}），参数会自动绑定到下面的入参表。" />

        <el-form-item label="入参定义">
          <div class="table-wrap">
            <el-table :data="paramRows" size="small" border>
              <el-table-column label="参数名" min-width="130">
                <template #default="{ row }"><el-input v-model="row.name" placeholder="如 id" size="small" /></template>
              </el-table-column>
              <el-table-column label="类型" width="110">
                <template #default="{ row }">
                  <el-select v-model="row.type" size="small" style="width: 100%">
                    <el-option v-for="t in ['String', 'Integer', 'Float', 'Date', 'Boolean', 'Object']" :key="t" :label="t" :value="t" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="必填" width="60">
                <template #default="{ row }">
                  <el-switch v-model="row.required" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="默认值" width="110">
                <template #default="{ row }"><el-input v-model="row.defaultValue" size="small" placeholder="可选" /></template>
              </el-table-column>
              <el-table-column label="说明" min-width="140">
                <template #default="{ row }"><el-input v-model="row.description" size="small" placeholder="可选" /></template>
              </el-table-column>
              <el-table-column label="" width="50">
                <template #default="{ $index }">
                  <el-button link type="danger" size="small" @click="paramRows.splice($index, 1)">删</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-button size="small" style="margin-top: 6px" @click="addParam">+ 添加参数</el-button>
          </div>
        </el-form-item>

        <el-form-item label="出参映射">
          <div class="table-wrap">
            <el-table :data="formatRows" size="small" border>
              <el-table-column label="源字段（SQL 返回列名）" min-width="170">
                <template #default="{ row }"><el-input v-model="row.source" size="small" placeholder="如 user_name" /></template>
              </el-table-column>
              <el-table-column label="输出字段名" min-width="150">
                <template #default="{ row }"><el-input v-model="row.target" size="small" placeholder="留空=同名输出" /></template>
              </el-table-column>
              <el-table-column label="格式化" width="180">
                <template #default="{ row }">
                  <el-input v-model="row.format" size="small" placeholder="如 date:yyyy-MM-dd" />
                </template>
              </el-table-column>
              <el-table-column label="" width="50">
                <template #default="{ $index }">
                  <el-button link type="danger" size="small" @click="formatRows.splice($index, 1)">删</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-button size="small" style="margin-top: 6px" @click="addFormat">+ 添加字段</el-button>
            <div style="color: #909399; font-size: 12px; margin-top: 4px">
              配置后返回结果只保留此处声明的字段；留空则返回 SQL 原始列。
            </div>
          </div>
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
import { ref, reactive, onMounted, computed } from 'vue'
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
const paramRows = ref([])
const formatRows = ref([])

const STATUS_MAP = { 0: ['info', '草稿'], 1: ['success', '已上线'], 2: ['warning', '已下线'] }
const statusText = (s) => (STATUS_MAP[s] || STATUS_MAP[0])[1]
const statusTag = (s) => (STATUS_MAP[s] || STATUS_MAP[0])[0]
const methodTag = (m) => ({ GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }[m] || 'info')

/** 从 SQL 中提取 #{} 参数名 */
const sqlParams = computed(() => {
  const names = []
  const re = /#\{([\w.]+)\}/g
  let m
  while ((m = re.exec(form.sqlContent || ''))) {
    if (!names.includes(m[1])) names.push(m[1])
  }
  return names
})

const missingParams = computed(() => sqlParams.value.filter((p) => !paramDefined(p)))

function paramDefined(name) {
  return paramRows.value.some((r) => r.name && r.name.trim() === name)
}

function onSqlInput() {
  // 仅触发重新计算（computed 已自动），无需额外逻辑
}

function fillMissingParams() {
  missingParams.value.forEach((name) => {
    paramRows.value.push({ name, type: 'String', required: false, defaultValue: '', description: '' })
  })
  ElMessage.success('已补全缺失参数')
}

function addParam() {
  paramRows.value.push({ name: '', type: 'String', required: false, defaultValue: '', description: '' })
}

function addFormat() {
  formatRows.value.push({ source: '', target: '', format: '' })
}

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
  paramRows.value = []
  formatRows.value = []
  dialogVisible.value = true
}

function openEdit(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row)
  logEnabled.value = String(row.logEnabled === 1 ? '1' : '0')
  paramRows.value = parseJsonRows(row.params)
  formatRows.value = parseJsonRows(row.responseFormat)
  dialogVisible.value = true
}

function parseJsonRows(json) {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

async function save() {
  // 校验入参表参数名非空
  for (const r of paramRows.value) {
    if (r.name && r.name.trim()) {
      r.name = r.name.trim()
    }
  }
  if (paramRows.value.some((r) => !r.name)) {
    ElMessage.error('入参定义中存在空的参数名，请删除空行或填写参数名')
    return
  }
  if (formatRows.value.some((r) => !r.source)) {
    ElMessage.error('出参映射中存在空的源字段，请删除空行或填写源字段名')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, logEnabled: Number(logEnabled.value) }
    payload.params = paramRows.value.length ? JSON.stringify(paramRows.value) : ''
    payload.responseFormat = formatRows.value.length ? JSON.stringify(formatRows.value) : ''
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
.param-bind-box {
  background: #f5f7fa; border: 1px dashed #dcdfe6; border-radius: 6px;
  padding: 8px 12px; margin: -6px 0 14px 100px; display: flex; align-items: center; flex-wrap: wrap; gap: 6px;
}
.param-bind-title { color: #606266; font-size: 13px; }
.param-chip { font-family: Consolas, Menlo, monospace; }
.table-wrap { width: 100%; }
</style>
