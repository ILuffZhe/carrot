package com.example.carrot.service;

import com.example.carrot.dto.DashboardStats;
import com.example.carrot.dto.TrendPoint;
import com.example.carrot.repository.PointRepository;
import com.example.carrot.repository.RedemptionRepository;
import com.example.carrot.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据看板聚合：当前积分、今日概览、7/30 天趋势、最近流水与兑换。
 */
@Service
public class DashboardService {

    private final PointRepository pointRepository;
    private final TaskRepository taskRepository;
    private final RedemptionRepository redemptionRepository;

    public DashboardService(PointRepository pointRepository,
                            TaskRepository taskRepository,
                            RedemptionRepository redemptionRepository) {
        this.pointRepository = pointRepository;
        this.taskRepository = taskRepository;
        this.redemptionRepository = redemptionRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStats stats() {
        LocalDate today = LocalDate.now();
        DashboardStats stats = new DashboardStats();
        stats.setCurrentBalance(pointRepository.getCurrentBalance());
        stats.setTodayPositiveCount(taskRepository.countByDateAndTier(today.toString(), true));
        stats.setTodayNegativeCount(taskRepository.countByDateAndTier(today.toString(), false));
        stats.setTodayNetChange(pointRepository.sumChangeOn(today.toString()));
        stats.setTrend7(trend(7));
        stats.setTrend30(trend(30));
        stats.setRecentTransactions(pointRepository.findRecent(10));
        stats.setRecentRedemptions(redemptionRepository.findRecent(5));
        return stats;
    }

    /**
     * 最近 N 天（含今天）逐日趋势，无流水的日期补 0，保证图表日期连续。
     */
    private List<TrendPoint> trend(int days) {
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        Map<String, TrendPoint> byDate = new HashMap<>();
        for (TrendPoint p : pointRepository.dailyTrend(start.toString())) {
            byDate.put(p.getDate(), p);
        }
        List<TrendPoint> result = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            String date = start.plusDays(i).toString();
            TrendPoint p = byDate.get(date);
            if (p == null) {
                p = new TrendPoint();
                p.setDate(date);
                p.setIncome(0);
                p.setDeduction(0);
                p.setNet(0);
            }
            result.add(p);
        }
        return result;
    }
}
