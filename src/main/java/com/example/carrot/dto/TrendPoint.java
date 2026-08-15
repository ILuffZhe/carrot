package com.example.carrot.dto;

/**
 * 每日积分趋势聚合点：date=日期，income=入账合计，deduction=扣减合计（正数），net=净变动。
 */
public class TrendPoint {

    private String date;
    private int income;
    private int deduction;
    private int net;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getIncome() {
        return income;
    }

    public void setIncome(int income) {
        this.income = income;
    }

    public int getDeduction() {
        return deduction;
    }

    public void setDeduction(int deduction) {
        this.deduction = deduction;
    }

    public int getNet() {
        return net;
    }

    public void setNet(int net) {
        this.net = net;
    }
}
