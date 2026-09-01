package org.example.genpass.core;

/** Генерация PIN: только цифры, опция «без ведущих нулей». */
public final class PinGenerator {

    private final CryptoRandom random;

    public PinGenerator() {
        this(CryptoRandom.INSTANCE);
    }

    public PinGenerator(CryptoRandom random) {
        this.random = random;
    }

    public String generate(PinOptions options) {
        char[] result = new char[options.length()];
        int firstBound = options.noLeadingZero() ? 9 : 10;
        int first = random.nextInt(firstBound) + (options.noLeadingZero() ? 1 : 0);
        result[0] = (char) ('0' + first);
        for (int i = 1; i < result.length; i++) {
            result[i] = (char) ('0' + random.nextInt(10));
        }
        return new String(result);
    }
}
