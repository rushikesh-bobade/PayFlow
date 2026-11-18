package com.payflow.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

    public static final int STORAGE_SCALE = 4;
    public static final int DISPLAY_SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    private MoneyUtils() {}

    public static BigDecimal formatStorage(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO.setScale(STORAGE_SCALE, ROUNDING_MODE);
        return amount.setScale(STORAGE_SCALE, ROUNDING_MODE);
    }

    public static BigDecimal formatDisplay(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO.setScale(DISPLAY_SCALE, ROUNDING_MODE);
        return amount.setScale(DISPLAY_SCALE, ROUNDING_MODE);
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
