package com.elminster.minilang;

import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.compile.JcpCompiler;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.RootEvalContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MiniLang that validate dual-mode execution.
 *
 * <p>These tests serve two purposes:
 * <ol>
 *   <li><strong>Validate MiniLang:</strong> Ensure the grammar, converter, and examples work correctly</li>
 *   <li><strong>Validate JCP Core:</strong> Ensure eval and compile modes produce identical results</li>
 * </ol>
 *
 * <p>This test pattern is critical for JCP's dual-mode execution guarantee. Any divergence
 * between eval and compile modes indicates a bug in JCP core that must be fixed.
 */
public class MiniLangIntegrationTest {

    /**
     * Tests that all example programs execute correctly in both eval and compile modes
     * and produce identical output.
     *
     * <p>This is the primary validation that JCP's dual-mode execution works correctly.
     * If this test fails, it means either:
     * <ul>
     *   <li>The MiniLang converter has a bug (check parse tree conversion)</li>
     *   <li>JCP's evaluator has a bug (check eval/ package)</li>
     *   <li>JCP's compiler has a bug (check compile/ package)</li>
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

        // Execute in eval mode
        ByteArrayOutputStream evalOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(evalOutput, true, StandardCharsets.UTF_8));
            EvalContext evalCtx = new RootEvalContext();
            new EvalVisitor(evalCtx).visit(program);
        } finally {
            System.setOut(originalOut);
        }

        // Execute in compile mode
        ByteArrayOutputStream compileOutput = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(compileOutput, true, StandardCharsets.UTF_8));
            JcpCompiler compiler = new JcpCompiler();
            String className = "Test_" + exampleFile.replace(".minilang", "").replace("-", "_");
            Class<?> clazz = compiler.compileAndLoad(program, className);
            clazz.getMethod("main", String[].class).invoke(null, (Object) new String[]{});
        } finally {
            System.setOut(originalOut);
        }

        // Assert parity - this is the critical JCP core validation
        String evalResult = evalOutput.toString(StandardCharsets.UTF_8);
        String compileResult = compileOutput.toString(StandardCharsets.UTF_8);

        assertEquals(
            evalResult,
            compileResult,
            String.format(
                "Eval and compile modes must produce identical output for %s\n" +
                "Eval output:\n%s\n" +
                "Compile output:\n%s",
                exampleFile, evalResult, compileResult
            )
        );

        // Additional validation: output should not be empty for these examples
        assertFalse(evalResult.trim().isEmpty(),
            "Example " + exampleFile + " should produce output");
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

    /**
     * Loads an example file from resources.
     */
    private String loadExample(String filename) throws Exception {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("examples/" + filename)) {
            if (is == null) {
                fail("Example file not found: " + filename);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
