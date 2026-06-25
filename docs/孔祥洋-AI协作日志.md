# AI 协作日志

## 日期与工具

日期：2026-06-25

工具：Claude Code（DeepSeek v4 Pro）

## 本次任务目标

完成"统计看板与类别管理"模块，包括：
- 统计看板：工单总数、待处理、已完成、完成率、状态分布、类别分布、近7天趋势、维修人员排行
- 类别管理：类别新增、修改、启停、删除限制（有关联工单不可删除）

## 原始提示词

> 孔祥洋：统计看板与类别管理
> 分支：feat/dashboard-category
> 今天必须完成：
> - 统计看板
> - 工单总数、待处理、已完成、完成率
> - 状态分布
> - 类别分布
> - 最近7天趋势
> - 维修人员排行
> - 类别新增、修改、启停、删除限制
> - 图表必须来自后端真实接口，不能写死

## AI 输出摘要

**后端新增文件（14个）：**
- Entity：`RepairOrder.java`、`RepairCategory.java`、`SysUser.java`
- DTO：`StatusStat.java`、`CategoryStat.java`、`TrendItem.java`、`WorkerRankItem.java`
- Mapper：`RepairOrderMapper.java`（含 6 条统计 SQL）、`RepairCategoryMapper.java`、`SysUserMapper.java`
- Service：`StatisticsService.java` + `StatisticsServiceImpl.java`、`CategoryService.java` + `CategoryServiceImpl.java`
- Controller：`CategoryController.java`（5 个接口，含删除校验）

**后端改造文件（1个）：**
- `StatisticsController.java`：从返回硬编码 0 改为查询数据库真实数据，新增 4 个统计接口

**前端改造文件（3个）：**
- `systemApi.ts`：新增 10 个 API 函数 + 8 个 TypeScript 类型
- `DashboardView.vue`：从占位符替换为 ECharts 饼图/柱状图/折线图 + 维修人员排行表格
- `router/index.ts` + `App.vue`：新增类别管理路由和菜单

**前端新增文件（1个）：**
- `CategoryView.vue`：完整 CRUD 管理页，含表单校验、启停开关、删除确认

## 人工审查

- [x] 没有越过本人模块边界（仅修改统计和类别相关代码）
- [x] 没有新增非法状态
- [x] 没有写死用户 ID
- [x] 后端校验：类别删除时检查关联工单
- [x] 页面调用真实后端 API（无 mock 数据）
- [x] 没有写死统计数据（全部从数据库查询）
- [x] 没有提交 node_modules、target、dist

## 人工修改

1. **RepairOrderMapper 返回类型修复**：AI 初次将 `countGroupByStatus()` 返回类型写为 `List<CategoryStat>`，实际应为 `List<StatusStat>`，已修正。
2. **ECharts 引入方式**：确认使用按需引入（`echarts/core` + 独立 chart/component），而非全量引入，减小打包体积。
3. **删除拦截文案**：在 `CategoryServiceImpl.delete()` 中补充了具体的错误提示"该类别下存在 X 条工单，无法删除"。

## 验证结果

**启动命令：**
```bash
# 后端（IDEA 运行 CityRepairBackendApplication）
# 前端（Git Bash）
cd frontend && npm run dev
```

**接口测试：**
- `GET /api/statistics/overview` → 返回真实统计数据
- `GET /api/statistics/status` → 返回状态分布
- `GET /api/statistics/category` → 返回类别分布
- `GET /api/statistics/trend` → 返回 7 天趋势
- `GET /api/statistics/worker-rank` → 返回维修人员排行
- `GET /api/categories` → 返回 4 条种子类别
- `POST /api/categories` → 新增成功
- `PUT /api/categories/{id}` → 修改成功
- `PUT /api/categories/{id}/status` → 启停正常
- `DELETE /api/categories/{id}` → 有关联工单时拒绝删除

**前端页面：**
- `http://localhost:5173/dashboard` → 4 张指标卡 + 3 张 ECharts 图表 + 排行表
- `http://localhost:5173/categories` → 类别列表 + 新增/编辑弹窗 + 启停开关

## 最终证据

commit：`be5ec98` feat: 统计看板与类别管理

PR：feat/dashboard-category → develop

截图：见 docs/截图/

## 剩余风险

- 数据库种子数据仅 1 条工单，图表数据较稀疏（功能正常）
- 缺少登录鉴权（全局基础设施，非本模块范围）
- 统计为只读查询，不涉及状态流转，无需写 order_status_log
