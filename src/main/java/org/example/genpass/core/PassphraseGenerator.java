package org.example.genpass.core;

/**
 * Генерация passphrase из словаря: количество слов, разделитель, капитализация,
 * цифра в конец одного случайного слова (решение A2, PLAN 2.2).
 */
public final class PassphraseGenerator {

    private final CryptoRandom random;
    private final Wordlist wordlist;

    public PassphraseGenerator() {
        this(CryptoRandom.INSTANCE, Wordlist.getDefault());
    }

    public PassphraseGenerator(CryptoRandom random, Wordlist wordlist) {
        this.random = random;
        this.wordlist = wordlist;
    }

    public String generate(PassphraseOptions options) {
        String[] words = new String[options.wordCount()];
        for (int i = 0; i < words.length; i++) {
            String word = wordlist.word(random.nextInt(wordlist.size()));
            words[i] = options.capitalize() ? capitalize(word) : word;
        }
        if (options.addDigit()) {
            int target = random.nextInt(words.length);
            words[target] = words[target] + (char) ('0' + random.nextInt(10));
        }
        return String.join(options.separator(), words);
    }

    private static String capitalize(String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }
}
