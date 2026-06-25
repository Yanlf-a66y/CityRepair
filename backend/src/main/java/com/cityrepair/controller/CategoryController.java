package com.cityrepair.controller;

import com.cityrepair.common.ApiResponse;
import com.cityrepair.entity.RepairCategory;
import com.cityrepair.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 类别管理 Controller
 */
@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 查询所有类别
     */
    @GetMapping
    public ApiResponse<List<RepairCategory>> list() {
        return ApiResponse.success(categoryService.listAll());
    }

    /**
     * 新增类别
     */
    @PostMapping
    public ApiResponse<Void> create(@RequestBody RepairCategory category) {
        categoryService.create(category);
        return ApiResponse.success("创建成功", null);
    }

    /**
     * 修改类别
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody RepairCategory category) {
        category.setId(id);
        categoryService.update(category);
        return ApiResponse.success("修改成功", null);
    }

    /**
     * 启停类别
     */
    @PutMapping("/{id}/status")
    public ApiResponse<Void> toggleStatus(@PathVariable Long id) {
        categoryService.toggleStatus(id);
        return ApiResponse.success("状态已切换", null);
    }

    /**
     * 删除类别（有关联工单时不可删除）
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
