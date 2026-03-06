package com.elminster.minilang;

import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.Data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for MiniLang organized by feature category.
 */
public class MiniLangTest {

    private String loadTestScript(String filename) throws Exception {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("test-scripts/" + filename)) {
            if (is == null) {
                fail("Test script not found: " + filename);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void assertTestPasses(String scriptFile) throws Exception {
        String source = loadTestScript(scriptFile);
        ParseTreeConverter converter = new ParseTreeConverter(scriptFile, source);
        Block program = converter.parse(source);

        EvalContext evalCtx = new RootEvalContext();
        new EvalVisitor(evalCtx).visit(program);

        Data result = evalCtx.getVariable("result");
        assertNotNull(result, "Variable 'result' should exist");
        assertTrue((Boolean) result.get(),
            String.format("Test should pass: %s", scriptFile));
    }

    // ===== BASIC LANGUAGE FEATURES =====

    @ParameterizedTest(name = "Basic: {0}")
    @CsvSource({
        "simple-arithmetic-test.minilang",
        "simple-comparison-test.minilang",
        "simple-logical-test.minilang",
        "simple-boolean-test.minilang",
        "simple-string-test.minilang",
        "simple-double-test.minilang"
    })
    void testBasicFeatures(String scriptFile) throws Exception {
        assertTestPasses(scriptFile);
    }

    // ===== CONTROL FLOW =====

    @ParameterizedTest(name = "Control flow: {0}")
    @CsvSource({
        "simple-loops-test.minilang",
        "simple-nested-loops-test.minilang",
        "simple-nested-conditions-test.minilang",
        "simple-else-branches-test.minilang"
    })
    void testControlFlow(String scriptFile) throws Exception {
        assertTestPasses(scriptFile);
    }

    // ===== FUNCTIONS =====

    @ParameterizedTest(name = "Functions: {0}")
    @CsvSource({
        "simple-recursion-test.minilang",
        "simple-multiple-functions-test.minilang",
        "simple-void-function-test.minilang"
    })
    void testFunctions(String scriptFile) throws Exception {
        assertTestPasses(scriptFile);
    }

    // ===== OPERATORS AND EXPRESSIONS =====

    @ParameterizedTest(name = "Operators: {0}")
    @CsvSource({
        "simple-division-test.minilang",
        "simple-modulo-test.minilang",
        "simple-all-comparisons-test.minilang",
        "simple-complex-expressions-test.minilang",
        "simple-parentheses-test.minilang"
    })
    void testOperators(String scriptFile) throws Exception {
        assertTestPasses(scriptFile);
    }

    // ===== EDGE CASES =====

    @ParameterizedTest(name = "Edge cases: {0}")
    @CsvSource({
        "simple-edge-cases-test.minilang"
    })
    void testEdgeCases(String scriptFile) throws Exception {
        assertTestPasses(scriptFile);
    }

    // ===== ERROR HANDLING =====

    @Test
    void testParseError_InvalidSyntax() {
        String source = "func test() {\nlet x: int = \n}";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });
        assertNotNull(exception.getMessage(), "Should have error message");
        assertTrue(exception.getMessage().length() > 0, "Error message should not be empty");
    }

    @Test
    void testParseError_UnterminatedString() {
        String source = "let s: string = \"unterminated\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });
        assertNotNull(exception.getMessage());
    }

    @Test
    void testParseError_InvalidType() {
        String source = "let x: invalid = 10\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);

        Exception exception = assertThrows(Exception.class, () -> {
            converter.parse(source);
        });
        assertNotNull(exception.getMessage());
    }
}
