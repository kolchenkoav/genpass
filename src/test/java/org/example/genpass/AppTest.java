package org.example.genpass;

import org.testng.annotations.Test;

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
    public void mainRunsAndExits() {
        App.main(new String[0]);
    }
}
