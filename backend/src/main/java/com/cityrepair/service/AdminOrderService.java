package com.cityrepair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityrepair.common.ApiResponse;
import com.cityrepair.common.AuthContext;
import com.cityrepair.common.PageQuery;
import com.cityrepair.dto.ApproveRequest;
import com.cityrepair.dto.AssignRequest;
import com.cityrepair.dto.RejectRequest;
import com.cityrepair.entity.*;
import com.cityrepair.enums.OrderStatus;
import com.cityrepair.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AdminOrderService {

    private final RepairOrderMapper orderMapper;
    private final OrderAssignmentMapper assignmentMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    public AdminOrderService(RepairOrderMapper orderMapper,
                             OrderAssignmentMapper assignmentMapper,
                             OrderStatusLogMapper statusLogMapper,
                             SysUserMapper userMapper,
                             SysUserRoleMapper userRoleMapper,
                             SysRoleMapper roleMapper) {
        this.orderMapper = orderMapper;
        this.assignmentMapper = assignmentMapper;
        this.statusLogMapper = statusLogMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    // ----- role guard -----
    private void requireAdmin() {
        var user = AuthContext.get();
        if (user == null || !"ADMIN".equals(user.role())) {
            throw new RuntimeException("403:无权访问，需要管理员权限");
        }
    }

    private Long currentUserId() {
        return AuthContext.get().userId();
    }

    // ----- query orders -----
    public ApiResponse<Map<String, Object>> listOrders(PageQuery query) {
        requireAdmin();
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(RepairOrder::getTitle, query.getKeyword())
                    .or().like(RepairOrder::getOrderNo, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(RepairOrder::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(RepairOrder::getCreatedAt);
        Page<RepairOrder> page = orderMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);
        return ApiResponse.success(Map.of(
                "page", page.getCurrent(),
                "pageSize", page.getSize(),
                "total", page.getTotal(),
                "records", page.getRecords()
        ));
    }

    // ----- approve -----
    @Transactional
    public ApiResponse<Void> approve(Long orderId, ApproveRequest request) {
        requireAdmin();
        // Validate priority is a valid enum value
        try {
            com.cityrepair.enums.OrderPriority.valueOf(request.priority());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, "无效的优先级，有效值：LOW, NORMAL, HIGH, URGENT");
        }
        RepairOrder order = orderMapper.selectById(orderId);
        if (order == null) return ApiResponse.error(404, "工单不存在");
        if (!OrderStatus.PENDING_REVIEW.name().equals(order.getStatus())) {
            return ApiResponse.error(400, "只有待审核工单可以审核");
        }
        if (!OrderStatus.PENDING_REVIEW.canMoveTo(OrderStatus.PENDING_ASSIGN)) {
            return ApiResponse.error(400, "非法状态流转");
        }
        order.setStatus(OrderStatus.PENDING_ASSIGN.name());
        order.setPriority(request.priority());
        orderMapper.updateById(order);

        createStatusLog(orderId, "APPROVE", OrderStatus.PENDING_REVIEW.name(),
                OrderStatus.PENDING_ASSIGN.name(), request.remark());
        return ApiResponse.success("审核通过", null);
    }

    // ----- reject -----
    @Transactional
    public ApiResponse<Void> reject(Long orderId, RejectRequest request) {
        requireAdmin();
        RepairOrder order = orderMapper.selectById(orderId);
        if (order == null) return ApiResponse.error(404, "工单不存在");
        if (!OrderStatus.PENDING_REVIEW.name().equals(order.getStatus())) {
            return ApiResponse.error(400, "只有待审核工单可以驳回");
        }
        if (!OrderStatus.PENDING_REVIEW.canMoveTo(OrderStatus.REJECTED)) {
            return ApiResponse.error(400, "非法状态流转");
        }
        order.setStatus(OrderStatus.REJECTED.name());
        order.setRejectReason(request.reason());
        orderMapper.updateById(order);

        createStatusLog(orderId, "REJECT", OrderStatus.PENDING_REVIEW.name(),
                OrderStatus.REJECTED.name(), request.reason());
        return ApiResponse.success("已驳回", null);
    }

    // ----- assign -----
    @Transactional
    public ApiResponse<Void> assign(Long orderId, AssignRequest request) {
        requireAdmin();
        RepairOrder order = orderMapper.selectById(orderId);
        if (order == null) return ApiResponse.error(404, "工单不存在");
        if (!OrderStatus.PENDING_ASSIGN.name().equals(order.getStatus())) {
            return ApiResponse.error(400, "只有待分派工单可以分派");
        }
        // verify worker exists and has WORKER role
        SysUser worker = userMapper.selectById(request.workerId());
        if (worker == null || worker.getEnabled() == null || worker.getEnabled() != 1) {
            return ApiResponse.error(400, "维修人员不存在或已禁用");
        }
        // Single query: check if user has WORKER role
        Long workerRoleId = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, "WORKER")).getId();
        Long count = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, request.workerId())
                        .eq(SysUserRole::getRoleId, workerRoleId));
        boolean isWorker = count != null && count > 0;
        if (!isWorker) {
            return ApiResponse.error(400, "被分派人必须具有维修人员(WORKER)角色");
        }
        if (!OrderStatus.PENDING_ASSIGN.canMoveTo(OrderStatus.PENDING_ACCEPT)) {
            return ApiResponse.error(400, "非法状态流转");
        }
        order.setStatus(OrderStatus.PENDING_ACCEPT.name());
        order.setCurrentWorkerId(request.workerId());
        order.setAssignedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        OrderAssignment assignment = new OrderAssignment();
        assignment.setOrderId(orderId);
        assignment.setAdminId(currentUserId());
        assignment.setWorkerId(request.workerId());
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setIsCurrent(1);
        assignment.setRemark(request.remark());
        assignmentMapper.insert(assignment);

        createStatusLog(orderId, "ASSIGN", OrderStatus.PENDING_ASSIGN.name(),
                OrderStatus.PENDING_ACCEPT.name(),
                "分派给维修人员 #" + request.workerId()
                        + (StringUtils.hasText(request.remark()) ? "：" + request.remark() : ""));
        return ApiResponse.success("分派成功", null);
    }

    // ----- status logs query -----
    public ApiResponse<List<OrderStatusLog>> getLogs(Long orderId) {
        requireAdmin();
        List<OrderStatusLog> logs = statusLogMapper.selectList(
                new LambdaQueryWrapper<OrderStatusLog>()
                        .eq(OrderStatusLog::getOrderId, orderId)
                        .orderByAsc(OrderStatusLog::getCreatedAt));
        return ApiResponse.success(logs);
    }

    // ----- worker list for assign dropdown -----
    public ApiResponse<List<SysUser>> getWorkers() {
        requireAdmin();
        // Find WORKER role by code, not hardcoded ID
        SysRole workerRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, "WORKER"));
        if (workerRole == null) return ApiResponse.success(List.of());

        List<SysUserRole> workerRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, workerRole.getId()));
        List<Long> workerIds = workerRoles.stream().map(SysUserRole::getUserId).toList();
        if (workerIds.isEmpty()) return ApiResponse.success(List.of());
        List<SysUser> workers = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, workerIds)
                        .eq(SysUser::getEnabled, 1));
        return ApiResponse.success(workers);
    }

    private void createStatusLog(Long orderId, String action, String fromStatus,
                                  String toStatus, String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setOperatorId(currentUserId());
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        statusLogMapper.insert(log);
    }
}
