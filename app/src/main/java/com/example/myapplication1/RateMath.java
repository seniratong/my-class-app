package com.example.myapplication1;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class RateMath {
    private RateMath() { }

    static float parseRate(String text) {
        float value = Float.parseFloat(text.trim());
        if (!Float.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException("Rate must be positive and finite");
        }
        return value;
    }

    static String formatRate(float value) {
        return new BigDecimal(Float.toString(value)).toPlainString();
    }

    static String convert(String text, float rate) {
        parseRate(Float.toString(rate));
        BigDecimal amount = new BigDecimal(text.trim());
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        return amount.multiply(new BigDecimal(Float.toString(rate)))
                .setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
