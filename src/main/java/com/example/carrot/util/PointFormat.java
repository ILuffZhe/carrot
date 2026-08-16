package com.example.carrot.util;

import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * 积分显示格式化器（模板通过 ${@pointFormat.fmt(x)} / ${@pointFormat.fmtSigned(x)} 调用）。
 * 规则：最多 2 位小数、去尾零（5 → "5"、5.5 → "5.5"、5.25 → "5.25"）。
 */
@Component
public class PointFormat {

    private final DecimalFormat df;

    public PointFormat() {
        this.df = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
        this.df.setRoundingMode(RoundingMode.HALF_UP);
    }

    public String fmt(double value) {
        return df.format(value);
    }

    /** 带符号显示：非负加 + 前缀，负数自带负号。 */
    public String fmtSigned(double value) {
        return value >= 0 ? "+" + df.format(value) : df.format(value);
    }
}
