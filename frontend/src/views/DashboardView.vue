<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">统计看板</h1>
        <p class="muted">展示工单总量、待处理、已完成和完成率，数据来自真实统计 API。</p>
      </div>
      <el-button :loading="loading" @click="loadAll">刷新</el-button>
    </div>

    <!-- 指标卡片 -->
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :xs="24" :sm="12" :lg="6">
        <div class="metric panel">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域：第一行 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :lg="12">
        <div class="panel chart-panel">
          <h3 class="chart-title">状态分布</h3>
          <div ref="statusChartRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="panel chart-panel">
          <h3 class="chart-title">类别分布</h3>
          <div ref="categoryChartRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域：第二行 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :lg="12">
        <div class="panel chart-panel">
          <h3 class="chart-title">近 7 天趋势</h3>
          <div ref="trendChartRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="panel chart-panel">
          <h3 class="chart-title">维修人员排行</h3>
          <el-table :data="workerRank" stripe size="small" class="rank-table" v-loading="loading">
            <el-table-column type="index" label="排名" width="60" align="center" />
            <el-table-column prop="workerName" label="姓名" />
            <el-table-column prop="completedCount" label="完成数" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="success" effect="plain">{{ row.completedCount }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && workerRank.length === 0" description="暂无排行数据" />
        </div>
      </el-col>
    </el-row>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts/core'
import { PieChart, BarChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import {
  getOverview,
  getStatusDistribution,
  getCategoryDistribution,
  getTrend,
  getWorkerRank,
  ORDER_STATUS_MAP,
  type Overview,
  type StatusStat,
  type CategoryStat,
  type TrendItem,
  type WorkerRankItem,
} from '@/api/systemApi'

// 注册 ECharts 组件
echarts.use([PieChart, BarChart, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

// ===== 数据 =====

const loading = ref(false)

const overview = ref<Overview>({
  totalOrders: 0,
  pendingOrders: 0,
  completedOrders: 0,
  completionRate: 0,
})

const statusStats = ref<StatusStat[]>([])
const categoryStats = ref<CategoryStat[]>([])
const trendData = ref<TrendItem[]>([])
const workerRank = ref<WorkerRankItem[]>([])

const cards = computed(() => [
  { label: '工单总数', value: overview.value.totalOrders },
  { label: '待处理', value: overview.value.pendingOrders },
  { label: '已完成', value: overview.value.completedOrders },
  { label: '完成率', value: `${overview.value.completionRate}%` },
])

// ===== 图表引用 =====

const statusChartRef = ref<HTMLElement>()
const categoryChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()

let statusChart: echarts.ECharts | null = null
let categoryChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

// ===== 状态饼图 =====

function renderStatusChart() {
  if (!statusChartRef.value) return
  if (!statusChart) {
    statusChart = echarts.init(statusChartRef.value)
  }
  const data = statusStats.value.map((s) => ({
    name: ORDER_STATUS_MAP[s.status] || s.status,
    value: s.count,
  }))
  statusChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { fontSize: 12 } },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' },
        },
        data,
      },
    ],
  })
}

// ===== 类别柱状图 =====

function renderCategoryChart() {
  if (!categoryChartRef.value) return
  if (!categoryChart) {
    categoryChart = echarts.init(categoryChartRef.value)
  }
  const names = categoryStats.value.map((c) => c.categoryName)
  const values = categoryStats.value.map((c) => c.count)
  categoryChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '8%', bottom: '3%', top: 10, containLabel: true },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: { rotate: names.length > 5 ? 30 : 0 },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
    },
    series: [
      {
        type: 'bar',
        data: values,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#83bff6' },
            { offset: 1, color: '#188df0' },
          ]),
          borderRadius: [6, 6, 0, 0],
        },
        barWidth: '50%',
      },
    ],
  })
}

// ===== 趋势折线图 =====

function renderTrendChart() {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  const dates = trendData.value.map((t) => t.date)
  const counts = trendData.value.map((t) => t.count)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 10, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
    },
    series: [
      {
        type: 'line',
        data: counts,
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { color: '#188df0', width: 3 },
        itemStyle: { color: '#188df0' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(24,141,240,0.3)' },
            { offset: 1, color: 'rgba(24,141,240,0.02)' },
          ]),
        },
      },
    ],
  })
}

// ===== 数据加载 =====

async function loadAll() {
  loading.value = true
  try {
    const [ovRes, stRes, caRes, trRes, wrRes] = await Promise.all([
      getOverview(),
      getStatusDistribution(),
      getCategoryDistribution(),
      getTrend(),
      getWorkerRank(),
    ])
    overview.value = ovRes.data.data
    statusStats.value = stRes.data.data
    categoryStats.value = caRes.data.data
    trendData.value = trRes.data.data
    workerRank.value = wrRes.data.data

    await nextTick()
    renderStatusChart()
    renderCategoryChart()
    renderTrendChart()
  } finally {
    loading.value = false
  }
}

// ===== 响应式调整 =====

function handleResize() {
  statusChart?.resize()
  categoryChart?.resize()
  trendChart?.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  statusChart?.dispose()
  categoryChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped>
.metric {
  display: flex;
  min-height: 110px;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
  padding: 18px;
}

.metric span {
  color: var(--cr-muted);
  font-size: 14px;
}

.metric strong {
  color: var(--cr-deep-blue);
  font-size: 30px;
}

.chart-panel {
  min-height: 320px;
  padding: 18px;
  display: flex;
  flex-direction: column;
}

.chart-title {
  margin: 0 0 12px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--cr-text);
}

.chart-box {
  flex: 1;
  min-height: 280px;
  width: 100%;
}

.rank-table {
  flex: 1;
}
</style>
