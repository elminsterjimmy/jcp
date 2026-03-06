package com.elminster.minilang;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Utility methods for MiniLang tests.
 */
public class TestUtils {

    /**
     * Loads a test script from the test-scripts resources directory.
     *
     * @param filename the script filename (e.g., "simple-arithmetic-test.minilang")
     * @return the script source code
     * @throws Exception if the script cannot be loaded
     */
    public static String loadTestScript(String filename) throws Exception {
        try (InputStream is = TestUtils.class.getClassLoader()
                .getResourceAsStream("test-scripts/" + filename)) {
            if (is == null) {
                fail("Test script not found: " + filename);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
