package com.example.myapplication1;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class RateMathTest {
    @Test
    public void convertsThreeCurrenciesAndRoundsHalfUp() {
        assertEquals("14.00", RateMath.convert("100", 0.14f));
        assertEquals("13.00", RateMath.convert("100", 0.13f));
        assertEquals("19000.00", RateMath.convert("100", 190f));
        assertEquals("0.01", RateMath.convert("0.05", 0.1f));
        assertEquals("0.00", RateMath.convert("0", 0.2f));
    }

    @Test
    public void formatsRatesWithoutScientificNotation() {
        assertEquals("0.000000010", RateMath.formatRate(0.00000001f));
        assertEquals(0.2f, RateMath.parseRate(" 0.2 "), 0f);
    }

    @Test
    public void rejectsInvalidRatesAndAmounts() {
        for (String rate : new String[]{"", "abc", "NaN", "Infinity", "0", "-1", "1e100", "1e-100"}) {
            assertThrows(IllegalArgumentException.class, () -> RateMath.parseRate(rate));
        }
        for (String amount : new String[]{"", "abc", "NaN", "Infinity", "-1"}) {
            assertThrows(IllegalArgumentException.class, () -> RateMath.convert(amount, 0.2f));
        }
    }
}
