package com.example.myapplication1;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ScoreInputTest {
    @Test
    public void acceptsZeroPositiveAndMaximumScores() {
        assertEquals(0, ScoreInput.parse("0"));
        assertEquals(42, ScoreInput.parse(" 42 "));
        assertEquals(Integer.MAX_VALUE, ScoreInput.parse("2147483647"));
    }

    @Test
    public void rejectsEmptyNegativeDecimalTextAndOverflowScores() {
        for (String value : new String[]{"", " ", "-1", "1.5", "abc", "2147483648"}) {
            assertThrows(value, IllegalArgumentException.class, () -> ScoreInput.parse(value));
        }
    }
}
