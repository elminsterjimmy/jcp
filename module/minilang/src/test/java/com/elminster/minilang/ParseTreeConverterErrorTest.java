package com.elminster.minilang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for error handling and non-happy paths in the parser.
 */
public class ParseTreeConverterErrorTest {

    @Test
    void testParseError_MissingSemicolon() {
        String source = "let x: int = 10";  // Missing newline
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        // Should fail to parse
        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });
        assertTrue(exception.getMessage() != null);
    }

    @Test
    void testParseError_InvalidSyntax() {
        String source = "func test() {\nlet x: int = \n}";  // Missing expression
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });
        assertTrue(exception.getMessage() != null);
    }

    @Test
    void testParseError_UnterminatedString() {
        String source = "let s: string = \"unterminated\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });
        assertTrue(exception.getMessage() != null);
    }

    @Test
    void testParseError_InvalidType() {
        String source = "let x: invalid = 10\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });
        assertTrue(exception.getMessage() != null);
    }

    @Test
    void testParseError_MissingReturnType() {
        String source = "func test()\nreturn 5\n}\n";  // Missing arrow and type
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });
        assertTrue(exception.getMessage() != null);
    }
}
