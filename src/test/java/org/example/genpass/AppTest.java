package org.example.genpass;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class AppTest {

    @Test
    public void versionIsDefined() {
        String version = App.version();
        assertFalse(version.isBlank());
        assertTrue(version.matches("\\d+\\.\\d+(\\.\\d+)?(-SNAPSHOT)?"), "unexpected version format: " + version);
    }

    @Test
    public void parsePortDefaultsAndValidValues() {
        assertEquals(App.parsePort(null), 8080);
        assertEquals(App.parsePort(""), 8080);
        assertEquals(App.parsePort("8080"), 8080);
        assertEquals(App.parsePort(" 9090 "), 9090);
        assertEquals(App.parsePort("65535"), 65535);
        assertEquals(App.parsePort("1"), 1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void parsePortRejectsZero() {
        App.parsePort("0");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void parsePortRejectsOutOfRange() {
        App.parsePort("70000");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void parsePortRejectsNonNumeric() {
        App.parsePort("abc");
    }
}
