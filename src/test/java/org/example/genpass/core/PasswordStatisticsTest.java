package org.example.genpass.core;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class PasswordStatisticsTest {

    private final PasswordGenerator generator = new PasswordGenerator();

    @Test
    public void lowercaseDistributionWithinTwentyPercent() {
        // Длина 1 невалидна (минимум 4) — измеряем распределение первого символа генераций длины 4.
        PasswordOptions o = new PasswordOptions(4, true, false, false, false, false);
        int[] counts = new int[26];
        int iterations = 100_000;
        for (int i = 0; i < iterations; i++) {
            counts[generator.generate(o).charAt(0) - 'a']++;
        }
        double expected = (double) iterations / 26;
        for (int count : counts) {
            assertTrue(count >= expected * 0.8 && count <= expected * 1.2,
                    "count " + count + " outside ±20% of " + expected);
        }
    }
}
