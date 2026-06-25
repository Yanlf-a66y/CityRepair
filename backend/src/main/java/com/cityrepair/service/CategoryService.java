package com.cityrepair.service;

import com.cityrepair.entity.RepairCategory;

import java.util.List;

/**
 * 类别管理服务接口
 */
public interface CategoryService {

    /**
     * 查询所有类别（按排序号升序）
     */
    List<RepairCategory> listAll();

    /**
     * 新增类别
     */
    void create(RepairCategory category);

    /**
     * 修改类别
     */
    void update(RepairCategory category);

    /**
     * 启停类别
     */
    void toggleStatus(Long id);

    /**
     * 删除类别（关联工单检查不通过则抛异常）
     */
    void delete(Long id);
}
