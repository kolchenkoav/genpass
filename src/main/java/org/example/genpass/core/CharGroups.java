package org.example.genpass.core;

/** Наборы символов и неоднозначные символы (PLAN 2.1). */
public final class CharGroups {

    public static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    public static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String DIGITS = "0123456789";
    public static final String SPECIAL = "!@#$%^&*()-_=+[]{};:,.<>?/~";

    /** Исключаемые при excludeAmbiguous: I l 1 O 0 и | (в SPECIAL отсутствует — фильтр no-op). */
    public static final String AMBIGUOUS = "Il1O0|";

    private CharGroups() {
    }
}
