package com.example.carrot.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 任务/记录（登记式：无预创建，记录即完成），对应 tasks 表。
 */
public class Task {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Long id;
    private Long taskTypeId;
    private String title;          // 冗余标题快照
    private String description;
    private double basePoints;     // 三档积分快照
    private double goodPoints;
    private double excellentPoints;
    private String status;         // COMPLETED=已登记生效 / CANCELLED=已撤销
    private Integer tier;          // 正向定档：1=完成 2=良好 3=优秀；惩罚项为 null
    private Double earnedPoints;   // 实际入账/扣减积分（正负，可空）
    private String taskDate;       // 执行日期 YYYY-MM-DD
    private String completedAt;    // 登记入系统时间
    private String photoPaths;     // JSON 数组
    private String remark;
    private String createdBy;      // 操作人用户名（登记人；历史数据为 null）
    private String createdAt;

    /** 解析照片相对路径数组，供模板展示。 */
    public List<String> photoList() {
        if (photoPaths == null || photoPaths.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(photoPaths, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    public boolean isPositive() {
        return tier != null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskTypeId() {
        return taskTypeId;
    }

    public void setTaskTypeId(Long taskTypeId) {
        this.taskTypeId = taskTypeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getBasePoints() {
        return basePoints;
    }

    public void setBasePoints(double basePoints) {
        this.basePoints = basePoints;
    }

    public double getGoodPoints() {
        return goodPoints;
    }

    public void setGoodPoints(double goodPoints) {
        this.goodPoints = goodPoints;
    }

    public double getExcellentPoints() {
        return excellentPoints;
    }

    public void setExcellentPoints(double excellentPoints) {
        this.excellentPoints = excellentPoints;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTier() {
        return tier;
    }

    public void setTier(Integer tier) {
        this.tier = tier;
    }

    public Double getEarnedPoints() {
        return earnedPoints;
    }

    public void setEarnedPoints(Double earnedPoints) {
        this.earnedPoints = earnedPoints;
    }

    public String getTaskDate() {
        return taskDate;
    }

    public void setTaskDate(String taskDate) {
        this.taskDate = taskDate;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }

    public String getPhotoPaths() {
        return photoPaths;
    }

    public void setPhotoPaths(String photoPaths) {
        this.photoPaths = photoPaths;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
