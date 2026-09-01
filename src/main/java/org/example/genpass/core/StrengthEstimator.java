package org.example.genpass.core;

/**
 * Оценка стойкости: энтропия в битах, словесная метка (PLAN 2.4) и справочное время
 * перебора при офлайн-атаке 10^10 попыток/сек.
 */
public final class StrengthEstimator {

    private static final double LOG2 = Math.log(2);
    private static final double LOG2_10 = Math.log(10) / LOG2;
    private static final double ATTEMPTS_PER_SECOND = 1e10;
    private static final double SECONDS_PER_YEAR = 31557600.0;

    private StrengthEstimator() {
    }

    public static double entropyBits(PasswordOptions options) {
        return options.length() * log2(PasswordGenerator.alphabetSize(options));
    }

    /** Для PIN с noLeadingZero — точная формула log2(9) + (n-1)·log2(10). */
    public static double entropyBits(PinOptions options) {
        return options.noLeadingZero()
                ? log2(9) + (options.length() - 1) * LOG2_10
                : options.length() * LOG2_10;
    }

    public static double entropyBits(PassphraseOptions options) {
        return entropyBits(options, Wordlist.getDefault().size());
    }

    public static double entropyBits(PassphraseOptions options, int dictionarySize) {
        return options.wordCount() * log2(dictionarySize) + (options.addDigit() ? LOG2_10 : 0);
    }

    public static String strengthLabel(double entropyBits) {
        if (entropyBits < 40) {
            return "слабый";
        }
        if (entropyBits < 60) {
            return "средний";
        }
        if (entropyBits < 80) {
            return "сильный";
        }
        return "очень сильный";
    }

    public static String crackTime(double entropyBits) {
        double seconds = Math.pow(2, entropyBits) / ATTEMPTS_PER_SECOND;
        if (seconds < 1) {
            return "<1 сек";
        }
        if (seconds < 60) {
            return "~" + Math.round(seconds) + " сек";
        }
        if (seconds < 3600) {
            return "~" + Math.round(seconds / 60) + " мин";
        }
        if (seconds < 86400) {
            return "~" + Math.round(seconds / 3600) + " час";
        }
        if (seconds < SECONDS_PER_YEAR) {
            return "~" + Math.round(seconds / 86400) + " дней";
        }
        return "~" + Math.round(seconds / SECONDS_PER_YEAR) + " лет";
    }

    private static double log2(double x) {
        return Math.log(x) / LOG2;
    }
}
