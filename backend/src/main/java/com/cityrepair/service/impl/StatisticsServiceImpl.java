package com.cityrepair.service.impl;

import com.cityrepair.dto.*;
import com.cityrepair.mapper.RepairOrderMapper;
import com.cityrepair.service.StatisticsService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final RepairOrderMapper orderMapper;

    public StatisticsServiceImpl(RepairOrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public Map<String, Object> getOverview() {
        Long totalOrders = orderMapper.selectCount(null);
        Long pendingOrders = orderMapper.countPending();
        Long completedOrders = orderMapper.countCompleted();
        int completionRate = totalOrders > 0
                ? (int) Math.round(completedOrders * 100.0 / totalOrders)
                : 0;

        return Map.of(
                "totalOrders", totalOrders,
                "pendingOrders", pendingOrders,
                "completedOrders", completedOrders,
                "completionRate", completionRate
        );
    }

    @Override
    public List<StatusStat> getStatusDistribution() {
        return orderMapper.countGroupByStatus();
    }

    @Override
    public List<CategoryStat> getCategoryDistribution() {
        return orderMapper.countGroupByCategory();
    }

    @Override
    public List<TrendItem> getTrend() {
        String startDate = LocalDate.now().minusDays(6)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
        return orderMapper.countDailySince(startDate);
    }

    @Override
    public List<WorkerRankItem> getWorkerRank() {
        return orderMapper.countCompletedByWorker();
    }
}
