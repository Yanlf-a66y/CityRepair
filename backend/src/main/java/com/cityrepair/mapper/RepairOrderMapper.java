package com.cityrepair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityrepair.dto.CategoryStat;
import com.cityrepair.dto.StatusStat;
import com.cityrepair.dto.TrendItem;
import com.cityrepair.dto.WorkerRankItem;
import com.cityrepair.entity.RepairOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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

    // ===== 统计查询 =====

    /** 按状态统计工单数 */
    @Select("SELECT status, COUNT(*) AS count FROM repair_order GROUP BY status ORDER BY count DESC")
    List<StatusStat> countGroupByStatus();

    /** 按类别统计工单数（JOIN 类别名称） */
    @Select("SELECT c.category_name AS categoryName, COUNT(*) AS count " +
            "FROM repair_order o JOIN repair_category c ON o.category_id = c.id " +
            "GROUP BY o.category_id, c.category_name ORDER BY count DESC")
    List<CategoryStat> countGroupByCategory();

    /** 统计待处理工单数（排除终态） */
    @Select("SELECT COUNT(*) FROM repair_order WHERE status NOT IN ('COMPLETED','EVALUATED','REJECTED','CANCELLED')")
    Long countPending();

    /** 统计已完成工单数 */
    @Select("SELECT COUNT(*) FROM repair_order WHERE status IN ('COMPLETED','EVALUATED')")
    Long countCompleted();

    /** 近7天每日新增工单数 */
    @Select("SELECT DATE(created_at) AS date, COUNT(*) AS count " +
            "FROM repair_order WHERE created_at >= #{startDate} " +
            "GROUP BY DATE(created_at) ORDER BY date ASC")
    List<TrendItem> countDailySince(@Param("startDate") String startDate);

    /** 维修人员完成工单排行（TOP 10） */
    @Select("SELECT o.current_worker_id AS workerId, " +
            "COALESCE(u.real_name, u.username) AS workerName, " +
            "COUNT(*) AS completedCount " +
            "FROM repair_order o " +
            "LEFT JOIN sys_user u ON o.current_worker_id = u.id " +
            "WHERE o.status IN ('COMPLETED','EVALUATED') AND o.current_worker_id IS NOT NULL " +
            "GROUP BY o.current_worker_id, u.real_name, u.username " +
            "ORDER BY completedCount DESC LIMIT 10")
    List<WorkerRankItem> countCompletedByWorker();
}
