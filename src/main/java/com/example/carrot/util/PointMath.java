package com.example.carrot.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 积分小数运算工具：统一 2 位小数四舍五入，保证账本余额自洽。
 */
public final class PointMath {

    private PointMath() {
    }

    /**
     * 保留 2 位小数（HALF_UP 四舍五入）。用于 balance_after / change_amount 等账本字段。
     */
    public static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
