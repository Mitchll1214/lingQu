<template>
  <div>
    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :lg="6" v-for="card in cards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-body">
            <div class="stat-icon">
              <el-icon :size="22"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-meta">
              <div class="stat-label">{{ card.label }}</div>
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-sub">{{ card.sub || '' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>近 7 日调用趋势（柱状 + 折线）</template>
      <svg v-if="trend.length" viewBox="0 0 800 200" class="trend-svg" role="img" aria-label="近 7 日调用趋势柱状折线图">
        <defs>
          <linearGradient id="trend-bar" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stop-color="#3f7fd4"/>
            <stop offset="1" stop-color="#1f3b73"/>
          </linearGradient>
        </defs>
        <line v-for="g in [0, 1, 2, 3]" :key="'g' + g" x1="60" :x2="760" :y1="35 + g * 43" :y2="35 + g * 43"
              stroke="#e4e9f2" stroke-width="1" stroke-dasharray="4 4" />
        <template v-for="(t, i) in trend" :key="t.day">
          <rect :x="xAt(i) - 17" :y="barTop(i)" width="34" :height="barH(i)" rx="4" fill="url(#trend-bar)" />
          <text :x="xAt(i)" :y="barTop(i) - 8" font-size="12" text-anchor="middle" fill="#6b7a90">{{ t.cnt }}</text>
          <text :x="xAt(i)" :y="188" font-size="13" text-anchor="middle" fill="#909399">{{ shortDay(t.day) }}</text>
        </template>
        <polyline :points="linePoints" fill="none" stroke="#e6a23c" stroke-width="2.5"
                  stroke-linejoin="round" stroke-linecap="round" />
        <circle v-for="(t, i) in trend" :key="'p' + i" :cx="xAt(i)" :cy="barTop(i)" r="4"
                fill="#e6a23c" stroke="#fff" stroke-width="1.5" />
      </svg>
      <el-empty v-else description="暂无调用日志" :image-size="60" />
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>项目调用量排行（TOP 5）</template>
          <el-table :data="topProjects" empty-text="暂无调用日志" size="small">
            <el-table-column prop="project_code" label="项目编码" />
            <el-table-column prop="cnt" label="调用次数" width="140" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
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
import { ref, computed, onMounted, markRaw } from 'vue'
import { Folder, Connection, Coin, Odometer } from '@element-plus/icons-vue'
import { dashboardApi } from '../api/modules'

const cards = ref([])
const topProjects = ref([])
const topApis = ref([])
const trend = ref([])

const maxCnt = computed(() => Math.max(...trend.value.map((t) => Number(t.cnt) || 0), 1))
const xAt = (i) => {
  const n = trend.value.length
  return n > 1 ? 60 + (i * 700) / (n - 1) : 60
}
const barH = (i) => Math.max(6, ((Number(trend.value[i]?.cnt) || 0) / maxCnt.value) * 130)
const barTop = (i) => 165 - barH(i)
const linePoints = computed(() => trend.value.map((t, i) => `${xAt(i)},${barTop(i)}`).join(' '))

const shortDay = (day) => (day ? String(day).slice(5) : '-')

onMounted(async () => {
  const stats = await dashboardApi.stats()
  cards.value = [
    { label: '项目总数', value: stats.projectTotal ?? 0, icon: markRaw(Folder) },
    { label: '接口总数', value: stats.apiTotal ?? 0, icon: markRaw(Connection) },
    { label: '数据源总数', value: stats.datasourceTotal ?? 0, icon: markRaw(Coin) },
    {
      label: '今日调用',
      value: stats.logToday ?? 0,
      sub: `错误率 ${stats.todayErrorRate ?? 0}%`,
      icon: markRaw(Odometer)
    }
  ]
  topProjects.value = stats.topProjects || []
  topApis.value = stats.topApis || []
  trend.value = stats.trend || []
})
</script>

<style scoped>
.stat-body {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 6px 4px;
}
.stat-icon {
  flex: none;
  width: 46px;
  height: 46px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1f3b73;
  background: #e8edf5;
}
.stat-meta { min-width: 0; }
.stat-label { color: #6b7a90; font-size: 13px; margin-bottom: 4px; }
.stat-value { font-size: 26px; font-weight: 700; color: #1f3b73; line-height: 1.2; font-variant-numeric: tabular-nums; }
.stat-sub { color: #909399; font-size: 12px; margin-top: 3px; min-height: 18px; }
.trend-svg { width: 100%; height: auto; display: block; }
</style>
