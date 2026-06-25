<template>
  <section class="page">
<<<<<<< HEAD
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-icon">
          <el-icon :size="28"><Tools /></el-icon>
        </div>
        <div>
          <h1 class="page-title">维修工单</h1>
          <p class="page-subtitle">查看分派给您的工单，进行接单和处理操作</p>
        </div>
      </div>
      <el-button class="refresh-btn" @click="loadData" :loading="loading">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card stat-pending">
        <div class="stat-icon">
          <el-icon :size="24"><Clock /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.pending }}</span>
          <span class="stat-label">待接单</span>
        </div>
      </div>
      <div class="stat-card stat-processing">
        <div class="stat-icon">
          <el-icon :size="24"><Loading /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.processing }}</span>
          <span class="stat-label">处理中</span>
        </div>
      </div>
      <div class="stat-card stat-completed">
        <div class="stat-icon">
          <el-icon :size="24"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.completed }}</span>
          <span class="stat-label">已完成</span>
        </div>
      </div>
      <div class="stat-card stat-total">
        <div class="stat-icon">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ pagination.total }}</span>
          <span class="stat-label">全部工单</span>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="panel filter-panel">
      <div class="filter-header">
        <el-icon><Search /></el-icon>
        <span>筛选条件</span>
      </div>
      <el-form :inline="true" :model="filterForm" @submit.prevent="handleSearch" class="filter-form">
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable class="filter-select">
            <el-option label="待接单" value="PENDING_ACCEPT" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="filterForm.keyword"
            placeholder="工单编号/标题"
            clearable
            class="filter-input"
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 工单列表 -->
    <div class="panel table-panel">
      <el-table
        v-loading="loading"
        :data="orderList"
        style="width: 100%"
        empty-text="暂无工单数据"
        :header-cell-style="{ background: '#f8fafc', color: '#1f2937', fontWeight: '600' }"
        :row-style="{ cursor: 'pointer' }"
        @row-click="goToDetail"
      >
        <el-table-column prop="orderNo" label="工单编号" width="150">
          <template #default="{ row }">
            <span class="order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="order-title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="类别" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" class="category-tag">
              {{ row.categoryName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)" size="small" effect="dark" round>
              {{ getPriorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small" effect="plain">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="residentName" label="居民" width="90" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" class="action-btn">
              查看详情
              <el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="pagination.total > 0">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Search, RefreshLeft, Tools, Clock, Loading, CircleCheck, Document, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getWorkerOrders, type WorkerOrder } from '../api/workerApi'

const router = useRouter()

// 加载状态
const loading = ref(false)

// 工单列表
const orderList = ref<WorkerOrder[]>([])

// 筛选表单
const filterForm = reactive({
  keyword: '',
  status: ''
})

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 统计数据
const stats = reactive({
  pending: 0,
  processing: 0,
  completed: 0
})

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const response = await getWorkerOrders({
      page: pagination.page,
      pageSize: pagination.pageSize,
      keyword: filterForm.keyword || undefined,
      status: filterForm.status || undefined
    })

    if (response.data.code === 0) {
      orderList.value = response.data.data.records
      pagination.total = response.data.data.total

      // 加载统计数据
      await loadStats()
    } else {
      ElMessage.error(response.data.message || '加载失败')
    }
  } catch (error) {
    console.error('加载工单列表失败:', error)
    ElMessage.error('加载工单列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 加载统计数据
const loadStats = async () => {
  try {
    const [pendingRes, processingRes, completedRes] = await Promise.all([
      getWorkerOrders({ status: 'PENDING_ACCEPT', pageSize: 100 }),
      getWorkerOrders({ status: 'PROCESSING', pageSize: 100 }),
      getWorkerOrders({ status: 'COMPLETED', pageSize: 100 })
    ])
    stats.pending = pendingRes.data.data.records ? pendingRes.data.data.records.length : 0
    stats.processing = processingRes.data.data.records ? processingRes.data.data.records.length : 0
    stats.completed = completedRes.data.data.records ? completedRes.data.data.records.length : 0
  } catch (error) {
    console.error('加载统计失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadData()
}

// 重置
const handleReset = () => {
  filterForm.keyword = ''
  filterForm.status = ''
  pagination.page = 1
  loadData()
}

// 跳转到详情页
const goToDetail = (row: WorkerOrder) => {
  router.push(`/worker/orders/${row.id}`)
}

// 分页大小改变
const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.page = 1
  loadData()
}

// 页码改变
const handlePageChange = (page: number) => {
  pagination.page = page
  loadData()
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取状态标签
const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    'PENDING_REVIEW': '待审核',
    'PENDING_ASSIGN': '待分派',
    'PENDING_ACCEPT': '待接单',
    'PROCESSING': '处理中',
    'COMPLETED': '已完成',
    'EVALUATED': '已评价',
    'REJECTED': '已驳回',
    'CANCELLED': '已取消'
  }
  return map[status] || status
}

// 获取状态类型
const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    'PENDING_REVIEW': 'warning',
    'PENDING_ASSIGN': 'warning',
    'PENDING_ACCEPT': 'primary',
    'PROCESSING': 'primary',
    'COMPLETED': 'success',
    'EVALUATED': 'success',
    'REJECTED': 'danger',
    'CANCELLED': 'info'
  }
  return map[status] || 'info'
}

// 获取优先级标签
const getPriorityLabel = (priority: string) => {
  const map: Record<string, string> = {
    'LOW': '低',
    'NORMAL': '普通',
    'HIGH': '高',
    'URGENT': '紧急'
  }
  return map[priority] || priority
}

// 获取优先级类型
const getPriorityType = (priority: string) => {
  const map: Record<string, string> = {
    'LOW': 'info',
    'NORMAL': 'primary',
    'HIGH': 'warning',
    'URGENT': 'danger'
  }
  return map[priority] || 'info'
}

// 页面加载时获取数据
onMounted(() => {
  loadData()
})
</script>

<style scoped>
/* 页面标题区 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: linear-gradient(135deg, var(--cr-primary) 0%, var(--cr-deep-blue) 100%);
  border-radius: var(--cr-radius-card);
  box-shadow: var(--cr-shadow);
}

.page-header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-icon {
  width: 52px;
  height: 52px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.page-title {
  margin: 0;
  color: #fff;
  font-size: 24px;
  font-weight: 700;
}

.page-subtitle {
  margin: 4px 0 0;
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
}

.refresh-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #fff;
}

.refresh-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: var(--cr-surface);
  border-radius: var(--cr-radius-card);
  box-shadow: var(--cr-shadow);
  border-left: 4px solid transparent;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(31, 41, 55, 0.12);
}

.stat-pending {
  border-left-color: var(--cr-warning);
}

.stat-processing {
  border-left-color: var(--cr-primary);
}

.stat-completed {
  border-left-color: var(--cr-success);
}

.stat-total {
  border-left-color: var(--cr-deep-blue);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-pending .stat-icon {
  background: rgba(201, 122, 16, 0.1);
  color: var(--cr-warning);
}

.stat-processing .stat-icon {
  background: rgba(31, 95, 191, 0.1);
  color: var(--cr-primary);
}

.stat-completed .stat-icon {
  background: rgba(46, 125, 91, 0.1);
  color: var(--cr-success);
}

.stat-total .stat-icon {
  background: rgba(23, 74, 126, 0.1);
  color: var(--cr-deep-blue);
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--cr-text);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--cr-muted);
}

/* 筛选面板 */
.filter-panel {
  padding: 0;
  overflow: hidden;
}

.filter-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  background: #f8fafc;
  border-bottom: 1px solid var(--cr-border);
  color: var(--cr-text);
  font-size: 14px;
  font-weight: 500;
}

