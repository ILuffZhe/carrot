package com.example.carrot.dto;

/**
 * 登记表单绑定对象。
 *
 * <p>正向记录使用 positive* 字段 + tier；记违规使用 negative* 字段。</p>
 */
public class TaskForm {

    private String kind;                    // POSITIVE / NEGATIVE

    // 正向任务（记录完成）
    private Long positiveTypeId;            // 为空时使用自定义标题
    private String positiveCustomTitle;
    private Double positiveBasePoints;
    private Double positiveGoodPoints;
    private Double positiveExcellentPoints;
    private Integer tier;                   // 1=完成 2=良好 3=优秀

    // 惩罚项（记违规）
    private Long negativeTypeId;
    private String negativeCustomTitle;
    private Double negativePoints;          // 单次扣分值（正数）

    // 公共
    private String taskDate;                // YYYY-MM-DD
    private String remark;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Long getPositiveTypeId() {
        return positiveTypeId;
    }

    public void setPositiveTypeId(Long positiveTypeId) {
        this.positiveTypeId = positiveTypeId;
    }

    public String getPositiveCustomTitle() {
        return positiveCustomTitle;
    }

    public void setPositiveCustomTitle(String positiveCustomTitle) {
        this.positiveCustomTitle = positiveCustomTitle;
    }

    public Double getPositiveBasePoints() {
        return positiveBasePoints;
    }

    public void setPositiveBasePoints(Double positiveBasePoints) {
        this.positiveBasePoints = positiveBasePoints;
    }

    public Double getPositiveGoodPoints() {
        return positiveGoodPoints;
    }

    public void setPositiveGoodPoints(Double positiveGoodPoints) {
        this.positiveGoodPoints = positiveGoodPoints;
    }

    public Double getPositiveExcellentPoints() {
        return positiveExcellentPoints;
    }

    public void setPositiveExcellentPoints(Double positiveExcellentPoints) {
        this.positiveExcellentPoints = positiveExcellentPoints;
    }

    public Integer getTier() {
        return tier;
    }

    public void setTier(Integer tier) {
        this.tier = tier;
    }

    public Long getNegativeTypeId() {
        return negativeTypeId;
    }

    public void setNegativeTypeId(Long negativeTypeId) {
        this.negativeTypeId = negativeTypeId;
    }

    public String getNegativeCustomTitle() {
        return negativeCustomTitle;
    }

    public void setNegativeCustomTitle(String negativeCustomTitle) {
        this.negativeCustomTitle = negativeCustomTitle;
    }

    public Double getNegativePoints() {
        return negativePoints;
    }

    public void setNegativePoints(Double negativePoints) {
        this.negativePoints = negativePoints;
    }

    public String getTaskDate() {
        return taskDate;
    }

    public void setTaskDate(String taskDate) {
        this.taskDate = taskDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
