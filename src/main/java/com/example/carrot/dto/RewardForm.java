package com.example.carrot.dto;

/**
 * 奖励新建/编辑表单绑定对象（图片单独以 MultipartFile 接收）。
 */
public class RewardForm {

    private String name;
    private String type;        // PHYSICAL=实物 / ACTIVITY=活动
    private String description;
    private Double pointsCost;  // 所需积分
    private Integer stock;      // 库存，null=不限量

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPointsCost() {
        return pointsCost;
    }

    public void setPointsCost(Double pointsCost) {
        this.pointsCost = pointsCost;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
}
