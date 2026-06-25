<template>
  <el-container class="app-shell">
    <el-aside class="sidebar" width="232px">
      <div class="brand">
        <span class="brand-mark">修</span>
        <div>
          <strong>城市报修</strong>
          <span>工单处理系统</span>
        </div>
      </div>

      <el-menu router :default-active="$route.path" class="menu">
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>统计看板</span>
        </el-menu-item>
        <el-menu-item index="/resident/repairs">
          <el-icon><EditPen /></el-icon>
          <span>居民报修</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><Checked /></el-icon>
          <span>审核与分派</span>
        </el-menu-item>
        <el-menu-item index="/worker/orders">
          <el-icon><Tools /></el-icon>
          <span>维修处理</span>
        </el-menu-item>
        <el-menu-item index="/evaluations">
          <el-icon><ChatLineSquare /></el-icon>
          <span>评价与通知</span>
        </el-menu-item>
        <el-menu-item index="/categories">
          <el-icon><Collection /></el-icon>
          <span>类别管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <span class="topbar-title">{{ String($route.meta.title || '工作台') }}</span>
          <span class="topbar-subtitle">数据库：city_repair</span>
        </div>
        <el-tag type="success" effect="light">开发骨架</el-tag>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ChatLineSquare, Checked, Collection, DataAnalysis, EditPen, Tools } from '@element-plus/icons-vue'
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.sidebar {
  border-right: 1px solid var(--cr-border);
  background: #ffffff;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 64px;
  padding: 0 18px;
  border-bottom: 1px solid var(--cr-border);
  color: var(--cr-deep-blue);
}

.brand-mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 8px;
  background: var(--cr-primary);
  color: #ffffff;
  font-weight: 700;
}

.brand strong,
.brand span {
  display: block;
}

.brand span {
  margin-top: 2px;
  color: var(--cr-muted);
  font-size: 12px;
}

.menu {
  border-right: 0;
  padding: 8px;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  border-bottom: 1px solid var(--cr-border);
  background: #ffffff;
}

.topbar-title {
  display: block;
  color: var(--cr-text);
  font-size: 17px;
  font-weight: 700;
}

.topbar-subtitle {
  display: block;
  margin-top: 4px;
  color: var(--cr-muted);
  font-size: 12px;
}

.main-content {
  padding: 20px;
}
</style>
