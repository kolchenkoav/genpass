package org.example.genpass.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Словарь passphrase: строки UTF-8 из classpath, пустые/пробельные строки пропускаются. */
public final class Wordlist {

    public static final String DEFAULT_RESOURCE = "/wordlists/eff-short.txt";

    private static volatile Wordlist defaultInstance;

    private final List<String> words;

    private Wordlist(List<String> words) {
        this.words = List.copyOf(words);
    }

    /** Ленивый потокобезопасный кэш словаря по умолчанию (EFF short, ~1296 слов). */
    public static Wordlist getDefault() {
        Wordlist wordlist = defaultInstance;
        if (wordlist == null) {
            synchronized (Wordlist.class) {
                wordlist = defaultInstance;
                if (wordlist == null) {
                    try (InputStream in = Wordlist.class.getResourceAsStream(DEFAULT_RESOURCE)) {
                        if (in == null) {
                            throw new IllegalStateException("wordlist resource not found: " + DEFAULT_RESOURCE);
                        }
                        defaultInstance = wordlist = load(in);
                    } catch (IOException e) {
                        throw new UncheckedIOException("failed to load wordlist", e);
                    }
                }
            }
        }
        return wordlist;
    }

    public static Wordlist load(InputStream in) throws IOException {
        List<String> words = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim();
                if (!word.isEmpty()) {
                    words.add(word);
                }
            }
        }
        if (words.isEmpty()) {
            throw new IllegalArgumentException("wordlist is empty");
        }
        return new Wordlist(words);
    }

    public int size() {
        return words.size();
    }

    public String word(int index) {
        return words.get(index);
    }
}
