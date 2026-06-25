package com.cityrepair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cityrepair.entity.RepairCategory;
import com.cityrepair.mapper.RepairCategoryMapper;
import com.cityrepair.mapper.RepairOrderMapper;
import com.cityrepair.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<RepairCategoryMapper, RepairCategory> implements CategoryService {

    private final RepairOrderMapper orderMapper;

    public CategoryServiceImpl(RepairOrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public List<RepairCategory> listAll() {
        LambdaQueryWrapper<RepairCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(RepairCategory::getSortOrder);
        return list(wrapper);
    }

    @Override
    @Transactional
    public void create(RepairCategory category) {
        save(category);
    }

    @Override
    @Transactional
    public void update(RepairCategory category) {
        // 只更新非空字段
        RepairCategory existing = getById(category.getId());
        if (existing == null) {
            throw new RuntimeException("类别不存在");
        }
        updateById(category);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        RepairCategory category = getById(id);
        if (category == null) {
            throw new RuntimeException("类别不存在");
        }
        category.setEnabled(category.getEnabled() == 1 ? 0 : 1);
        updateById(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 检查是否有关联工单
        Long count = orderMapper.selectCount(
                new LambdaQueryWrapper<com.cityrepair.entity.RepairOrder>()
                        .eq(com.cityrepair.entity.RepairOrder::getCategoryId, id)
        );
        if (count != null && count > 0) {
            throw new RuntimeException("该类别下存在 " + count + " 条工单，无法删除。请先处理关联工单或停用该类别。");
        }
        removeById(id);
    }
}
