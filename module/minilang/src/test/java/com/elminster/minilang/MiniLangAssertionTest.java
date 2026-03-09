package com.elminster.minilang;

import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.compile.JcpCompiler;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

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
 *
 * <p><b>Dual-Mode Execution:</b> All assertion tests now run in both eval and compile modes,
 * ensuring JCP's dual-mode execution guarantee. This validates that the interpreter and
 * bytecode compiler produce identical results.
 */
public class MiniLangAssertionTest {

    /**
     * Execution mode for tests.
     */
    enum Mode {
        EVAL,
        COMPILE
    }

    /**
     * Provides test cases for all assertion test scripts in both eval and compile modes.
     */
    static Stream<Arguments> assertionTestCases() {
        String[] scripts = {
            "basic/arithmetic-assertions.minilang",
            "basic/comparison-assertions.minilang",
            "basic/logical-assertions.minilang",
            "functions/function-call-assertions.minilang",
            "functions/recursion-assertions.minilang",
            "control-flow/if-else-assertions.minilang",
            "control-flow/while-loop-assertions.minilang",
            "control-flow/break-assertions.minilang",
            "control-flow/continue-assertions.minilang"
        };

        return Stream.of(scripts)
            .flatMap(script -> Stream.of(
                Arguments.of(Mode.EVAL, script),
                Arguments.of(Mode.COMPILE, script)
            ));
    }

    /**
     * Tests assertion scripts in both eval and compile modes.
     *
     * <p>The script computes results and verifies them with Assertions.assertTrue() calls.
     * If any assertion fails, an AssertException is thrown, causing the test to fail.
     *
     * <p>This validates that both execution modes produce correct results and that
     * module functions (like Assertions.assertTrue) work in compile mode.
     *
     * @param mode execution mode (EVAL or COMPILE)
     * @param scriptFile path to test script
     */
    @ParameterizedTest(name = "{0} mode: {1}")
    @MethodSource("assertionTestCases")
    void testDualModeWithAssertions(Mode mode, String scriptFile) throws Exception {
        String source = loadTestScript(scriptFile);
        assertNotNull(source, "Test script should load: " + scriptFile);

        // Parse source into JCP AST
        ParseTreeConverter converter = new ParseTreeConverter(scriptFile, source);
        Block program = converter.parse(source);
        assertNotNull(program, "Parser should produce AST");

        if (mode == Mode.EVAL) {
            // Execute in eval mode - assertions will throw if they fail
            assertDoesNotThrow(() -> {
                EvalContext evalCtx = new RootEvalContext();
                new EvalVisitor(evalCtx).visit(program);
            }, "Eval mode assertions should pass for " + scriptFile);
        } else {
            // Execute in compile mode - assertions will throw if they fail
            assertDoesNotThrow(() -> {
                JcpCompiler compiler = new JcpCompiler();
                String className = "Test_" + scriptFile
                    .replace("/", "_")
                    .replace(".minilang", "")
                    .replace("-", "_");
                Class<?> clazz = compiler.compileAndLoad(program, className);
                clazz.getMethod("main", String[].class).invoke(null, (Object) new String[]{});
            }, "Compile mode assertions should pass for " + scriptFile);
        }
    }
}
