package org.example.genpass.core;

import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class WordlistTest {

    @Test
    public void defaultWordlistHasAbout1296Words() {
        assertEquals(Wordlist.getDefault().size(), 1296);
    }

    @Test
    public void defaultWordsAreLowercaseAscii() {
        Wordlist wordlist = Wordlist.getDefault();
        for (int i = 0; i < wordlist.size(); i++) {
            String word = wordlist.word(i);
            assertTrue(word.matches("[a-z-]+"), "unexpected word: " + word);
        }
    }

    @Test
    public void loadSkipsBlankAndWhitespaceLines() throws IOException {
        try (InputStream in = WordlistTest.class.getResourceAsStream("/test-wordlists/with-blanks.txt")) {
            Wordlist wordlist = Wordlist.load(in);
            assertEquals(wordlist.size(), 4);
            assertEquals(List.of(wordlist.word(0), wordlist.word(1), wordlist.word(2), wordlist.word(3)),
                    List.of("alpha", "beta", "gamma", "delta"));
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyWordlistRejected() throws IOException {
        Wordlist.load(InputStream.nullInputStream());
    }
}
