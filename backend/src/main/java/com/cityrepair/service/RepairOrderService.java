package com.cityrepair.service;

import com.cityrepair.entity.RepairOrder;
import com.cityrepair.entity.OrderStatusLog;
import com.cityrepair.enums.OrderStatus;
import com.cityrepair.entity.RepairCategory;
import com.cityrepair.entity.OrderAttachment;
import com.cityrepair.entity.SysUser;
import com.cityrepair.mapper.RepairOrderMapper;
import com.cityrepair.mapper.OrderStatusLogMapper;
import com.cityrepair.mapper.RepairCategoryMapper;
import com.cityrepair.mapper.OrderAttachmentMapper;
import com.cityrepair.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class RepairOrderService {

    private final RepairOrderMapper orderMapper;
    private final OrderStatusLogMapper logMapper;
    private final RepairCategoryMapper categoryMapper;
    private final OrderAttachmentMapper attachmentMapper;
    private final SysUserMapper userMapper;

    public RepairOrderService(RepairOrderMapper orderMapper, OrderStatusLogMapper logMapper,
                              RepairCategoryMapper categoryMapper, OrderAttachmentMapper attachmentMapper,
                              SysUserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.logMapper = logMapper;
        this.categoryMapper = categoryMapper;
        this.attachmentMapper = attachmentMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public RepairOrder createOrder(Long residentId, Long categoryId, String title,
                                   String location, String description, String contactPhone) {
        String orderNo = generateOrderNo();

        RepairOrder order = new RepairOrder();
        order.setOrderNo(orderNo);
        order.setResidentId(residentId);
        order.setCategoryId(categoryId);
        order.setTitle(title);
        order.setLocation(location);
        order.setDescription(description);
        order.setContactPhone(contactPhone);
        order.setPriority("NORMAL");
        order.setStatus("PENDING_REVIEW");
        order.setVersion(0);
        orderMapper.insert(order);

        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(order.getId());
        log.setOperatorId(residentId);
        log.setAction("CREATE");
        log.setFromStatus(null);
        log.setToStatus("PENDING_REVIEW");
        log.setRemark("居民提交报修");
        logMapper.insert(log);

        return order;
    }

    public Map<String, Object> listMyOrders(Long residentId, Integer page, Integer pageSize,
                                            String keyword, String status) {
        Page<RepairOrder> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getResidentId, residentId)
                .orderByDesc(RepairOrder::getCreatedAt);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(RepairOrder::getTitle, keyword)
                    .or().like(RepairOrder::getOrderNo, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(RepairOrder::getStatus, status);
        }

        Page<RepairOrder> result = orderMapper.selectPage(p, wrapper);
        List<Map<String, Object>> records = result.getRecords().stream().map(this::toOrderVo).toList();

        Map<String, Object> map = new HashMap<>();
        map.put("page", (int) result.getCurrent());
        map.put("pageSize", (int) result.getSize());
        map.put("total", result.getTotal());
        map.put("records", records);
        return map;
    }

    public RepairOrder getOrderById(Long id) {
        return orderMapper.selectById(id);
    }

    public Map<String, Object> getOrderDetail(Long id) {
        RepairOrder order = orderMapper.selectById(id);
        if (order == null) return null;

        Map<String, Object> detail = toOrderVo(order);

        List<OrderStatusLog> logs = logMapper.selectList(
                new LambdaQueryWrapper<OrderStatusLog>()
                        .eq(OrderStatusLog::getOrderId, id)
                        .orderByAsc(OrderStatusLog::getCreatedAt));
        detail.put("logs", logs);

        List<OrderAttachment> attachments = attachmentMapper.selectList(
                new LambdaQueryWrapper<OrderAttachment>()
                        .eq(OrderAttachment::getOrderId, id));
        detail.put("attachments", attachments);

        SysUser resident = userMapper.selectById(order.getResidentId());
        detail.put("residentName", resident != null ? resident.getRealName() : "");

        if (order.getCurrentWorkerId() != null) {
            SysUser worker = userMapper.selectById(order.getCurrentWorkerId());
            detail.put("workerName", worker != null ? worker.getRealName() : "");
        }

        RepairCategory category = categoryMapper.selectById(order.getCategoryId());
        detail.put("categoryName", category != null ? category.getCategoryName() : "");

        return detail;
    }

    @Transactional
    public void cancelOrder(Long orderId, Long userId, String reason) {
        RepairOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("工单不存在");
        }
        if (!order.getResidentId().equals(userId)) {
            throw new RuntimeException("无权操作该工单");
        }
        OrderStatus currentStatus = OrderStatus.valueOf(order.getStatus());
        if (!currentStatus.canMoveTo(OrderStatus.CANCELLED)) {
            throw new RuntimeException("当前状态不可取消");
        }

        order.setStatus("CANCELLED");
        order.setCancelReason(reason);
        orderMapper.updateById(order);

        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setOperatorId(userId);
        log.setAction("CANCEL");
        log.setFromStatus("PENDING_REVIEW");
        log.setToStatus("CANCELLED");
        log.setRemark(reason);
        logMapper.insert(log);
    }

    public void saveAttachment(OrderAttachment attachment) {
        attachmentMapper.insert(attachment);
    }

    public List<RepairCategory> getCategories() {
        return categoryMapper.findEnabled();
    }

    private Map<String, Object> toOrderVo(RepairOrder order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("orderNo", order.getOrderNo());
        map.put("residentId", order.getResidentId());
        map.put("categoryId", order.getCategoryId());
        map.put("title", order.getTitle());
        map.put("location", order.getLocation());
        map.put("description", order.getDescription());
        map.put("contactPhone", order.getContactPhone());
        map.put("priority", order.getPriority());
        map.put("status", order.getStatus());
        map.put("currentWorkerId", order.getCurrentWorkerId());
        map.put("rejectReason", order.getRejectReason());
        map.put("cancelReason", order.getCancelReason());
        map.put("completionResult", order.getCompletionResult());
        map.put("createdAt", order.getCreatedAt());
        map.put("updatedAt", order.getUpdatedAt());
        return map;
    }

    private String generateOrderNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = orderMapper.selectCount(
                new LambdaQueryWrapper<RepairOrder>()
                        .likeRight(RepairOrder::getOrderNo, "RO" + datePart)) + 1;
        return "RO" + datePart + String.format("%04d", count);
    }
}
