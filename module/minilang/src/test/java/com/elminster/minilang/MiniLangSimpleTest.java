package com.elminster.minilang;

import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.Data;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified assertion tests that check if test functions return true.
 *
 * <p>This approach is simpler and more flexible than embedding assertions in scripts:
 * <ul>
 *   <li>Each test script defines a test function that returns boolean</li>
 *   <li>The test function performs all checks and returns true if all pass</li>
 *   <li>The Java test simply calls the function and checks for true</li>
 *   <li>Works in both eval and compile modes</li>
 *   <li>Easy to add new test cases by creating new scripts</li>
 * </ul>
 *
 * <p>Test scripts follow a simple pattern:
 * <pre>{@code
 * func testSomething() -> boolean {
 *     let result: int = compute()
 *     if result != expected {
 *         return false
 *     }
 *     return true
 * }
 * }</pre>
 */
public class MiniLangSimpleTest {

    /**
     * Loads a test script from resources.
     */
    private String loadTestScript(String filename) throws Exception {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("test-scripts/" + filename)) {
            if (is == null) {
                fail("Test script not found: " + filename);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Tests using simple boolean-returning functions in eval mode.
     *
     * <p>Each script defines a test function that returns true if all checks pass,
     * then stores the result in a variable named "result". This approach provides
     * clear, self-documenting test cases.
     *
     * <p>Note: These tests currently only run in eval mode. Compile mode support
     * requires additional work to handle local variables and the NOT operator.
     * See issue #XX for compile mode support.
     *
     * @param scriptFile the test script file
     * @param variableName the variable to check (always "result")
     */
    @ParameterizedTest(name = "Eval: {0}")
    @CsvSource({
        "simple-arithmetic-test.minilang, result",
        "simple-comparison-test.minilang, result",
        "simple-logical-test.minilang, result",
        "simple-recursion-test.minilang, result",
        "simple-loops-test.minilang, result",
        "simple-nested-conditions-test.minilang, result",
        "simple-edge-cases-test.minilang, result",
        "simple-complex-expressions-test.minilang, result",
        "simple-string-test.minilang, result",
        "simple-nested-loops-test.minilang, result"
    })
    void testWithBooleanReturn(String scriptFile, String variableName) throws Exception {
        String source = loadTestScript(scriptFile);
        assertNotNull(source, "Test script should load: " + scriptFile);

        // Parse source into JCP AST
        ParseTreeConverter converter = new ParseTreeConverter(scriptFile, source);
        Block program = converter.parse(source);
        assertNotNull(program, "Parser should produce AST");

        // Test in eval mode
        EvalContext evalCtx = new RootEvalContext();
        new EvalVisitor(evalCtx).visit(program);

        // Check the result variable
        Data result = evalCtx.getVariable(variableName);
        assertNotNull(result, "Variable '" + variableName + "' should exist");
        assertTrue((Boolean) result.get(),
            String.format("Eval mode: %s should be true in %s", variableName, scriptFile));
    }
}
