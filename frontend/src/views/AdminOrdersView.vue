<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">审核与分派</h1>
        <p class="muted">管理员审核、驳回、设置优先级和分派维修人员</p>
      </div>
      <el-button :loading="loading" @click="fetchOrders">刷新</el-button>
    </div>

    <!-- Filters -->
    <div class="panel filter-bar">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索工单编号或标题"
        clearable
        style="width: 220px"
        @clear="search"
        @keyup.enter="search"
      />
      <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 140px" @change="search">
        <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-button type="primary" @click="search" :loading="loading">查询</el-button>
    </div>

    <!-- Table -->
    <div class="panel table-panel">
      <el-table v-loading="loading" :data="orders" stripe empty-text="暂无工单" style="width: 100%">
        <el-table-column prop="orderNo" label="工单编号" min-width="150" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityTag(row.priority)" size="small" effect="light">
              {{ priorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="位置" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="提交时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING_REVIEW'">
              <el-button type="primary" size="small" @click="openApprove(row)">审核通过</el-button>
              <el-button type="danger" size="small" @click="openReject(row)">驳回</el-button>
            </template>
            <template v-else-if="row.status === 'PENDING_ASSIGN'">
              <el-button type="primary" size="small" @click="openAssign(row)">分派</el-button>
            </template>
            <template v-else>
              <el-button size="small" @click="openLogs(row)">日志</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="filters.page"
          v-model:page-size="filters.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="fetchOrders"
        />
      </div>
    </div>

    <!-- Approve Dialog -->
    <el-dialog v-model="approveVisible" title="审核通过" width="480px">
      <el-form label-width="80px">
        <el-form-item label="工单编号"><strong>{{ currentOrder?.orderNo }}</strong></el-form-item>
        <el-form-item label="标题">{{ currentOrder?.title }}</el-form-item>
        <el-form-item label="优先级" required>
          <el-select v-model="approveForm.priority" style="width: 100%">
            <el-option v-for="p in priorities" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="approveForm.remark" type="textarea" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doApprove">确认通过</el-button>
      </template>
    </el-dialog>

    <!-- Reject Dialog -->
    <el-dialog v-model="rejectVisible" title="驳回工单" width="480px">
      <el-form label-width="80px">
        <el-form-item label="工单编号"><strong>{{ currentOrder?.orderNo }}</strong></el-form-item>
        <el-form-item label="驳回原因" required>
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            maxlength="500"
            show-word-limit
            placeholder="请填写驳回原因（必填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" :disabled="!rejectForm.reason.trim()" @click="doReject">
          确认驳回
        </el-button>
      </template>
    </el-dialog>

    <!-- Assign Dialog -->
    <el-dialog v-model="assignVisible" title="分派维修人员" width="480px">
      <el-form label-width="100px">
        <el-form-item label="工单编号"><strong>{{ currentOrder?.orderNo }}</strong></el-form-item>
        <el-form-item label="维修人员" required>
          <el-select v-model="assignForm.workerId" style="width: 100%" placeholder="请选择维修人员">
            <el-option
              v-for="w in workers"
              :key="w.id"
              :label="`${w.realName} (${w.username})`"
              :value="w.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="assignForm.remark" type="textarea" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!assignForm.workerId" @click="doAssign">
          确认分派
        </el-button>
      </template>
    </el-dialog>

    <!-- Status Log Drawer -->
    <el-drawer v-model="logVisible" title="状态日志" size="450px">
      <el-timeline v-if="logs.length">
        <el-timeline-item
          v-for="log in logs"
          :key="log.id"
          :timestamp="formatTime(log.createdAt)"
          placement="top"
        >
          <p><strong>{{ log.action }}</strong>: {{ log.fromStatus || '—' }} → {{ log.toStatus }}</p>
          <p v-if="log.remark" class="muted" style="font-size:13px">{{ log.remark }}</p>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无日志" />
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminOrders,
  approveOrder,
  rejectOrder,
  assignOrder,
  getWorkerList,
  type RepairOrder,
  type OrderStatusLog,
  type WorkerInfo,
} from '@/api/adminApi'
import { getOrderLogs } from '@/api/repairOrderApi'

// ---- state ----
const loading = ref(false)
const submitting = ref(false)
const orders = ref<RepairOrder[]>([])
const total = ref(0)
const workers = ref<WorkerInfo[]>([])

const filters = reactive({ page: 1, pageSize: 10, keyword: '', status: '' as string })

const statuses = [
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '待分派', value: 'PENDING_ASSIGN' },
  { label: '待接单', value: 'PENDING_ACCEPT' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已评价', value: 'EVALUATED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELLED' },
]

const priorities = [
  { label: '低', value: 'LOW' },
  { label: '普通', value: 'NORMAL' },
  { label: '高', value: 'HIGH' },
  { label: '紧急', value: 'URGENT' },
]

// dialog state
const approveVisible = ref(false)
const rejectVisible = ref(false)
const assignVisible = ref(false)
const logVisible = ref(false)
const currentOrder = ref<RepairOrder | null>(null)
const logs = ref<OrderStatusLog[]>([])

const approveForm = reactive({ priority: 'NORMAL', remark: '' })
const rejectForm = reactive({ reason: '' })
const assignForm = reactive({ workerId: null as number | null, remark: '' })

// ---- fetch ----
async function fetchOrders() {
  loading.value = true
  try {
    const res = await getAdminOrders({
      page: filters.page,
      pageSize: filters.pageSize,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
    })
    const data = res.data.data
    orders.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadWorkers() {
  const res = await getWorkerList()
  workers.value = res.data.data
}

function search() {
  filters.page = 1
  fetchOrders()
}

// ---- actions ----
function openApprove(row: RepairOrder) {
  currentOrder.value = row
  approveForm.priority = 'NORMAL'
  approveForm.remark = ''
  approveVisible.value = true
}

async function doApprove() {
  submitting.value = true
  try {
    await approveOrder(currentOrder.value!.id, {
      priority: approveForm.priority,
      remark: approveForm.remark || undefined,
    })
    ElMessage.success('审核通过')
    approveVisible.value = false
    fetchOrders()
  } finally {
    submitting.value = false
  }
}

function openReject(row: RepairOrder) {
  currentOrder.value = row
  rejectForm.reason = ''
  rejectVisible.value = true
}

async function doReject() {
  if (!rejectForm.reason.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  try {
    await ElMessageBox.confirm('确认驳回该工单？', '确认操作', { type: 'warning' })
  } catch {
    return
  }
  submitting.value = true
  try {
    await rejectOrder(currentOrder.value!.id, { reason: rejectForm.reason.trim() })
    ElMessage.success('已驳回')
    rejectVisible.value = false
    fetchOrders()
  } finally {
    submitting.value = false
  }
}

function openAssign(row: RepairOrder) {
  currentOrder.value = row
  assignForm.workerId = null
  assignForm.remark = ''
  loadWorkers()
  assignVisible.value = true
}

async function doAssign() {
  if (!assignForm.workerId) {
    ElMessage.warning('请选择维修人员')
    return
  }
  try {
    await ElMessageBox.confirm('确认分派该工单？', '确认操作', { type: 'info' })
  } catch {
    return
  }
  submitting.value = true
  try {
    await assignOrder(currentOrder.value!.id, {
      workerId: assignForm.workerId,
      remark: assignForm.remark || undefined,
    })
    ElMessage.success('分派成功')
    assignVisible.value = false
    fetchOrders()
  } finally {
    submitting.value = false
  }
}

async function openLogs(row: RepairOrder) {
  currentOrder.value = row
  logVisible.value = true
  const res = await getOrderLogs(row.id)
  logs.value = res.data.data
}

// ---- helpers ----
function formatTime(s: string) {
  if (!s) return ''
  return s.replace('T', ' ').substring(0, 16)
}

function statusTag(s: string) {
  const map: Record<string, string> = {
    PENDING_REVIEW: 'warning', PENDING_ASSIGN: 'warning', PENDING_ACCEPT: 'warning',
    PROCESSING: 'primary', COMPLETED: 'success', EVALUATED: 'success',
    REJECTED: 'danger', CANCELLED: 'info',
  }
  return map[s] || ''
}

function statusLabel(s: string) {
  const map: Record<string, string> = {
    PENDING_REVIEW: '待审核', PENDING_ASSIGN: '待分派', PENDING_ACCEPT: '待接单',
    PROCESSING: '处理中', COMPLETED: '已完成', EVALUATED: '已评价',
    REJECTED: '已驳回', CANCELLED: '已取消',
  }
  return map[s] || s
}

function priorityTag(p: string) {
  const map: Record<string, string> = { LOW: 'info', NORMAL: '', HIGH: 'warning', URGENT: 'danger' }
  return map[p] || ''
}

function priorityLabel(p: string) {
  const map: Record<string, string> = { LOW: '低', NORMAL: '普通', HIGH: '高', URGENT: '紧急' }
  return map[p] || p
}

onMounted(fetchOrders)
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  flex-wrap: wrap;
}

.table-panel {
  padding: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
