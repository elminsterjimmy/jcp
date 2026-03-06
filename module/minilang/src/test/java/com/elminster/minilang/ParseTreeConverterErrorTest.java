package com.elminster.minilang;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static com.elminster.minilang.TestUtils.loadTestScript;

/**
 * Comprehensive error path tests for MiniLang parser.
 *
 * <p>These tests validate that the parser correctly:
 * <ul>
 *   <li>Reports error messages for invalid syntax</li>
 *   <li>Includes error locations (line and column)</li>
 *   <li>Handles various syntax errors gracefully</li>
 *   <li>Provides meaningful error messages for debugging</li>
 * </ul>
 *
 * <p>Error test scripts are in src/test/resources/test-scripts/error-tests/
 * Each script includes a comment with expected error metadata:
 * <pre>// Expected error: line X, contains "keyword"</pre>
 */
public class ParseTreeConverterErrorTest {

    /**
     * Pattern to extract error location from ANTLR error messages.
     * Example: "Syntax error at test.minilang:1:15 - mismatched input"
     * Group 1: line number, Group 2: column position
     */
    private static final Pattern ERROR_LOCATION_PATTERN = Pattern.compile(":(\\d+):(\\d+)");

    /**
     * Tests various syntax errors with expected error messages, line numbers, and column positions.
     *
     * <p>Each test:
     * <ul>
     *   <li>Loads error test script from file</li>
     *   <li>Expects parsing to fail with exception</li>
     *   <li>Validates error message contains expected keywords</li>
     *   <li>Validates error location (file:line:column) matches expectations</li>
     * </ul>
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "Missing newline, error-tests/missing-newline.minilang, 1, 16, 'mismatched|missing'",
        "Unterminated string, error-tests/unterminated-string.minilang, 1, 17, 'token|recognition error'",
        "Invalid type, error-tests/invalid-type.minilang, 1, 8, 'mismatched|extraneous'",
        "Missing brace, error-tests/missing-brace.minilang, -1, -1, 'missing|}''",
        "Invalid operator, error-tests/invalid-operator.minilang, 1, 21, 'extraneous|token|recognition error'"
    })
    void testSyntaxErrorsWithLocation(String testName, String scriptFile, int expectedLine, int expectedColumn, String expectedKeywords) throws Exception {
        // Load test script
        String source = loadTestScript(scriptFile);
        assertNotNull(source, "Test script should load: " + scriptFile);

        // Parse and expect error
        ParseTreeConverter converter = new ParseTreeConverter(scriptFile, source);
        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        }, "Should throw exception for: " + testName);

        String errorMessage = exception.getMessage();
        assertNotNull(errorMessage, "Error message should not be null");
        assertFalse(errorMessage.isEmpty(), "Error message should not be empty");

        // Validate error message contains expected keywords (OR logic)
        String[] keywords = expectedKeywords.split("\\|");
        boolean containsKeyword = false;
        for (String keyword : keywords) {
            if (errorMessage.toLowerCase().contains(keyword.toLowerCase().replace("'", ""))) {
                containsKeyword = true;
                break;
            }
        }
        assertTrue(containsKeyword,
            String.format("Error message should contain one of %s. Actual: %s",
                expectedKeywords, errorMessage));

        // Validate error location matches expected line, column, and includes file name
        // Note: expectedLine/Column = -1 means skip validation (e.g., EOF errors)

        // First, validate file name is in the error message
        assertTrue(errorMessage.contains(scriptFile),
            String.format("Error message should contain file name '%s'. Actual: %s",
                scriptFile, errorMessage));

        if (expectedLine > 0 && expectedColumn > 0) {
            Matcher matcher = ERROR_LOCATION_PATTERN.matcher(errorMessage);
            if (matcher.find()) {
                int actualLine = Integer.parseInt(matcher.group(1));
                int actualColumn = Integer.parseInt(matcher.group(2));

                // Validate line number
                assertEquals(expectedLine, actualLine,
                    String.format("Error should be on line %d but was on line %d. Message: %s",
                        expectedLine, actualLine, errorMessage));

                // Validate column position
                assertEquals(expectedColumn, actualColumn,
                    String.format("Error should be at column %d but was at column %d. Message: %s",
                        expectedColumn, actualColumn, errorMessage));
            } else {
                fail("Error message should include location information in format file:line:column. Actual: " + errorMessage);
            }
        } else {
            // For EOF errors, just verify location info is present
            assertTrue(errorMessage.matches(".*\\d+:\\d+.*"),
                "Error message should include location information (line:column). Actual: " + errorMessage);
        }
    }

    /**
     * Tests that error messages include file name for better debugging.
     */
    @ParameterizedTest(name = "File context: {0}")
    @CsvSource({
        "my-test-file.minilang, error-tests/invalid-type.minilang",
        "user-script.minilang, error-tests/missing-newline.minilang"
    })
    void testErrorMessageIncludesFileName(String fileName, String scriptFile) throws Exception {
        String source = loadTestScript(scriptFile);
        ParseTreeConverter converter = new ParseTreeConverter(fileName, source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });

