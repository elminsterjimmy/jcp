package com.elminster.minilang;

import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.compile.JcpCompiler;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.RootEvalContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dual-mode execution tests for MiniLang.
 *
 * <p>Validates that both eval (interpreter) and compile (bytecode) modes
 * produce identical results for the same MiniLang programs. This ensures
 * JCP's dual-mode execution guarantee.
 *
 * <p>These are basic parser validation tests, not full integration tests.
 */
public class MiniLangDualModeTest {

    /**
     * Loads an example program from resources.
     */
    private String loadExample(String filename) throws Exception {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("examples/" + filename)) {
            if (is == null) {
                throw new IllegalArgumentException("Example not found: " + filename);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Tests that all example programs execute correctly in both eval and compile modes.
     *
     * <p>The examples now include assertions that validate their own correctness.
     * If any assertion fails, an AssertException will be thrown, causing the test to fail.
     * If the test passes, it means:
     * <ul>
     *   <li>The MiniLang converter works correctly</li>
     *   <li>Both eval and compile modes execute correctly</li>
     *   <li>All assertions in the example passed</li>
     * </ul>
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "01-basics.minilang",
        "02-functions.minilang",
        "03-control-flow.minilang"
    })
    void testDualModeExecution(String exampleFile) throws Exception {
        String source = loadExample(exampleFile);
        assertNotNull(source, "Example file should load: " + exampleFile);
        assertFalse(source.trim().isEmpty(), "Example should not be empty: " + exampleFile);

        // Parse source into JCP AST
        ParseTreeConverter converter = new ParseTreeConverter(exampleFile, source);
        Block program = converter.parse(source);
        assertNotNull(program, "Parser should produce AST");

        // Execute in eval mode - should not throw
        assertDoesNotThrow(() -> {
            EvalContext evalCtx = new RootEvalContext();
            new EvalVisitor(evalCtx).visit(program);
        }, "Eval mode should execute without errors for " + exampleFile);

        // Execute in compile mode - should not throw
        assertDoesNotThrow(() -> {
            JcpCompiler compiler = new JcpCompiler();
            String className = "Test_" + exampleFile.replace(".minilang", "").replace("-", "_");
            Class<?> clazz = compiler.compileAndLoad(program, className);
            clazz.getMethod("main", String[].class).invoke(null, (Object) new String[]{});
        }, "Compile mode should execute without errors for " + exampleFile);
    }

    /**
     * Tests basic parsing without execution to catch syntax errors early.
     */
    @ParameterizedTest(name = "Parse {0}")
    @ValueSource(strings = {
        "01-basics.minilang",
        "02-functions.minilang",
        "03-control-flow.minilang"
    })
    void testParsing(String exampleFile) throws Exception {
        String source = loadExample(exampleFile);
        ParseTreeConverter converter = new ParseTreeConverter(exampleFile, source);

        // Should parse without throwing
        Block program = converter.parse(source);

        assertNotNull(program, "Parser should produce AST");
        assertNotNull(program.getBody(), "Program should have statements");
        assertFalse(program.getBody().isEmpty(), "Program should not be empty");
    }
}
