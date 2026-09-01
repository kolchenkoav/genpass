package org.example.genpass.core;

/** Параметры генерации пароля: длина 4–128, хотя бы один набор символов включён. */
public record PasswordOptions(int length, boolean lowercase, boolean uppercase,
                              boolean digits, boolean special, boolean excludeAmbiguous) {

    public PasswordOptions {
        if (length < 4 || length > 128) {
            throw new IllegalArgumentException("length must be between 4 and 128");
        }
        if (!lowercase && !uppercase && !digits && !special) {
            throw new IllegalArgumentException("at least one character set must be enabled");
        }
    }
}