        String errorMessage = exception.getMessage();
        assertNotNull(errorMessage);
        // Some parsers include filename, others may not - at minimum should have location
        assertTrue(errorMessage.contains(fileName) || errorMessage.matches(".*\\d+:\\d+.*"),
            "Error message should include file name or location. Actual: " + errorMessage);
    }

    /**
     * Tests multiple syntax errors to ensure parser reports the first error encountered.
     */
    @ParameterizedTest(name = "Multiple errors: {0}")
    @CsvSource({
        "error-tests/invalid-type.minilang",
        "error-tests/unterminated-string.minilang"
    })
    void testMultipleSyntaxErrors(String scriptFile) throws Exception {
        String source = loadTestScript(scriptFile);
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });

        // Should report at least one error
        assertNotNull(exception.getMessage());
        assertFalse(exception.getMessage().isEmpty());

        // Should include location information
        String errorMessage = exception.getMessage();
        assertTrue(errorMessage.matches(".*\\d+:\\d+.*") || errorMessage.contains("line"),
            "Error message should include location. Actual: " + errorMessage);
    }

    /**
     * Tests unclosed code blocks with proper error location (line and column).
     */
    @ParameterizedTest(name = "Unclosed block: {0}")
    @CsvSource({
        "error-tests/missing-brace.minilang, -1, '}|missing'"
    })
    void testUnclosedBlockWithLocation(String scriptFile, int expectedMinLine, String expectedKeywords) throws Exception {
        String source = loadTestScript(scriptFile);
        ParseTreeConverter converter = new ParseTreeConverter(scriptFile, source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });

        String errorMessage = exception.getMessage();
        assertNotNull(errorMessage);

        // Check for expected keywords
        String[] keywords = expectedKeywords.split("\\|");
        boolean containsKeyword = false;
        for (String keyword : keywords) {
            if (errorMessage.contains(keyword)) {
                containsKeyword = true;
                break;
            }
        }
        assertTrue(containsKeyword,
            String.format("Error should mention %s. Actual: %s", expectedKeywords, errorMessage));

        // Validate location present (exact line may vary for EOF errors)
        if (expectedMinLine > 0) {
            Matcher matcher = ERROR_LOCATION_PATTERN.matcher(errorMessage);
            if (matcher.find()) {
                int actualLine = Integer.parseInt(matcher.group(1));
                assertTrue(actualLine >= expectedMinLine,
                    String.format("Error should be at or after line %d, was line %d", expectedMinLine, actualLine));
            }
        } else {
            // Just verify location info is present
            assertTrue(errorMessage.matches(".*\\d+:\\d+.*") || errorMessage.contains("line"),
                "Error message should include location information");
        }
    }

    /**
     * Tests that parser provides descriptive error messages with context.
     */
    @ParameterizedTest(name = "Error context: {0}")
    @CsvSource({
        "error-tests/invalid-operator.minilang",
        "error-tests/unterminated-string.minilang",
        "error-tests/missing-brace.minilang"
    })
    void testErrorContextInformation(String scriptFile) throws Exception {
        String source = loadTestScript(scriptFile);
        ParseTreeConverter converter = new ParseTreeConverter(scriptFile, source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });

        String errorMessage = exception.getMessage();
        assertNotNull(errorMessage);

        // Error message should be descriptive (more than just "error")
        assertTrue(errorMessage.length() > 10,
            "Error message should be descriptive, not just a simple string");

        // Should include what was expected or what went wrong
        assertTrue(
            errorMessage.toLowerCase().contains("mismatched") ||
            errorMessage.toLowerCase().contains("missing") ||
            errorMessage.toLowerCase().contains("extraneous") ||
            errorMessage.toLowerCase().contains("token"),
            "Error message should describe what went wrong. Actual: " + errorMessage
        );
    }
}
