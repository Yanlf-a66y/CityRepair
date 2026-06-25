# 管理员审核与分派模块 — 设计文档

日期：2026-06-25
负责人：闫龙飞
分支：`feat/admin-dispatch`
状态：approved

## 1. 概述

实现《城市报修与工单处理系统》的管理员审核与分派模块，包含 JWT 认证基础、管理员工单查询/审核/驳回/分派接口、前端页面和状态日志。

## 2. 架构

### 2.1 JWT 认证链路

```
POST /api/auth/login → AuthController → AuthService（验证密码）→ JwtUtil（生成token）
     ↓
后续请求 Header: Authorization: Bearer <token>
     ↓
JwtAuthFilter（解析token，提取 userId/username/role）→ AuthContext.setCurrentUser()
     ↓
Controller 从 AuthContext 获取当前用户，校验角色 = ADMIN
```

### 2.2 事务边界

每个状态变更操作在同一 @Transactional 中完成：
- update repair_order（状态、优先级、reason 等）
- insert order_status_log
- assign 额外 insert order_assignment

## 3. 文件变更

### 新建文件（21个）

**认证：**
- `JwtUtil.java` — JWT 生成/解析
- `JwtAuthFilter.java` — OncePerRequestFilter
- `AuthContext.java` — ThreadLocal 用户上下文
- `dto/LoginRequest.java` — {username, password}
- `dto/LoginResponse.java` — {token, userId, username, role, realName}

**实体：**
- `entity/SysUser.java` — @TableName("sys_user")
- `entity/SysRole.java` — @TableName("sys_role")
- `entity/SysUserRole.java` — @TableName("sys_user_role")
- `entity/RepairOrder.java` — @TableName("repair_order")
- `entity/OrderAssignment.java` — @TableName("order_assignment")
- `entity/OrderStatusLog.java` — @TableName("order_status_log")

**Mapper：**
- `mapper/SysUserMapper.java`
- `mapper/SysRoleMapper.java`
- `mapper/SysUserRoleMapper.java`
- `mapper/RepairOrderMapper.java`
- `mapper/OrderAssignmentMapper.java`
- `mapper/OrderStatusLogMapper.java`

**Service：**
- `service/AuthService.java` — 登录校验
- `service/AdminOrderService.java` — 工单查询/审核/驳回/分派

**前端：**
- `frontend/src/api/adminApi.ts`
- `frontend/src/api/repairOrderApi.ts`

### 修改文件（4个）

- `AuthController.java` — 添加 POST /login, GET /me, POST /logout
- `AdminOrderController.java` — 完整替换 stub
- `RepairOrderController.java` — 添加 GET /{id}/logs
- `AdminOrdersView.vue` — 完整重写

## 4. 接口

| 方法 | 路径 | 校验 | 说明 |
|------|------|------|------|
| POST | /api/auth/login | 无 | 登录返回 JWT |
| GET | /api/auth/me | 登录 | 当前用户信息 |
| POST | /api/auth/logout | 登录 | 登出 |
| GET | /api/admin/orders | ADMIN | 分页查询全部工单 |
| PUT | /api/admin/orders/{id}/approve | ADMIN | 审核通过 → PENDING_ASSIGN |
| PUT | /api/admin/orders/{id}/reject | ADMIN | 驳回 → REJECTED |
| PUT | /api/admin/orders/{id}/assign | ADMIN | 分派 → PENDING_ACCEPT |
| GET | /api/repair-orders/{id}/logs | 登录 | 状态日志 |

## 5. 校验规则

- 所有管理接口：AuthContext 角色 = ADMIN
- approve: 当前状态必须 = PENDING_REVIEW
- reject: 当前状态 = PENDING_REVIEW + reason 非空
- assign: 当前状态 = PENDING_ASSIGN + workerId 存在 + 用户角色 = WORKER
- 所有状态跳转用 OrderStatus.canMoveTo() 校验
- current_worker_id / reject_reason 从 AuthContext 获取，不信任前端

## 6. 前端页面

AdminOrdersView.vue：筛选栏 + el-tabs 状态切换 + 表格 + 分页

3 个操作弹窗：
- 审核通过：优先级选择 + 备注
- 驳回：原因必填 textarea
- 分派：维修人员下拉 + 备注

状态日志：el-drawer + el-timeline

必须覆盖：loading、empty、error、二次确认、防重复提交

## 7. 测试

12 个测试用例覆盖正常/参数/权限/状态/越权/事务场景。

## 8. 不变更

- 数据库表结构
- 状态/优先级/角色枚举
- pom.xml、application.yml、CorsConfig
- 其他 Controller 和 View
- 其他成员模块文件
