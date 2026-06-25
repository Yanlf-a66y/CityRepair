import { http, type ApiResponse } from './http'

export interface RepairOrder {
  id: number
  orderNo: string
  residentId: number
  categoryId: number
  title: string
  location: string
  description: string
  contactPhone: string
  priority: string
  status: string
  currentWorkerId: number | null
  rejectReason: string | null
  completionResult: string | null
  createdAt: string
  updatedAt: string
}

export interface OrderStatusLog {
  id: number
  orderId: number
  operatorId: number
  action: string
  fromStatus: string | null
  toStatus: string
  remark: string | null
  createdAt: string
}

export interface OrderListResult {
  page: number
  pageSize: number
  total: number
  records: RepairOrder[]
}

export interface WorkerInfo {
  id: number
  username: string
  realName: string
  phone: string
}

export interface OrderListParams {
  page?: number
  pageSize?: number
  keyword?: string
  status?: string
}

export function getAdminOrders(params: OrderListParams) {
  return http.get<ApiResponse<OrderListResult>>('/admin/orders', { params })
}

export function approveOrder(id: number, body: { priority: string; remark?: string }) {
  return http.put<ApiResponse<null>>(`/admin/orders/${id}/approve`, body)
}

export function rejectOrder(id: number, body: { reason: string }) {
  return http.put<ApiResponse<null>>(`/admin/orders/${id}/reject`, body)
}

export function assignOrder(id: number, body: { workerId: number; remark?: string }) {
  return http.put<ApiResponse<null>>(`/admin/orders/${id}/assign`, body)
}

export function getWorkerList() {
  return http.get<ApiResponse<WorkerInfo[]>>('/admin/workers')
}
