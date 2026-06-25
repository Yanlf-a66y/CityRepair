import { http, type ApiResponse } from './http'
import type { OrderStatus } from '@/types/order'

export interface CreateOrderReq {
  categoryId: number
  title: string
  location: string
  description: string
  contactPhone: string
}

export interface CancelOrderReq {
  reason: string
}

export interface RepairOrderVO {
  id: number
  orderNo: string
  residentId: number
  categoryId: number
  title: string
  location: string
  description: string
  contactPhone: string
  priority: string
  status: OrderStatus
  currentWorkerId: number | null
  rejectReason: string | null
  cancelReason: string | null
  completionResult: string | null
  createdAt: string
  updatedAt: string
}

export interface CategoryVO {
  id: number
  categoryCode: string
  categoryName: string
  description: string
  enabled: number
}

export interface StatusLogVO {
  id: number
  orderId: number
  action: string
  fromStatus: string | null
  toStatus: string
  remark: string | null
  createdAt: string
}

export interface AttachmentVO {
  id: number
  fileUrl: string
  originalName: string
  attachmentType: string
}

export interface OrderDetailVO extends RepairOrderVO {
  logs: StatusLogVO[]
  attachments: AttachmentVO[]
  categoryName: string
  residentName: string
  workerName?: string
}

export function createOrder(data: CreateOrderReq) {
  return http.post<ApiResponse<{ id: number; orderNo: string }>>('/repair-orders', data)
}

export function getMyOrders(params: { page: number; pageSize: number; keyword?: string; status?: string }) {
  return http.get<ApiResponse<{ page: number; pageSize: number; total: number; records: RepairOrderVO[] }>>('/repair-orders/my', { params })
}

export function getOrderDetail(id: number) {
  return http.get<ApiResponse<OrderDetailVO>>(`/repair-orders/${id}`)
}

export function cancelOrder(id: number, data: CancelOrderReq) {
  return http.put<ApiResponse<null>>(`/repair-orders/${id}/cancel`, data)
}

export function getCategories() {
  return http.get<ApiResponse<CategoryVO[]>>('/repair-orders/categories')
}

export function uploadAttachment(id: number, file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.post<ApiResponse<{ url: string; originalName: string }>>(`/repair-orders/${id}/attachments`, form)
}
