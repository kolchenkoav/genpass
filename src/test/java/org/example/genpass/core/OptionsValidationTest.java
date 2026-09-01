package org.example.genpass.core;

import org.testng.annotations.Test;

public class OptionsValidationTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void passwordLengthTooShort() {
        new PasswordOptions(3, true, false, false, false, false);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void passwordLengthTooLong() {
        new PasswordOptions(129, true, false, false, false, false);
    }

    @Test
    public void passwordBoundaryLengthsValid() {
        new PasswordOptions(4, true, false, false, false, false);
        new PasswordOptions(128, true, false, false, false, false);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void passwordRequiresAtLeastOneSet() {
        new PasswordOptions(10, false, false, false, false, false);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void passphraseTooFewWords() {
        new PassphraseOptions(2, "-", false, false);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void passphraseTooManyWords() {
        new PassphraseOptions(13, "-", false, false);
    }

    @Test
    public void passphraseBoundaryWordCountsValid() {
        new PassphraseOptions(3, "-", false, false);
        new PassphraseOptions(12, "-", false, false);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void passphraseRejectsUnknownSeparator() {
        new PassphraseOptions(4, "|", false, false);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void passphraseRejectsNullSeparator() {
        new PassphraseOptions(4, null, false, false);
    }

    @Test
    public void passphraseAllowsAllSeparators() {
        new PassphraseOptions(4, "-", false, false);
        new PassphraseOptions(4, "_", false, false);
        new PassphraseOptions(4, " ", false, false);
        new PassphraseOptions(4, "", false, false);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void pinTooShort() {
        new PinOptions(2, false);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void pinTooLong() {
        new PinOptions(13, false);
    }

    @Test
    public void pinBoundaryLengthsValid() {
        new PinOptions(3, false);
        new PinOptions(12, false);
    }
}
