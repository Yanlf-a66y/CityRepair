package com.cityrepair.service;

import com.cityrepair.dto.*;

import java.util.List;
import java.util.Map;

/**
 * 统计服务接口
 */
public interface StatisticsService {

    /**
     * 获取总览数据：总工单、待处理、已完成、完成率
     */
    Map<String, Object> getOverview();

    /**
     * 状态分布
     */
    List<StatusStat> getStatusDistribution();

    /**
     * 类别分布
     */
    List<CategoryStat> getCategoryDistribution();

    /**
     * 近7天趋势
     */
    List<TrendItem> getTrend();

    /**
     * 维修人员排行
     */
    List<WorkerRankItem> getWorkerRank();
}
