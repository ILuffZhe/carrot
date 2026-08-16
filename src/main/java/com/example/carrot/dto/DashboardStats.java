package com.example.carrot.dto;

import com.example.carrot.model.PointTransaction;
import com.example.carrot.model.Redemption;

import java.util.List;

/**
 * 数据看板聚合数据。
 */
public class DashboardStats {

    private double currentBalance;
    private int todayPositiveCount;   // 今日完成任务数
    private int todayNegativeCount;   // 今日违规数
    private double todayNetChange;    // 今日积分净变动
    private List<TrendPoint> trend7;
    private List<TrendPoint> trend30;
    private List<PointTransaction> recentTransactions; // 最近 10 条
    private List<Redemption> recentRedemptions;        // 最近 5 条

    public double getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(double currentBalance) {
        this.currentBalance = currentBalance;
    }

    public int getTodayPositiveCount() {
        return todayPositiveCount;
    }

    public void setTodayPositiveCount(int todayPositiveCount) {
        this.todayPositiveCount = todayPositiveCount;
    }

    public int getTodayNegativeCount() {
        return todayNegativeCount;
    }

    public void setTodayNegativeCount(int todayNegativeCount) {
        this.todayNegativeCount = todayNegativeCount;
    }

    public double getTodayNetChange() {
        return todayNetChange;
    }

    public void setTodayNetChange(double todayNetChange) {
        this.todayNetChange = todayNetChange;
    }

    public List<TrendPoint> getTrend7() {
        return trend7;
    }

    public void setTrend7(List<TrendPoint> trend7) {
        this.trend7 = trend7;
    }

    public List<TrendPoint> getTrend30() {
        return trend30;
    }

    public void setTrend30(List<TrendPoint> trend30) {
        this.trend30 = trend30;
    }

    public List<PointTransaction> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(List<PointTransaction> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }

    public List<Redemption> getRecentRedemptions() {
        return recentRedemptions;
    }

    public void setRecentRedemptions(List<Redemption> recentRedemptions) {
        this.recentRedemptions = recentRedemptions;
    }
}
