package com.cityrepair.controller;

import com.cityrepair.common.ApiResponse;
import com.cityrepair.common.PageQuery;
import com.cityrepair.entity.OrderStatusLog;
import com.cityrepair.enums.OrderStatus;
import com.cityrepair.service.AdminOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/repair-orders")
public class RepairOrderController {

    private final AdminOrderService adminOrderService;

    public RepairOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping("/statuses")
    public ApiResponse<List<String>> statuses() {
        return ApiResponse.success(Arrays.stream(OrderStatus.values())
                .map(Enum::name).toList());
    }

    @GetMapping("/my")
    public ApiResponse<Map<String, Object>> myOrders(@ModelAttribute PageQuery query) {
        return ApiResponse.success(Map.of(
                "page", query.getPage(),
                "pageSize", query.getPageSize(),
                "total", 0,
                "records", List.of()
        ));
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<List<OrderStatusLog>> logs(@PathVariable Long id) {
        return adminOrderService.getLogs(id);
    }
}
