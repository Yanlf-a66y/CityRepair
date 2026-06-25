# PR：统计看板与类别管理

## 本次完成

- **统计看板**：工单总数、待处理、已完成、完成率、状态分布饼图、类别分布柱状图、近 7 天趋势折线图、维修人员排行表
- **类别管理**：类别列表、新增、修改、启停开关、删除限制（有关联工单时拒绝删除）
- 所有图表数据来自后端真实 API，查询 `city_repair` 数据库，无硬编码

## 修改文件

**后端（14 新建 + 1 改造）：**

| 操作 | 文件 | 说明 |
|------|------|------|
| 新建 | `entity/RepairOrder.java` | 工单实体 |
| 新建 | `entity/RepairCategory.java` | 类别实体 |
| 新建 | `entity/SysUser.java` | 用户实体 |
| 新建 | `dto/StatusStat.java` | 状态分布 DTO |
| 新建 | `dto/CategoryStat.java` | 类别分布 DTO |
| 新建 | `dto/TrendItem.java` | 趋势数据 DTO |
| 新建 | `dto/WorkerRankItem.java` | 排行数据 DTO |
| 新建 | `mapper/RepairOrderMapper.java` | 工单 Mapper（6 条统计 SQL） |
| 新建 | `mapper/RepairCategoryMapper.java` | 类别 Mapper |
| 新建 | `mapper/SysUserMapper.java` | 用户 Mapper |
| 新建 | `service/StatisticsService.java` | 统计服务接口 |
| 新建 | `service/impl/StatisticsServiceImpl.java` | 统计服务实现 |
| 新建 | `service/CategoryService.java` | 类别服务接口 |
| 新建 | `service/impl/CategoryServiceImpl.java` | 类别服务实现（含删除关联检查） |
| 新建 | `controller/CategoryController.java` | 类别 CRUD 接口（5 个） |
| 改造 | `controller/StatisticsController.java` | 从返回 0 改为真实查询，新增 4 个接口 |

**前端（1 新建 + 3 改造）：**

| 操作 | 文件 | 说明 |
|------|------|------|
| 改造 | `api/systemApi.ts` | 新增 10 个 API 函数 + 8 个 TS 类型 |
| 改造 | `views/DashboardView.vue` | 接入 ECharts 饼图/柱状图/折线图 + 排行表 |
| 新建 | `views/CategoryView.vue` | 类别管理 CRUD 页面 |
| 改造 | `router/index.ts` | 新增 `/categories` 路由 |
| 改造 | `App.vue` | 新增类别管理菜单项 |

## 接口变化

### 统计看板（新增 4 个，改造 1 个）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/statistics/overview` | 改造：返回真实数据 |
| GET | `/api/statistics/status` | 新增：状态分布 |
| GET | `/api/statistics/category` | 新增：类别分布 |
| GET | `/api/statistics/trend` | 新增：近 7 天趋势 |
| GET | `/api/statistics/worker-rank` | 新增：维修人员 TOP 10 |

### 类别管理（全部新增）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/categories` | 查询所有类别 |
| POST | `/api/categories` | 新增类别 |
| PUT | `/api/categories/{id}` | 修改类别 |
| PUT | `/api/categories/{id}/status` | 启停切换 |
| DELETE | `/api/categories/{id}` | 删除（有关联工单时拒绝） |

## 数据库变化

无。使用现有 `repair_order`、`repair_category`、`sys_user` 表，未新增表或字段。

## 实际执行命令

```bash
# 前端启动
cd frontend
npm install
npm run dev
# → http://localhost:5173

# 前端构建验证
npm run build
# ✓ 2230 modules, built in 10.7s

# 后端启动（IDEA 运行 CityRepairBackendApplication）
# → http://localhost:8080/api

# 接口验证
curl http://localhost:8080/api/statistics/overview
curl http://localhost:8080/api/statistics/status
curl http://localhost:8080/api/statistics/category
curl http://localhost:8080/api/statistics/trend
curl http://localhost:8080/api/statistics/worker-rank
curl http://localhost:8080/api/categories
```

## 测试结果

- [x] 统计总览显示真实工单数（非 0）
- [x] 状态分布饼图正常渲染
- [x] 类别分布柱状图正常渲染
- [x] 7 天趋势折线图正常渲染
- [x] 维修人员排行表正常显示
- [x] 类别列表显示 4 条种子数据
- [x] 新增类别成功
- [x] 修改类别成功
- [x] 启停开关正常切换
- [x] 删除有关联工单的类别时拒绝并提示错误

## 截图

| 页面 | 截图 |
|------|------|
| 统计看板 | [插入截图] |
| 类别管理 | [插入截图] |

## AI 协作日志

见 `docs/孔祥洋-AI协作日志.md`

## 自检清单

- [x] 没有修改其他成员主责模块
- [x] 没有新增非法状态
- [x] 没有写死当前用户 ID
- [x] 权限校验在后端完成（类别删除检查）
- [x] 页面调用真实后端 API
- [x] 至少有成功和失败场景测试
- [x] 已提交有效 commit
- [x] 没有提交 node_modules、target、dist、日志或密钥

## 剩余风险

- 数据库仅 1 条种子工单，图表数据较稀疏
- 全局登录鉴权尚未实现，统计和类别接口暂未加权限拦截
