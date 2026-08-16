package com.example.carrot.dto;

/**
 * 每日积分趋势聚合点：date=日期，income=入账合计，deduction=扣减合计（正数），net=净变动。
 */
public class TrendPoint {

    private String date;
    private double income;
    private double deduction;
    private double net;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getIncome() {
        return income;
    }

    public void setIncome(double income) {
        this.income = income;
    }

    public double getDeduction() {
        return deduction;
    }

    public void setDeduction(double deduction) {
        this.deduction = deduction;
    }

    public double getNet() {
        return net;
    }

    public void setNet(double net) {
        this.net = net;
    }
}
