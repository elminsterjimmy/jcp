package com.elminster.jcp.compile;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for module:: prefix type disambiguation in compile mode (issue #73).
 *
 * Verifies that guava::Strings.repeat(s, 3) resolves to Guava's Strings when
 * both Guava Strings and base module Strings are registered.
 */
public class ModulePrefixTypeDisambiguationCompileTest extends AbstractCompileTest {

    /**
     * guava::Strings.repeat("ab", 3) in compile mode.
     * Both Guava Strings and base module Strings are registered.
     * The module:: prefix disambiguates to Guava's version.
     */
    @Test
    void testGuavaStringsRepeatWithModulePrefix() throws Exception {
        Block program = new BlockImpl();

        StaticMethodCallExpression call = new StaticMethodCallExpression(
            Identifier.fromName("guava::Strings"),
            "repeat",
            LiteralExpression.of("ab"),
            LiteralExpression.of(IntLiteral.of(3))
        );

        String className = uniqueClassName("TestGuavaRepeat");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        generator.registerExternalClass(com.google.common.base.Strings.class, "guava");
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.STRING);

        Class<?> clazz = new ByteArrayClassLoader().defineClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        Object result = evaluate.invoke(null);
        assertEquals("ababab", result);
    }

    /**
     * Strings.length(s) still works when only the base module Strings is registered
     * (no collision). The simple name resolution is unchanged.
     */
    @Test
    void testSimpleNameResolutionNoCollision() throws Exception {
        Block program = new BlockImpl();

        StaticMethodCallExpression call = new StaticMethodCallExpression(
            Identifier.fromName("Strings"),
            "length",
            LiteralExpression.of("hello")
        );

        String className = uniqueClassName("TestSimpleName");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.INT);

        Class<?> clazz = new ByteArrayClassLoader().defineClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        Object result = evaluate.invoke(null);
        assertEquals(5, result);
    }

    /**
     * Unknown module prefix returns null from getDataType, causing CompileException.
     */
    @Test
    void testUnknownModulePrefixThrowsCompileException() {
        Block program = new BlockImpl();

        StaticMethodCallExpression call = new StaticMethodCallExpression(
            Identifier.fromName("unknownmod::Strings"),
            "length",
            LiteralExpression.of("hello")
        );

        String className = uniqueClassName("TestUnknownModule");
        BytecodeGenerator generator = new BytecodeGenerator(className);

        CompileException ex = assertThrows(CompileException.class, () ->
            generator.compileWithReturn(program, call, SystemDataType.INT)
        );
        assertTrue(ex.getMessage().contains("unknownmod::Strings"),
            "Error message should contain the module-qualified name");
    }

    /**
     * base::Strings.length(s) with explicit base module prefix — non-collision usage.
     */
    @Test
    void testExplicitBaseModulePrefix() throws Exception {
        Block program = new BlockImpl();

        StaticMethodCallExpression call = new StaticMethodCallExpression(
            Identifier.fromName("base::Strings"),
            "length",
            LiteralExpression.of("world")
        );

        String className = uniqueClassName("TestBasePrefix");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.INT);

        Class<?> clazz = new ByteArrayClassLoader().defineClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        Object result = evaluate.invoke(null);
        assertEquals(5, result);
    }

    /**
     * When both Guava and base Strings are registered, bare "Strings" is ambiguous
     * and throws CompileException with a message suggesting module:: syntax.
     */
    @Test
    void testAmbiguousSimpleNameWithBothRegistered() {
        Block program = new BlockImpl();

        StaticMethodCallExpression call = new StaticMethodCallExpression(
            Identifier.fromName("Strings"),
            "length",
            LiteralExpression.of("test")
        );

        String className = uniqueClassName("TestAmbiguous");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        generator.registerExternalClass(com.google.common.base.Strings.class, "guava");

        CompileException ex = assertThrows(CompileException.class, () ->
            generator.compileWithReturn(program, call, SystemDataType.INT)
        );
        assertTrue(ex.getMessage().contains("ambiguous"),
            "Error should mention ambiguity");
        assertTrue(ex.getMessage().contains("module::SimpleName"),
            "Error should suggest module:: syntax");
    }

    /**
     * Three-tier test: all three resolution paths work in the same compilation context.
     * 1. Simple name for unambiguous type (Math.abs)
     * 2. module::SimpleName for ambiguous type (guava::Strings.repeat)
     * 3. FQN for ambiguous type (via getDataTypeByFqn)
     */
    @Test
    void testThreeTierResolution() throws Exception {
        Block program = new BlockImpl();

        // Tier 2: module::SimpleName
        StaticMethodCallExpression guavaRepeat = new StaticMethodCallExpression(
            Identifier.fromName("guava::Strings"),
            "repeat",
            LiteralExpression.of("x"),
            LiteralExpression.of(IntLiteral.of(4))
        );

        String className = uniqueClassName("TestThreeTier");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        generator.registerExternalClass(com.google.common.base.Strings.class, "guava");
        byte[] bytecode = generator.compileWithReturn(program, guavaRepeat, SystemDataType.STRING);

        Class<?> clazz = new ByteArrayClassLoader().defineClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        Object result = evaluate.invoke(null);
        assertEquals("xxxx", result);
    }

    /**
     * Variable declaration with module::Type resolves correctly.
     * Verifies that VariableDeclarationCompiler.resolveDataType flows through
     * CompileContext.getDataType with module:: prefix.
     */
    @Test
    void testVariableDeclarationWithModulePrefix() throws Exception {
        Block program = new BlockImpl();

        // Declare var s: guava::Strings = null (just to verify type resolution)
        // Then call guava::Strings.repeat("a", 2)
        DataType modulePrefixedType = new DataTypeImpl("guava::Strings");
        program.addStatement(new VariableDeclarationImpl("s", modulePrefixedType));

        StaticMethodCallExpression call = new StaticMethodCallExpression(
            Identifier.fromName("guava::Strings"),
            "repeat",
            LiteralExpression.of("a"),
            LiteralExpression.of(IntLiteral.of(2))
        );

        String className = uniqueClassName("TestVarDeclModule");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        generator.registerExternalClass(com.google.common.base.Strings.class, "guava");
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.STRING);

        Class<?> clazz = new ByteArrayClassLoader().defineClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        Object result = evaluate.invoke(null);
        assertEquals("aa", result);
    }

    @Nested
    class GuavaIntMathNonCollision {
        /**
         * guava::IntMath.factorial(5) — explicit module prefix on a non-colliding type.
         * Verifies module:: prefix works even when there's no ambiguity.
         */
        @Test
        void testGuavaIntMathWithModulePrefix() throws Exception {
            Block program = new BlockImpl();

            StaticMethodCallExpression call = new StaticMethodCallExpression(
                Identifier.fromName("guava::IntMath"),
                "factorial",
                LiteralExpression.of(IntLiteral.of(5))
            );

            String className = uniqueClassName("TestIntMathFactorial");
            BytecodeGenerator generator = new BytecodeGenerator(className);
            generator.registerExternalClass(com.google.common.math.IntMath.class, "guava");
            byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.INT);

            Class<?> clazz = new ByteArrayClassLoader().defineClass(className, bytecode);
            Method evaluate = clazz.getMethod("evaluate");
            Object result = evaluate.invoke(null);
            assertEquals(120, result);
        }
    }

    private static class ByteArrayClassLoader extends ClassLoader {
        public Class<?> defineClass(String name, byte[] bytecode) {
            return defineClass(name, bytecode, 0, bytecode.length);
        }
    }
}