.filter-form {
  padding: 16px 20px 0;
  margin-bottom: 0;
}

.filter-select {
  width: 150px;
}

.filter-input {
  width: 220px;
}

.filter-actions {
  margin-left: 8px;
}

/* 表格面板 */
.table-panel {
  padding: 0;
  overflow: hidden;
}

.order-no {
  font-family: 'Courier New', monospace;
  font-weight: 600;
  color: var(--cr-primary);
}

.order-title {
  color: var(--cr-text);
  font-weight: 500;
}

.category-tag {
  border-color: var(--cr-border);
  background: #f8fafc;
}

.time-text {
  color: var(--cr-muted);
  font-size: 13px;
}

.action-btn {
  font-weight: 500;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid var(--cr-border);
  background: #f8fafc;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }

  .filter-form {
    flex-direction: column;
  }

  .filter-select,
  .filter-input {
    width: 100%;
  }
}
</style>
=======
    <div class="page-header">
      <div>
        <h1 class="page-title">维修处理</h1>
        <p class="muted">维修人员待接单、处理中和完成结果提交。</p>
      </div>
    </div>

    <el-alert
      title="本页面只展示分派给当前维修人员的工单，后端必须校验归属。"
      type="warning"
      show-icon
      :closable="false"
    />
  </section>
</template>
>>>>>>> feat/resident-repair
