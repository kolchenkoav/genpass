package org.example.genpass.core;

import java.util.ArrayList;
import java.util.List;

/** Генерация пароля: гарантия ≥1 символа каждого включённого набора + перемешивание Фишера–Йетса. */
public final class PasswordGenerator {

    private final CryptoRandom random;

    public PasswordGenerator() {
        this(CryptoRandom.INSTANCE);
    }

    public PasswordGenerator(CryptoRandom random) {
        this.random = random;
    }

    public String generate(PasswordOptions options) {
        String[] sets = enabledSets(options);
        String alphabet = String.join("", sets);
        char[] result = new char[options.length()];
        for (int i = 0; i < sets.length; i++) {
            result[i] = pick(sets[i]);
        }
        for (int i = sets.length; i < result.length; i++) {
            result[i] = pick(alphabet);
        }
        random.shuffle(result);
        return new String(result);
    }

    /** Размер фактического пула символов после excludeAmbiguous (для оценки энтропии). */
    public static int alphabetSize(PasswordOptions options) {
        return String.join("", enabledSets(options)).length();
    }

    private static String[] enabledSets(PasswordOptions options) {
        List<String> sets = new ArrayList<>(4);
        addIf(sets, options.lowercase(), CharGroups.LOWERCASE, options.excludeAmbiguous());
        addIf(sets, options.uppercase(), CharGroups.UPPERCASE, options.excludeAmbiguous());
        addIf(sets, options.digits(), CharGroups.DIGITS, options.excludeAmbiguous());
        addIf(sets, options.special(), CharGroups.SPECIAL, options.excludeAmbiguous());
        return sets.toArray(String[]::new);
    }

    private static void addIf(List<String> sets, boolean enabled, String set, boolean excludeAmbiguous) {
        if (enabled) {
            String effective = excludeAmbiguous ? filterAmbiguous(set) : set;
            if (effective.isEmpty()) {
                throw new IllegalArgumentException("enabled character set becomes empty after excluding ambiguous characters");
            }
            sets.add(effective);
        }
    }

    private static String filterAmbiguous(String set) {
        StringBuilder filtered = new StringBuilder(set.length());
        for (int i = 0; i < set.length(); i++) {
            char c = set.charAt(i);
            if (CharGroups.AMBIGUOUS.indexOf(c) < 0) {
                filtered.append(c);
            }
        }
        return filtered.toString();
    }

    private char pick(String set) {
        return set.charAt(random.nextInt(set.length()));
    }
}
