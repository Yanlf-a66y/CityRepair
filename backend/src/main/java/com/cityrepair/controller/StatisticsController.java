package com.cityrepair.controller;

import com.cityrepair.common.ApiResponse;
import com.cityrepair.dto.*;
import com.cityrepair.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 统计看板 Controller
 */
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * 统计总览：总工单数、待处理数、已完成数、完成率
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.success(statisticsService.getOverview());
    }

    /**
     * 状态分布：各状态下的工单数量
     */
    @GetMapping("/status")
    public ApiResponse<List<StatusStat>> statusDistribution() {
        return ApiResponse.success(statisticsService.getStatusDistribution());
    }

    /**
     * 类别分布：各类别下的工单数量
     */
    @GetMapping("/category")
    public ApiResponse<List<CategoryStat>> categoryDistribution() {
        return ApiResponse.success(statisticsService.getCategoryDistribution());
    }

    /**
     * 近7天趋势：每日新增工单数
     */
    @GetMapping("/trend")
    public ApiResponse<List<TrendItem>> trend() {
        return ApiResponse.success(statisticsService.getTrend());
    }

    /**
     * 维修人员排行：完成工单数 TOP 10
     */
    @GetMapping("/worker-rank")
    public ApiResponse<List<WorkerRankItem>> workerRank() {
        return ApiResponse.success(statisticsService.getWorkerRank());
    }
}
