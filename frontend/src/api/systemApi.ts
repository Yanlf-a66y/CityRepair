import { http, type ApiResponse } from './http'

// ===== 类型定义 =====

export interface HealthInfo {
  status: string
  service: string
}

/** 统计总览 */
export interface Overview {
  totalOrders: number
  pendingOrders: number
  completedOrders: number
  completionRate: number
}

/** 状态分布 */
export interface StatusStat {
  status: string
  count: number
}

/** 类别分布 */
export interface CategoryStat {
  categoryName: string
  count: number
}

/** 7天趋势 */
export interface TrendItem {
  date: string
  count: number
}

/** 维修人员排行 */
export interface WorkerRankItem {
  workerId: number
  workerName: string
  completedCount: number
}

/** 报修类别 */
export interface RepairCategory {
  id?: number
  categoryCode: string
  categoryName: string
  description: string
  enabled: number
  sortOrder: number
  createdAt?: string
  updatedAt?: string
}

// ===== 状态映射表（中文显示） =====

export const ORDER_STATUS_MAP: Record<string, string> = {
  PENDING_REVIEW: '待审核',
  PENDING_ASSIGN: '待分派',
  PENDING_ACCEPT: '待接单',
  PROCESSING: '处理中',
  COMPLETED: '已完成',
  EVALUATED: '已评价',
  REJECTED: '已驳回',
  CANCELLED: '已取消',
}

// ===== API 函数 =====

/** 健康检查 */
export function getHealth() {
  return http.get<ApiResponse<HealthInfo>>('/health')
}

// --- 统计 ---

/** 统计总览 */
export function getOverview() {
  return http.get<ApiResponse<Overview>>('/statistics/overview')
}

/** 状态分布 */
export function getStatusDistribution() {
  return http.get<ApiResponse<StatusStat[]>>('/statistics/status')
}

/** 类别分布 */
export function getCategoryDistribution() {
  return http.get<ApiResponse<CategoryStat[]>>('/statistics/category')
}

/** 近7天趋势 */
export function getTrend() {
  return http.get<ApiResponse<TrendItem[]>>('/statistics/trend')
}

/** 维修人员排行 */
export function getWorkerRank() {
  return http.get<ApiResponse<WorkerRankItem[]>>('/statistics/worker-rank')
}

// --- 类别管理 ---

/** 查询所有类别 */
export function getCategories() {
  return http.get<ApiResponse<RepairCategory[]>>('/categories')
}

/** 新增类别 */
export function createCategory(data: Partial<RepairCategory>) {
  return http.post<ApiResponse<null>>('/categories', data)
}

/** 修改类别 */
export function updateCategory(id: number, data: Partial<RepairCategory>) {
  return http.put<ApiResponse<null>>(`/categories/${id}`, data)
}

/** 启停类别 */
export function toggleCategoryStatus(id: number) {
  return http.put<ApiResponse<null>>(`/categories/${id}/status`)
}

/** 删除类别 */
export function deleteCategory(id: number) {
  return http.delete<ApiResponse<null>>(`/categories/${id}`)
}
