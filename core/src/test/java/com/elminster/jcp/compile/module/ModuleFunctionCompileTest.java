package com.elminster.jcp.compile.module;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.compile.BytecodeGenerator;
import com.elminster.jcp.compile.MultiClassLoader;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for module function compilation (e.g., Assertions.assertTrue).
 */
public class ModuleFunctionCompileTest extends AbstractCompileTest {

    @Test
    void testAssertionsTrueSucceeds() throws Exception {
        // Assertions.assertTrue(true)
        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
                new FunctionCallExpression(
                        Identifier.fromName("Assertions.assertTrue"),
                        LiteralExpression.of(BooleanLiteral.of(true))
                )
        ));

        Class<?> clazz = compiler.compileAndLoad(program, uniqueClassName("TestAssertTrue"));
        Method main = clazz.getMethod("main", String[].class);

        // Should not throw exception
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testAssertionsFalseFails() throws Exception {
        // Assertions.assertTrue(false)
        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
                new FunctionCallExpression(
                        Identifier.fromName("Assertions.assertTrue"),
                        LiteralExpression.of(BooleanLiteral.of(false))
                )
        ));

        Class<?> clazz = compiler.compileAndLoad(program, uniqueClassName("TestAssertFalse"));
        Method main = clazz.getMethod("main", String[].class);

        // Should throw AssertException
        assertThrows(java.lang.reflect.InvocationTargetException.class, () ->
                main.invoke(null, (Object) new String[]{})
        );
    }

    @Test
    void testExplicitBaseModuleSyntax() throws Exception {
        // base::Assertions.assertTrue(true) - explicit module syntax
        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
                new FunctionCallExpression(
                        Identifier.fromName("base::Assertions.assertTrue"),
                        LiteralExpression.of(BooleanLiteral.of(true))
                )
        ));

        Class<?> clazz = compiler.compileAndLoad(program, uniqueClassName("TestExplicitModule"));
        Method main = clazz.getMethod("main", String[].class);

        // Should not throw exception
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testInvalidModuleFunctionNotFound() {
        // NonExistent.method() - should fall through to user-defined function lookup
        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
                new FunctionCallExpression(
                        Identifier.fromName("NonExistent.method"),
                        LiteralExpression.of(BooleanLiteral.of(true))
                )
        ));

        // Should throw CompileException for undefined function
        assertThrows(Exception.class, () ->
                compiler.compileAndLoad(program, uniqueClassName("TestNonExistent"))
        );
    }

    // -------------------------------------------------------------------------
    // D4: boxing for Object-typed parameters
    // -------------------------------------------------------------------------

    /**
     * D4 fix — when a module method has an Object parameter, FunCallCompiler must
     * box the primitive before the INVOKESTATIC so the JVM verifier is satisfied.
     *
     * <p>ObjectParamFixture.wrap(Object) returns obj.toString(). Calling it with an
     * int literal (42) requires Integer.valueOf(42) before the call.
     */
    @Test
    void d4_boxPrimitive_intToObjectParam_compilesAndReturnsCorrectValue() throws Exception {
        String genName = uniqueClassName("D4BoxTest");
        BytecodeGenerator generator = new BytecodeGenerator(genName);
        generator.registerExternalClass(ObjectParamFixture.class);

        Block empty = new BlockImpl();
        com.elminster.jcp.ast.expression.StaticMethodCallExpression call =
                new com.elminster.jcp.ast.expression.StaticMethodCallExpression(
                        "ObjectParamFixture", "wrap",
                        LiteralExpression.of(IntLiteral.of(42)));
        byte[] bytecode = generator.compileWithReturn(empty, call, SystemDataType.STRING);

        MultiClassLoader loader = new MultiClassLoader();
        loader.defineClass(genName, bytecode);
        Class<?> clazz = loader.loadClass(genName);

        Object result = clazz.getMethod("evaluate").invoke(null);
        assertEquals("42", result,
                "D4: int arg must be boxed to Integer before Object param call");
    }

    // -------------------------------------------------------------------------
    // D1: context-registered external class reachable via Type.method shorthand
    // -------------------------------------------------------------------------

    /**
     * D1 fix — resolveModuleClass now falls back to the CompileContext after the
     * base-module package lookup fails, so user-registered external classes are
     * reachable with the {@code Type.method} shorthand (no base:: prefix required).
     *
     * <p>StringUtils is not in the base module package, so the old code threw
     * ClassNotFoundException. The new code looks it up in the context and succeeds.
     */
    @Test
    void d1_contextRegisteredExternalClass_reachableViaTypeMethodShorthand() throws Exception {
        String genName = uniqueClassName("D1ContextTest");
        BytecodeGenerator generator = new BytecodeGenerator(genName);
        generator.registerExternalClass(StringUtils.class);

        Block empty = new BlockImpl();
        com.elminster.jcp.ast.expression.StaticMethodCallExpression call =
                new com.elminster.jcp.ast.expression.StaticMethodCallExpression(
                        "StringUtils", "capitalize",
                        LiteralExpression.of("hello"));
        byte[] bytecode = generator.compileWithReturn(empty, call, SystemDataType.STRING);

        MultiClassLoader loader = new MultiClassLoader();
        loader.defineClass(genName, bytecode);
        Class<?> clazz = loader.loadClass(genName);

        Object result = clazz.getMethod("evaluate").invoke(null);
        assertEquals("Hello", result,
                "D1: context-registered StringUtils must be reachable via shorthand");
    }

    // -------------------------------------------------------------------------
    // Minimal fixture
    // -------------------------------------------------------------------------

    /** Fixture with an Object-parameter method for D4 boxing test. */
    public static final class ObjectParamFixture {
        private ObjectParamFixture() {}

        public static String wrap(Object obj) {
            return obj == null ? "null" : obj.toString();
        }
    }
}
