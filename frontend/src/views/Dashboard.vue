<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value">{{ card.value }}</div>
            <div v-if="card.sub" class="stat-sub">{{ card.sub }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>近 7 日调用趋势</template>
      <div v-if="trend.length" class="trend">
        <div v-for="t in trend" :key="t.day" class="trend-item">
          <div class="trend-bar" :style="{ height: barHeight(t.cnt) }">
            <span class="trend-cnt">{{ t.cnt }}</span>
          </div>
          <div class="trend-day">{{ shortDay(t.day) }}</div>
        </div>
      </div>
      <el-empty v-else description="暂无调用日志" :image-size="60" />
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>项目调用量排行（TOP 5）</template>
          <el-table :data="topProjects" empty-text="暂无调用日志" size="small">
            <el-table-column prop="project_code" label="项目编码" />
            <el-table-column prop="cnt" label="调用次数" width="140" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>接口调用量排行（TOP 5）</template>
          <el-table :data="topApis" empty-text="暂无调用日志" size="small">
            <el-table-column prop="api_name" label="接口名称" />
            <el-table-column prop="cnt" label="调用次数" width="140" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { dashboardApi } from '../api/modules'

const cards = ref([])
const topProjects = ref([])
const topApis = ref([])
const trend = ref([])

const barHeight = (cnt) => {
  const max = Math.max(...trend.value.map((t) => Number(t.cnt) || 0), 1)
  const h = Math.max(8, Math.round((Number(cnt) / max) * 150))
  return h + 'px'
}

const shortDay = (day) => (day ? String(day).slice(5) : '-')

onMounted(async () => {
  const stats = await dashboardApi.stats()
  cards.value = [
    { label: '项目总数', value: stats.projectTotal ?? 0 },
    { label: '接口总数', value: stats.apiTotal ?? 0 },
    { label: '数据源总数', value: stats.datasourceTotal ?? 0 },
    {
      label: '今日调用',
      value: stats.logToday ?? 0,
      sub: `错误率 ${stats.todayErrorRate ?? 0}%`
    }
  ]
  topProjects.value = stats.topProjects || []
  topApis.value = stats.topApis || []
  trend.value = stats.trend || []
})
</script>

<style scoped>
.stat-item { text-align: center; padding: 8px 0; }
.stat-label { color: #909399; font-size: 13px; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 600; color: #1f3b73; }
.stat-sub { color: #909399; font-size: 12px; margin-top: 6px; }
.trend {
  display: flex; align-items: flex-end; justify-content: space-around;
  height: 190px; padding: 10px 20px;
}
.trend-item { display: flex; flex-direction: column; align-items: center; justify-content: flex-end; flex: 1; }
.trend-bar {
  width: 34px; background: linear-gradient(180deg, #3f7fd4, #1f3b73);
  border-radius: 4px 4px 0 0; position: relative; min-height: 8px;
  display: flex; align-items: flex-start; justify-content: center;
}
.trend-cnt { color: #fff; font-size: 11px; margin-top: 4px; }
.trend-day { margin-top: 6px; font-size: 12px; color: #909399; }
</style>
