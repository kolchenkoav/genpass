package org.example.genpass.core;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PinGeneratorTest {

    private final PinGenerator generator = new PinGenerator();

    @Test
    public void digitsOnlyAndLengthMatches() {
        for (int length : new int[]{3, 6, 12}) {
            PinOptions o = new PinOptions(length, false);
            for (int i = 0; i < 100; i++) {
                String pin = generator.generate(o);
                assertEquals(pin.length(), length);
                for (char c : pin.toCharArray()) {
                    assertTrue(c >= '0' && c <= '9', "non-digit char: " + c);
                }
            }
        }
    }

    @Test
    public void noLeadingZeroNeverStartsWithZero() {
        PinOptions o = new PinOptions(8, true);
        for (int i = 0; i < 500; i++) {
            assertTrue(generator.generate(o).charAt(0) != '0', "leading zero with noLeadingZero");
        }
    }

    @Test
    public void leadingZeroAllowedSometimes() {
        PinOptions o = new PinOptions(8, false);
        boolean sawZero = false;
        for (int i = 0; i < 500 && !sawZero; i++) {
            sawZero = generator.generate(o).charAt(0) == '0';
        }
        assertTrue(sawZero, "no leading zero in 500 tries with leading zeros allowed");
    }
}
