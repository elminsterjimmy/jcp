package com.elminster.minilang;

import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.Data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static com.elminster.minilang.TestUtils.loadTestScript;

/**
 * Comprehensive test suite for MiniLang organized by feature category.
 */
public class MiniLangTest {

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
        "basic/simple-arithmetic-test.minilang",
        "basic/simple-comparison-test.minilang",
        "basic/simple-logical-test.minilang",
        "basic/simple-boolean-test.minilang",
        "basic/simple-string-test.minilang",
        "basic/simple-double-test.minilang"
    })
    void testBasicFeatures(String scriptFile) throws Exception {
        assertTestPasses(scriptFile);
    }

    // ===== CONTROL FLOW =====

    @ParameterizedTest(name = "Control flow: {0}")
    @CsvSource({
        "control-flow/simple-loops-test.minilang",
        "control-flow/simple-nested-loops-test.minilang",
        "control-flow/simple-nested-conditions-test.minilang",
        "control-flow/simple-else-branches-test.minilang"
    })
    void testControlFlow(String scriptFile) throws Exception {
        assertTestPasses(scriptFile);
    }

    // ===== FUNCTIONS =====

    @ParameterizedTest(name = "Functions: {0}")
    @CsvSource({
        "functions/simple-recursion-test.minilang",
        "functions/simple-multiple-functions-test.minilang",
        "functions/simple-void-function-test.minilang"
    })
    void testFunctions(String scriptFile) throws Exception {
        assertTestPasses(scriptFile);
    }

    // ===== OPERATORS AND EXPRESSIONS =====

    @ParameterizedTest(name = "Operators: {0}")
    @CsvSource({
        "operators/simple-division-test.minilang",
        "operators/simple-modulo-test.minilang",
        "operators/simple-all-comparisons-test.minilang",
        "operators/simple-complex-expressions-test.minilang",
        "operators/simple-parentheses-test.minilang"
    })
    void testOperators(String scriptFile) throws Exception {
        assertTestPasses(scriptFile);
    }

    // ===== EDGE CASES =====

    @ParameterizedTest(name = "Edge cases: {0}")
    @CsvSource({
        "edge-cases/simple-edge-cases-test.minilang"
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
