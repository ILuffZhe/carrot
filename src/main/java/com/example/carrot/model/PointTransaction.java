package com.example.carrot.model;

/**
 * 积分流水（唯一账本，只追加），对应 point_transactions 表。
 */
public class PointTransaction {

    private Long id;
    private double changeAmount;  // 正=入账 负=扣减
    private double balanceAfter;  // 变动后余额
    private String type;        // TASK / PENALTY / REDEEM / ADJUST / INTEREST
    private Long refId;         // 关联 tasks.id 或 redemptions.id
    private String description;
    private String createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(double changeAmount) {
        this.changeAmount = changeAmount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(double balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getRefId() {
        return refId;
    }

    public void setRefId(Long refId) {
        this.refId = refId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
