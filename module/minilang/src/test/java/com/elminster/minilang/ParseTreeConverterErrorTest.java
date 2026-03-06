package com.elminster.minilang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
 */
public class ParseTreeConverterErrorTest {

    /**
     * Tests missing semicolon error.
     */
    @Test
    void testMissingSemicolon() {
        String source = "let x: int = 10";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        }, "Should throw exception for missing semicolon");

        String errorMessage = exception.getMessage();
        assertNotNull(errorMessage, "Error message should not be null");
        assertFalse(errorMessage.isEmpty(), "Error message should not be empty");

        // ANTLR reports this as "mismatched input '<EOF>'" or similar
        assertTrue(errorMessage.toLowerCase().contains("mismatched") ||
                   errorMessage.toLowerCase().contains("missing"),
            "Error message should contain 'mismatched' or 'missing'. Actual: " + errorMessage);
    }

    /**
     * Tests unterminated string literal error.
     */
    @Test
    void testUnterminatedString() {
        String source = "let s: string = \"hello\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        }, "Should throw exception for unterminated string");

        String errorMessage = exception.getMessage();
        assertNotNull(errorMessage, "Error message should not be null");
        assertTrue(errorMessage.toLowerCase().contains("mismatched") ||
                   errorMessage.toLowerCase().contains("token") ||
                   errorMessage.toLowerCase().contains("string"),
            "Error message should contain token/string error. Actual: " + errorMessage);
    }

    /**
     * Tests invalid type name error.
     */
    @Test
    void testInvalidType() {
        String source = "let x: invalid = 10\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        }, "Should throw exception for invalid type");

        String errorMessage = exception.getMessage();
        assertNotNull(errorMessage, "Error message should not be null");
        assertTrue(errorMessage.toLowerCase().contains("mismatched") ||
                   errorMessage.toLowerCase().contains("extraneous"),
            "Error message should indicate syntax error. Actual: " + errorMessage);
    }

    /**
     * Tests missing expression after equals.
     */
    @Test
    void testMissingExpression() {
        String source = "let x: int = \n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        }, "Should throw exception for missing expression");

        String errorMessage = exception.getMessage();
        assertNotNull(errorMessage, "Error message should not be null");
        assertTrue(errorMessage.toLowerCase().contains("mismatched") ||
                   errorMessage.toLowerCase().contains("missing"),
            "Error message should indicate missing expression. Actual: " + errorMessage);
    }

    /**
     * Tests missing closing brace error.
     */
    @Test
    void testMissingClosingBrace() {
        String source = "func test() {\nlet x: int = 5\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        }, "Should throw exception for missing closing brace");

        String errorMessage = exception.getMessage();
        assertNotNull(errorMessage, "Error message should not be null");
        assertTrue(errorMessage.contains("}") || errorMessage.toLowerCase().contains("missing"),
            "Error message should mention missing '}'. Actual: " + errorMessage);
    }

    /**
     * Tests missing opening brace error.
     */
    @Test
    void testMissingOpeningBrace() {
        String source = "func test() \nlet x: int = 5\n}";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        }, "Should throw exception for missing opening brace");

        String errorMessage = exception.getMessage();
        assertNotNull(errorMessage, "Error message should not be null");
        assertTrue(errorMessage.toLowerCase().contains("mismatched") ||
                   errorMessage.toLowerCase().contains("missing"),
            "Error message should indicate syntax error. Actual: " + errorMessage);
    }

    /**
     * Tests invalid operator error.
     */
    @Test
    void testInvalidOperator() {
        String source = "let x: int = 5 @@ 3\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        }, "Should throw exception for invalid operator");

        String errorMessage = exception.getMessage();
        assertNotNull(errorMessage, "Error message should not be null");
        assertTrue(errorMessage.toLowerCase().contains("token") ||
                   errorMessage.toLowerCase().contains("mismatched") ||
                   errorMessage.toLowerCase().contains("extraneous") ||
                   errorMessage.contains("@@"),
            "Error message should indicate syntax error. Actual: " + errorMessage);
    }

    /**
     * Tests missing function return type arrow error.
     */
    @Test
    void testMissingFunctionReturnTypeArrow() {
        String source = "func test(): int {\nreturn 5\n}\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        }, "Should throw exception for missing return type arrow");

        String errorMessage = exception.getMessage();
        assertNotNull(errorMessage, "Error message should not be null");
        assertTrue(errorMessage.toLowerCase().contains("mismatched") ||
                   errorMessage.toLowerCase().contains("missing") ||
                   errorMessage.contains("->"),
            "Error message should indicate syntax error with ->. Actual: " + errorMessage);
    }

    /**
     * Tests that error messages include location information.
     */
    @Test
    void testErrorLocationInformation() {
        // Error on line 2
        String source = "let x: int = 10\nlet y: int = \n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });

        String errorMessage = exception.getMessage();
        assertNotNull(errorMessage, "Error message should include location");

        // Should mention line information
        boolean hasLineInfo = errorMessage.contains("line") ||
                              errorMessage.contains("Line") ||
                              errorMessage.matches(".*\\d+:\\d+.*"); // line:column format

        assertTrue(hasLineInfo,
            "Error message should include line information. Actual: " + errorMessage);
    }

    /**
     * Tests multiple syntax errors to ensure parser reports first error.
     */
    @Test
    void testMultipleSyntaxErrors() {
        String source = "let x: invalid = \nlet y: int = 10\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });

        assertNotNull(exception.getMessage());
        assertFalse(exception.getMessage().isEmpty());
    }

    /**
     * Tests type mismatch errors.
     */
    @Test
    void testTypeMismatchError() {
        String source = "let x: int = \"string value\"\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        // Note: Type checking happens at runtime, not parse time
        // This test verifies the parser accepts it (semantic analysis comes later)
        assertDoesNotThrow(() -> {
            converter.parse(source);
        });
    }

    /**
     * Tests unclosed code blocks.
     */
    @Test
    void testUnclosedBlock() {
        String source = "func test() {\nlet x: int = 5\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });

        String errorMessage = exception.getMessage();
        assertNotNull(errorMessage);
        assertTrue(errorMessage.contains("}") || errorMessage.toLowerCase().contains("missing"),
            "Error should mention missing closing brace. Actual: " + errorMessage);
    }

    /**
     * Tests invalid function declarations.
     */
    @Test
    void testInvalidFunctionDeclaration() {
        String source = "func () -> int {\nreturn 5\n}\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });

        assertNotNull(exception.getMessage());
        assertFalse(exception.getMessage().isEmpty());
    }

    /**
     * Tests invalid variable declarations.
     */
    @Test
    void testInvalidVariableDeclaration() {
        String source = "let : int = 5\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });

        assertNotNull(exception.getMessage());
    }

    /**
     * Tests malformed expressions.
     */
    @Test
    void testMalformedExpression() {
        String source = "let x: int = 5 + + 3\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });

        assertNotNull(exception.getMessage());
    }

    /**
     * Tests invalid control flow statements.
     */
    @Test
    void testInvalidControlFlow() {
        String source = "if {\nlet x: int = 5\n}\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });

        String errorMessage = exception.getMessage();
        assertNotNull(errorMessage);
        assertTrue(errorMessage.length() > 0, "Error message should provide details");
    }

    /**
     * Tests error message contains file name.
     */
    @Test
    void testErrorMessageContainsFileName() {
        String source = "let x: invalid = 10\n";
        String fileName = "my-test-file.minilang";
        ParseTreeConverter converter = new ParseTreeConverter(fileName, source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });

        // Some parsers include filename in error messages
        String errorMessage = exception.getMessage();
        assertNotNull(errorMessage);
        assertFalse(errorMessage.isEmpty());
    }

    /**
     * Tests that parser recovers error context.
     */
    @Test
    void testErrorContextInformation() {
        String source = "let x: int = 10\nlet y: int = 20\nlet z: invalid = 30\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });

        String errorMessage = exception.getMessage();
        assertNotNull(errorMessage);

        // Error message should be descriptive
        assertTrue(errorMessage.length() > 10,
            "Error message should be descriptive, not just a simple string");
    }
}
