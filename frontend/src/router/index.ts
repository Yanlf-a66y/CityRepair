import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '@/views/DashboardView.vue'
import ResidentRepairView from '@/views/ResidentRepairView.vue'
import AdminOrdersView from '@/views/AdminOrdersView.vue'
import WorkerOrdersView from '@/views/WorkerOrdersView.vue'
import WorkerOrderDetailView from '@/views/WorkerOrderDetailView.vue'
import EvaluationView from '@/views/EvaluationView.vue'
import CategoryView from '@/views/CategoryView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/dashboard', name: 'dashboard', component: DashboardView, meta: { title: '统计看板' } },
    { path: '/resident/repairs', name: 'resident-repairs', component: ResidentRepairView, meta: { title: '居民报修' } },
    { path: '/admin/orders', name: 'admin-orders', component: AdminOrdersView, meta: { title: '审核与分派' } },
    { path: '/worker/orders', name: 'worker-orders', component: WorkerOrdersView, meta: { title: '维修工单' } },
    { path: '/worker/orders/:id', name: 'worker-order-detail', component: WorkerOrderDetailView, meta: { title: '工单处理' } },
    { path: '/evaluations', name: 'evaluations', component: EvaluationView, meta: { title: '评价与通知' } },
    { path: '/categories', name: 'categories', component: CategoryView, meta: { title: '类别管理' } },
  ],
})

export default router
