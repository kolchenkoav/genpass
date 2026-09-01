package org.example.genpass.core;

/** Параметры passphrase: 3–12 слов, разделитель из {-,_," ",""}, капитализация, цифра. */
public record PassphraseOptions(int wordCount, String separator, boolean capitalize, boolean addDigit) {

    public PassphraseOptions {
        if (wordCount < 3 || wordCount > 12) {
            throw new IllegalArgumentException("wordCount must be between 3 and 12");
        }
        if (separator == null || !(separator.isEmpty() || separator.equals("-")
                || separator.equals("_") || separator.equals(" "))) {
            throw new IllegalArgumentException("separator must be one of: \"-\", \"_\", \" \" or empty");
        }
    }
}
