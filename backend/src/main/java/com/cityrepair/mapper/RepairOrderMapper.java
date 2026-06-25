package com.cityrepair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityrepair.entity.RepairOrder;
<<<<<<< HEAD
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 报修工单Mapper接口
 */
@Mapper
public interface RepairOrderMapper extends BaseMapper<RepairOrder> {

    /**
     * 生成下一个工单编号
     * 格式：RO + 年月日 + 4位序号，如 RO202606250001
     */
    @Select("SELECT CONCAT('RO', DATE_FORMAT(NOW(), '%Y%m%d'), " +
            "LPAD(COALESCE(MAX(CAST(SUBSTRING(order_no, 11) AS UNSIGNED)), 0) + 1, 4, '0')) " +
            "FROM repair_order WHERE order_no LIKE CONCAT('RO', DATE_FORMAT(NOW(), '%Y%m%d'), '%')")
    String generateNextOrderNo(@Param("datePrefix") String datePrefix);
=======

public interface RepairOrderMapper extends BaseMapper<RepairOrder> {
>>>>>>> feat/resident-repair
}
