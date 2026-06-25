<template>
  <section class="page">
    <div class="page-header">
      <div>
        <el-button text @click="$router.push('/resident/repairs')">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h1 class="page-title" v-if="order">报修详情 - {{ order.orderNo }}</h1>
      </div>
    </div>

    <div v-loading="loading">
      <template v-if="order">
        <!-- 基础信息 -->
        <div class="panel detail-section">
          <h3 class="section-title">基础信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="工单编号">{{ order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="报修类别">{{ order.categoryName }}</el-descriptions-item>
            <el-descriptions-item label="标题" :span="2">{{ order.title }}</el-descriptions-item>
            <el-descriptions-item label="位置" :span="2">{{ order.location }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ order.description }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ order.contactPhone }}</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ order.residentName }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusType(order.status)" size="small">{{ statusLabel(order.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="优先级">
              <el-tag :type="priorityType(order.priority)" size="small">{{ order.priority }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="维修人员" v-if="order.workerName">{{ order.workerName }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatTime(order.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatTime(order.updatedAt) }}</el-descriptions-item>
            <el-descriptions-item label="驳回原因" v-if="order.rejectReason" :span="2">
              <span style="color: var(--cr-danger)">{{ order.rejectReason }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="取消原因" v-if="order.cancelReason" :span="2">
              <span style="color: var(--cr-muted)">{{ order.cancelReason }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="处理结果" v-if="order.completionResult" :span="2">{{ order.completionResult }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 状态日志 -->
        <div class="panel detail-section">
          <h3 class="section-title">状态日志</h3>
          <el-timeline v-if="order.logs?.length">
            <el-timeline-item v-for="log in order.logs" :key="log.id"
              :timestamp="formatTime(log.createdAt)" placement="top">
              <div class="log-item">
                <el-tag :type="statusType(log.toStatus)" size="small">{{ statusLabel(log.toStatus) }}</el-tag>
                <span class="log-action">{{ log.action === 'CREATE' ? '居民提交报修' : log.remark }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无日志" :image-size="80" />
        </div>

        <!-- 图片附件 -->
        <div class="panel detail-section" v-if="order.attachments?.length">
          <h3 class="section-title">图片附件</h3>
          <div class="attachment-grid">
            <div v-for="att in order.attachments" :key="att.id" class="attachment-item">
              <el-image :src="att.fileUrl" :preview-teleported="true" :preview-src-list="[att.fileUrl]"
                fit="cover" class="attachment-img" />
              <p class="attachment-name">{{ att.originalName }}</p>
            </div>
          </div>
        </div>

        <!-- 操作 -->
        <div class="action-bar" v-if="order.status === 'PENDING_REVIEW'">
          <el-button type="danger" @click="handleCancel">取消报修</el-button>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="工单不存在" :image-size="80" />
    </div>

    <!-- 取消对话框 -->
    <el-dialog v-model="showCancel" title="取消报修" width="420px" destroy-on-close>
      <el-form :model="cancelForm">
        <el-form-item label="取消原因">
          <el-input v-model="cancelForm.reason" type="textarea" :rows="3" placeholder="请说明取消原因" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCancel = false">返回</el-button>
        <el-button type="danger" :loading="cancelling" @click="confirmCancel">确认取消</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getOrderDetail, cancelOrder } from '@/api/repair'
import type { OrderDetailVO } from '@/api/repair'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const order = ref<OrderDetailVO | null>(null)
const loading = ref(true)

// 取消
const showCancel = ref(false)
const cancelling = ref(false)
const cancelForm = ref({ reason: '' })

function statusLabel(s: string) {
  const m: Record<string, string> = {
    PENDING_REVIEW: '待审核', PENDING_ASSIGN: '待分派', PENDING_ACCEPT: '待接单',
    PROCESSING: '处理中', COMPLETED: '已完成', EVALUATED: '已评价',
    REJECTED: '已驳回', CANCELLED: '已取消',
  }
  return m[s] || s
}

function statusType(s: string) {
  if (['PENDING_REVIEW', 'PENDING_ASSIGN'].includes(s)) return 'warning'
  if (['PENDING_ACCEPT', 'PROCESSING'].includes(s)) return 'primary'
  if (['COMPLETED', 'EVALUATED'].includes(s)) return 'success'
  if (s === 'CANCELLED') return 'info'
  return 'danger'
}

function priorityType(p: string) {
  if (p === 'LOW') return 'info'
  if (p === 'NORMAL') return ''
  if (p === 'HIGH') return 'warning'
  return 'danger'
}

function formatTime(t: string) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

async function fetchDetail() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res = await getOrderDetail(id)
    order.value = res.data.data
  } catch {
    order.value = null
  } finally {
    loading.value = false
  }
}

async function handleCancel() {
  cancelForm.value.reason = ''
  showCancel.value = true
}

async function confirmCancel() {
  if (!cancelForm.value.reason.trim()) {
    ElMessage.warning('请输入取消原因')
    return
  }
  cancelling.value = true
  try {
    await cancelOrder(order.value!.id, { reason: cancelForm.value.reason })
    ElMessage.success('已取消')
    showCancel.value = false
    fetchDetail()
  } catch { /* handled */ }
  finally { cancelling.value = false }
}

onMounted(async () => {
  await auth.init()
  if (!auth.isLoggedIn()) {
    router.push('/resident/repairs')
    return
  }
  fetchDetail()
})
</script>

<style scoped>
.detail-section {
  padding: 20px;
}
.section-title {
  margin: 0 0 16px;
  color: var(--cr-deep-blue);
  font-size: 16px;
  font-weight: 700;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--cr-border);
}
.log-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.log-action {
  color: var(--cr-text);
  font-size: 14px;
}
.attachment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}
.attachment-item {
  text-align: center;
}
.attachment-img {
  width: 140px;
  height: 140px;
  border-radius: var(--cr-radius-control);
  border: 1px solid var(--cr-border);
}
.attachment-name {
  margin: 6px 0 0;
  color: var(--cr-muted);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.action-bar {
  display: flex;
  gap: 12px;
  padding: 16px 0;
}
</style>
