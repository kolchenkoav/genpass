package org.example.genpass.core;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class StrengthEstimatorTest {

    @Test
    public void passwordEntropyPointValues() {
        assertEquals(StrengthEstimator.entropyBits(new PasswordOptions(8, true, false, false, false, false)), 37.60, 0.01);
        assertEquals(StrengthEstimator.entropyBits(new PasswordOptions(20, true, true, true, true, false)), 129.51, 0.01);
        assertEquals(StrengthEstimator.entropyBits(new PasswordOptions(20, true, true, true, true, true)), 127.85, 0.01);
    }

    @Test
    public void pinEntropyPointValues() {
        assertEquals(StrengthEstimator.entropyBits(new PinOptions(6, false)), 19.93, 0.01);
        assertEquals(StrengthEstimator.entropyBits(new PinOptions(6, true)), 19.78, 0.01);
    }

    @Test
    public void passphraseEntropyPointValues() {
        assertEquals(StrengthEstimator.entropyBits(new PassphraseOptions(5, "-", false, false)), 51.70, 0.01);
        assertEquals(StrengthEstimator.entropyBits(new PassphraseOptions(5, "-", false, true)), 55.02, 0.01);
    }

    @Test
    public void strengthLabels() {
        assertEquals(StrengthEstimator.strengthLabel(39.9), "слабый");
        assertEquals(StrengthEstimator.strengthLabel(40), "средний");
        assertEquals(StrengthEstimator.strengthLabel(59.9), "средний");
        assertEquals(StrengthEstimator.strengthLabel(60), "сильный");
        assertEquals(StrengthEstimator.strengthLabel(79.9), "сильный");
        assertEquals(StrengthEstimator.strengthLabel(80), "очень сильный");
        assertEquals(StrengthEstimator.strengthLabel(120), "очень сильный");
    }

    @Test
    public void crackTimeAnchors() {
        assertEquals(StrengthEstimator.crackTime(0), "<1 сек");
        assertEquals(StrengthEstimator.crackTime(37.6), "~21 сек");
        assertEquals(StrengthEstimator.crackTime(40), "~2 мин");
    }
}
