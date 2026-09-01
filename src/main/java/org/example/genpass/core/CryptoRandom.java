package org.example.genpass.core;

import java.security.SecureRandom;

/**
 * Единственный источник случайности ядра: {@link SecureRandom} (КСГП, потокобезопасный).
 * Запрещены {@link java.util.Random}, Math.random(), currentTimeMillis.
 */
public final class CryptoRandom {

    public static final CryptoRandom INSTANCE = new CryptoRandom();

    private final SecureRandom random = new SecureRandom();

    private CryptoRandom() {
    }

    /** Равномерное целое в [0, bound) без modulo bias (bound > 0). */
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    /** Перемешивание Фишера–Йетса на месте. */
    public void shuffle(char[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }

    /** Перемешивание Фишера–Йетса на месте. */
    public void shuffle(String[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            String tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
}
