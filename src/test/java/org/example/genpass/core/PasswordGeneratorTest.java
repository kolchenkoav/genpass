package org.example.genpass.core;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PasswordGeneratorTest {

    private final PasswordGenerator generator = new PasswordGenerator();

    private static final String ALL = CharGroups.LOWERCASE + CharGroups.UPPERCASE
            + CharGroups.DIGITS + CharGroups.SPECIAL;

    @Test
    public void resultLengthMatchesOptions() {
        for (int length : new int[]{4, 8, 64, 128}) {
            PasswordOptions o = new PasswordOptions(length, true, true, true, true, false);
            assertEquals(generator.generate(o).length(), length);
        }
    }

    @Test
    public void charactersBelongToEnabledSets() {
        PasswordOptions o = new PasswordOptions(20, true, true, true, true, false);
        for (int i = 0; i < 200; i++) {
            for (char c : generator.generate(o).toCharArray()) {
                assertTrue(ALL.indexOf(c) >= 0, "char outside alphabet: " + c);
            }
        }
    }

    @Test
    public void excludeAmbiguousRemovesAmbiguousChars() {
        PasswordOptions o = new PasswordOptions(20, true, true, true, true, true);
        for (int i = 0; i < 500; i++) {
            for (char c : generator.generate(o).toCharArray()) {
                assertFalse(CharGroups.AMBIGUOUS.indexOf(c) >= 0, "ambiguous char leaked: " + c);
            }
        }
    }

    @Test
    public void everyEnabledSetIsRepresented() {
        PasswordOptions o = new PasswordOptions(4, true, true, true, true, false);
        for (int i = 0; i < 1000; i++) {
            String result = generator.generate(o);
            assertTrue(containsAny(result, CharGroups.LOWERCASE), "no lowercase: " + result);
            assertTrue(containsAny(result, CharGroups.UPPERCASE), "no uppercase: " + result);
            assertTrue(containsAny(result, CharGroups.DIGITS), "no digit: " + result);
            assertTrue(containsAny(result, CharGroups.SPECIAL), "no special: " + result);
        }
    }

    @Test
    public void alphabetSizeMatchesPoolAfterExclusion() {
        assertEquals(PasswordGenerator.alphabetSize(new PasswordOptions(10, true, true, true, true, false)), 89);
        assertEquals(PasswordGenerator.alphabetSize(new PasswordOptions(10, true, true, true, true, true)), 84);
        assertEquals(PasswordGenerator.alphabetSize(new PasswordOptions(10, true, false, false, false, false)), 26);
        assertEquals(PasswordGenerator.alphabetSize(new PasswordOptions(10, false, false, true, false, true)), 8);
    }

    private static boolean containsAny(String result, String set) {
        for (char c : result.toCharArray()) {
            if (set.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }
}
