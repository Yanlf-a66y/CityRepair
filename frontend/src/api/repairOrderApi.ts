import { http, type ApiResponse } from './http'
import type { OrderStatusLog } from './adminApi'

export function getOrderStatuses() {
  return http.get<ApiResponse<string[]>>('/repair-orders/statuses')
}

export function getOrderLogs(id: number) {
  return http.get<ApiResponse<OrderStatusLog[]>>(`/repair-orders/${id}/logs`)
}
