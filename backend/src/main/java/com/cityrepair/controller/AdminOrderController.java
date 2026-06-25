package com.cityrepair.controller;

import com.cityrepair.common.ApiResponse;
import com.cityrepair.common.PageQuery;
import com.cityrepair.dto.ApproveRequest;
import com.cityrepair.dto.AssignRequest;
import com.cityrepair.dto.RejectRequest;
import com.cityrepair.entity.SysUser;
import com.cityrepair.service.AdminOrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping("/orders")
    public ApiResponse<Map<String, Object>> orders(@ModelAttribute PageQuery query) {
        return adminOrderService.listOrders(query);
    }

    @PutMapping("/orders/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id,
                                     @Valid @RequestBody ApproveRequest request) {
        return adminOrderService.approve(id, request);
    }

    @PutMapping("/orders/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id,
                                    @Valid @RequestBody RejectRequest request) {
        return adminOrderService.reject(id, request);
    }

    @PutMapping("/orders/{id}/assign")
    public ApiResponse<Void> assign(@PathVariable Long id,
                                    @Valid @RequestBody AssignRequest request) {
        return adminOrderService.assign(id, request);
    }

    @GetMapping("/workers")
    public ApiResponse<List<SysUser>> workers() {
        return adminOrderService.getWorkers();
    }
}
