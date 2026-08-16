package com.example.carrot.model;

/**
 * 兑换记录，对应 redemptions 表。
 */
public class Redemption {

    private Long id;
    private Long rewardId;
    private String rewardName;  // 冗余快照
    private int pointsCost;     // 冗余快照
    private String status;      // PENDING=待发放 / DONE=已发放 / CANCELLED=已取消（退回积分）
    private String redeemedAt;  // 发起兑换时间
    private String completedAt; // 发放/取消时间
    private String note;
    private String createdBy;   // 操作人用户名（兑换发起人；历史数据为 null）
    private String createdAt;

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRewardId() {
        return rewardId;
    }

    public void setRewardId(Long rewardId) {
        this.rewardId = rewardId;
    }

    public String getRewardName() {
        return rewardName;
    }

    public void setRewardName(String rewardName) {
        this.rewardName = rewardName;
    }

    public int getPointsCost() {
        return pointsCost;
    }

    public void setPointsCost(int pointsCost) {
        this.pointsCost = pointsCost;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRedeemedAt() {
        return redeemedAt;
    }

    public void setRedeemedAt(String redeemedAt) {
        this.redeemedAt = redeemedAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
