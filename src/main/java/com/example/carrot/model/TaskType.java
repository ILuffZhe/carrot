package com.example.carrot.model;

/**
 * 任务类型（系统内置 + 自定义），对应 task_types 表。
 * <p>POSITIVE=正向任务（三档积分）/ NEGATIVE=惩罚项（单次扣分存 base_points，正数）。</p>
 */
public class TaskType {

    private Long id;
    private String name;
    private String kind;          // POSITIVE / NEGATIVE
    private String description;
    private String icon;          // emoji
    private double basePoints;       // 正向：完成档；惩罚：单次扣分值（正数）
    private double goodPoints;       // 良好档（惩罚项不使用）
    private double excellentPoints;  // 优秀档（惩罚项不使用）
    private boolean builtin;      // 1=系统内置（查询时用别名 is_builtin AS builtin）
    private boolean enabled;      // 1=启用
    private String createdAt;

    public boolean isPositive() {
        return "POSITIVE".equals(kind);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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

    public boolean isBuiltin() {
        return builtin;
    }

    public void setBuiltin(boolean builtin) {
        this.builtin = builtin;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
