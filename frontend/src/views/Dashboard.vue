<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value">{{ card.value }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>项目调用量排行（TOP 5）</template>
      <el-table :data="topProjects" empty-text="暂无调用日志">
        <el-table-column prop="project_code" label="项目编码" />
        <el-table-column prop="cnt" label="调用次数" width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { dashboardApi } from '../api/modules'

const cards = ref([
  { label: '项目总数', value: 0 },
  { label: '接口总数', value: 0 },
  { label: '数据源总数', value: 0 },
  { label: '今日调用', value: 0 }
])
const topProjects = ref([])

onMounted(async () => {
  const stats = await dashboardApi.stats()
  cards.value = [
    { label: '项目总数', value: stats.projectTotal ?? 0 },
    { label: '接口总数', value: stats.apiTotal ?? 0 },
    { label: '数据源总数', value: stats.datasourceTotal ?? 0 },
    { label: '今日调用', value: stats.logToday ?? 0 }
  ]
  topProjects.value = stats.topProjects || []
})
</script>

<style scoped>
.stat-item { text-align: center; padding: 8px 0; }
.stat-label { color: #909399; font-size: 13px; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 600; color: #1f3b73; }
</style>
