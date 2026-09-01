package org.example.genpass.core;

import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PassphraseGeneratorTest {

    private static final List<String> FIXTURE_WORDS = List.of("alpha", "beta", "gamma", "delta");

    private static Wordlist fixture() throws IOException {
        try (InputStream in = PassphraseGeneratorTest.class
                .getResourceAsStream("/test-wordlists/with-blanks.txt")) {
            return Wordlist.load(in);
        }
    }

    @Test
    public void wordsComeFromDictionary() throws IOException {
        PassphraseGenerator generator = new PassphraseGenerator(CryptoRandom.INSTANCE, fixture());
        PassphraseOptions o = new PassphraseOptions(5, "-", false, false);
        for (int i = 0; i < 50; i++) {
            String[] words = generator.generate(o).split("-");
            assertEquals(words.length, 5);
            for (String word : words) {
                assertTrue(FIXTURE_WORDS.contains(word), "word not in dictionary: " + word);
            }
        }
    }

    @Test
    public void separatorVariantsJoinWords() throws IOException {
        PassphraseGenerator generator = new PassphraseGenerator(CryptoRandom.INSTANCE, fixture());
        for (String separator : new String[]{"-", "_", " "}) {
            String result = generator.generate(new PassphraseOptions(4, separator, false, false));
            assertEquals(result.split(java.util.regex.Pattern.quote(separator)).length, 4);
        }
        int empty = generator.generate(new PassphraseOptions(4, "", false, false)).length();
        assertTrue(empty >= 16 && empty <= 20, "concatenated length out of range: " + empty);
    }

    @Test
    public void capitalizeFirstLetterOfEachWord() throws IOException {
        PassphraseGenerator generator = new PassphraseGenerator(CryptoRandom.INSTANCE, fixture());
        PassphraseOptions o = new PassphraseOptions(4, "-", true, false);
        for (int i = 0; i < 50; i++) {
            for (String word : generator.generate(o).split("-")) {
                assertTrue(Character.isUpperCase(word.charAt(0)), "not capitalized: " + word);
                assertEquals(word, word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase());
            }
        }
    }

    @Test
    public void addDigitAppendsToExactlyOneWord() throws IOException {
        PassphraseGenerator generator = new PassphraseGenerator(CryptoRandom.INSTANCE, fixture());
        PassphraseOptions o = new PassphraseOptions(5, "-", false, true);
        for (int i = 0; i < 100; i++) {
            long digitWords = 0;
            for (String word : generator.generate(o).split("-")) {
                char last = word.charAt(word.length() - 1);
                if (last >= '0' && last <= '9') {
                    digitWords++;
                    assertTrue(FIXTURE_WORDS.contains(word.substring(0, word.length() - 1)),
                            "digit must be appended to a dictionary word: " + word);
                }
            }
            assertEquals(digitWords, 1, "exactly one word must carry the digit");
        }
    }

    @Test
    public void repeatedGenerationsDiffer() {
        PassphraseGenerator generator = new PassphraseGenerator();
        PassphraseOptions o = new PassphraseOptions(5, "-", false, false);
        Set<String> results = new java.util.HashSet<>();
        for (int i = 0; i < 5; i++) {
            results.add(generator.generate(o));
        }
        assertEquals(results.size(), 5, "generated passphrases must differ");
    }

    @Test
    public void entropyWithFourWordDictionaryIsExact() {
        assertEquals(StrengthEstimator.entropyBits(new PassphraseOptions(5, "-", false, false), 4), 10.0, 1e-9);
        assertEquals(StrengthEstimator.entropyBits(new PassphraseOptions(5, "-", false, true), 4), 13.3219, 0.01);
    }
}
