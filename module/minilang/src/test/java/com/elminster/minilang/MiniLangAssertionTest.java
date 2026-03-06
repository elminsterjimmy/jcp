package com.elminster.minilang;

import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.compile.JcpCompiler;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static com.elminster.minilang.TestUtils.loadTestScript;

/**
 * Assertion-based tests for MiniLang using separate test script files.
 *
 * <p>These tests validate correctness by executing scripts that contain Assertions.assertTrue()
 * calls. Each script verifies its own behavior through assertions. If any assertion fails,
 * the test fails, providing clear feedback about what went wrong.
 *
 * <p>Test scripts are stored in src/test/resources/test-scripts/ for easy review and modification.
 * This approach makes it easy to:
 * <ul>
 *   <li>Review test logic without reading Java code</li>
 *   <li>Add new test cases by creating new .minilang files</li>
 *   <li>Verify behavior in both eval and compile modes</li>
 *   <li>Use MiniLang itself to test MiniLang (dogfooding)</li>
 * </ul>
 */
public class MiniLangAssertionTest {

    /**
     * Tests using assertions in eval mode only.
     *
     * <p>Note: Assertions.assertTrue() currently works in eval mode but not in compile mode
     * due to JCP's module function architecture. These tests validate eval mode correctness
     * using self-verifying scripts.
     *
     * <p>The script computes results and verifies them with assertions.
     * If any assertion fails, an AssertException is thrown, causing the test to fail.
     */
    @ParameterizedTest(name = "Eval mode: {0}")
    @ValueSource(strings = {
        "arithmetic-assertions.minilang",
        "comparison-assertions.minilang",
        "logical-assertions.minilang",
        "function-call-assertions.minilang",
        "recursion-assertions.minilang",
        "if-else-assertions.minilang",
        "while-loop-assertions.minilang",
        "break-assertions.minilang",
        "continue-assertions.minilang"
    })
    void testEvalModeWithAssertions(String scriptFile) throws Exception {
        String source = loadTestScript(scriptFile);
        assertNotNull(source, "Test script should load: " + scriptFile);

        // Parse source into JCP AST
        ParseTreeConverter converter = new ParseTreeConverter(scriptFile, source);
        Block program = converter.parse(source);
        assertNotNull(program, "Parser should produce AST");

        // Execute in eval mode - assertions will throw if they fail
        assertDoesNotThrow(() -> {
            EvalContext evalCtx = new RootEvalContext();
            new EvalVisitor(evalCtx).visit(program);
        }, "Eval mode assertions should pass for " + scriptFile);
    }
}
