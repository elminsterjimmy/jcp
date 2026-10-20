package com.elminster.jcp.module;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.compile.BytecodeGenerator;
import com.elminster.jcp.compile.MultiClassLoader;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.ExternalClassType;
import com.elminster.jcp.eval.excpetion.EvaluationException;
import com.elminster.jcp.util.DataTypeUtils;
import com.google.common.math.IntMath;
import com.google.common.primitives.Ints;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests exercising {@link JcpModuleRegistry} with real 3rd-party libraries
 * (Apache Commons Lang, Google Guava) in both eval and compile modes.
 */
class ThirdPartyLibRegistrationTest {

    // -------------------------------------------------------------------------
    // Compile-mode helper — uses BytecodeGenerator.registerExternalClass()
    // which is what JcpModuleRegistry.DEFAULT feeds into at compile time.
    // -------------------------------------------------------------------------

    private Object compileCallAndInvoke(
            Class<?> externalClass,
            String typeName, String methodName,
            DataType returnType,
            com.elminster.jcp.ast.Expression... args) throws Exception {

        String className = "ThirdParty_" + typeName.replace('.', '_') + "_" + methodName + "_" + System.nanoTime();
        BytecodeGenerator generator = new BytecodeGenerator(className);
        generator.registerExternalClass(externalClass);

        Block empty = new BlockImpl();
        StaticMethodCallExpression call = new StaticMethodCallExpression(typeName, methodName, args);
        byte[] bytecode = generator.compileWithReturn(empty, call, returnType);

        MultiClassLoader loader = new MultiClassLoader();
        loader.defineClass(className, bytecode);
        Class<?> clazz = loader.loadClass(className);
        return clazz.getMethod("evaluate").invoke(null);
    }

    // -------------------------------------------------------------------------
    // Eval-mode helper
    // -------------------------------------------------------------------------

    private void evalCall(
            JcpModuleRegistry registry,
            String typeName, String methodName,
            com.elminster.jcp.ast.Expression... args) {

        EvalContext ctx = new RootEvalContext();
        registry.initEval(ctx);

        Block program = new BlockImpl();
        StaticMethodCallExpression call = new StaticMethodCallExpression(typeName, methodName, args);
        program.addStatement(new ExpressionStatement(call));

        new EvalVisitor(ctx).visit(program);
    }

    // =========================================================================
    // Registration — JcpModuleRegistry populates contexts correctly
    // =========================================================================

    @Nested
    class RegistrationTests {

        @Test
        void registerStringUtils_initEval_typeAvailable() {
            JcpModuleRegistry registry = new JcpModuleRegistry();
            registry.registerClass(StringUtils.class, "commons");

            EvalContext ctx = new RootEvalContext();
            registry.initEval(ctx);

            DataType dt = DataTypeUtils.getDataType("StringUtils", ctx);
            assertNotNull(dt);
            assertInstanceOf(ExternalClassType.class, dt);
        }

        @Test
        void registerStringUtils_initCompile_typeAvailable() {
            JcpModuleRegistry registry = new JcpModuleRegistry();
            registry.registerClass(StringUtils.class, "commons");

            CompileContext ctx = new CompileContext();
            registry.initCompile(ctx);

            DataType dt = ctx.getDataType("StringUtils");
            assertNotNull(dt);
            assertInstanceOf(ExternalClassType.class, dt);
        }

        @Test
        void registerIntMath_initEval_typeAvailable() {
            JcpModuleRegistry registry = new JcpModuleRegistry();
            registry.registerClass(IntMath.class, "guava");

            EvalContext ctx = new RootEvalContext();
            registry.initEval(ctx);

            DataType dt = DataTypeUtils.getDataType("IntMath", ctx);
            assertNotNull(dt);
            assertInstanceOf(ExternalClassType.class, dt);
        }

        @Test
        void registerIntMath_initCompile_typeAvailable() {
            JcpModuleRegistry registry = new JcpModuleRegistry();
            registry.registerClass(IntMath.class, "guava");

            CompileContext ctx = new CompileContext();
            registry.initCompile(ctx);

            DataType dt = ctx.getDataType("IntMath");
            assertNotNull(dt);
            assertInstanceOf(ExternalClassType.class, dt);
        }

        @Test
        void registerInts_initCompile_typeAvailable() {
            JcpModuleRegistry registry = new JcpModuleRegistry();
            registry.registerClass(Ints.class, "guava");

            CompileContext ctx = new CompileContext();
            registry.initCompile(ctx);

            DataType dt = ctx.getDataType("Ints");
            assertNotNull(dt);
            assertInstanceOf(ExternalClassType.class, dt);
        }

        @Test
        void multiLib_allTypesRegistered() {
            JcpModuleRegistry registry = new JcpModuleRegistry();
            registry.registerClass(StringUtils.class, "commons");
            registry.registerClass(IntMath.class, "guava");
            registry.registerClass(Ints.class, "guava");

            assertEquals(3, registry.getEntries().size());
            assertEquals(StringUtils.class, registry.getEntries().get(0).getClazz());
            assertEquals("commons", registry.getEntries().get(0).getModule());
            assertEquals(IntMath.class, registry.getEntries().get(1).getClazz());
            assertEquals("guava", registry.getEntries().get(1).getModule());
            assertEquals(Ints.class, registry.getEntries().get(2).getClazz());
        }

        @Test
        void multiLib_initEval_allTypesAvailable() {
            JcpModuleRegistry registry = new JcpModuleRegistry();
            registry.registerClass(StringUtils.class, "commons");
            registry.registerClass(IntMath.class, "guava");
            registry.registerClass(Ints.class, "guava");

            EvalContext ctx = new RootEvalContext();
            registry.initEval(ctx);

            assertNotNull(DataTypeUtils.getDataType("StringUtils", ctx));
            assertNotNull(DataTypeUtils.getDataType("IntMath", ctx));
            assertNotNull(DataTypeUtils.getDataType("Ints", ctx));
        }

        @Test
        void multiLib_initCompile_allTypesAvailable() {
            JcpModuleRegistry registry = new JcpModuleRegistry();
            registry.registerClass(StringUtils.class, "commons");
            registry.registerClass(IntMath.class, "guava");
            registry.registerClass(Ints.class, "guava");

            CompileContext ctx = new CompileContext();
            registry.initCompile(ctx);

            assertNotNull(ctx.getDataType("StringUtils"));
            assertNotNull(ctx.getDataType("IntMath"));
            assertNotNull(ctx.getDataType("Ints"));
        }

        /**
         * Guava's {@code com.google.common.base.Strings} collides with JCP's base module
         * {@code com.elminster.jcp.module.base.strings.Strings}. Using simple name "Strings"
         * is ambiguous and should throw in compile mode.
         */
        @Test
        void nameCollision_simpleName_ambiguousInCompile() {
            assertThrows(CompileException.class, () ->
                    compileCallAndInvoke(
                            com.google.common.base.Strings.class,
                            "Strings", "repeat",
                            SystemDataType.STRING,
                            LiteralExpression.of("x"),
                            LiteralExpression.of(IntLiteral.of(3))));
        }

        @Test
        void nameCollision_simpleName_ambiguousInEval() {
            JcpModuleRegistry registry = new JcpModuleRegistry();
            registry.registerClass(com.google.common.base.Strings.class, "guava");

            assertThrows(EvaluationException.class, () ->
                    evalCall(registry, "Strings", "repeat",
                            LiteralExpression.of("x"),
                            LiteralExpression.of(IntLiteral.of(3))));
        }

        /**
         * Using the FQN {@code com.google.common.base.Strings} resolves the ambiguity
         * and allows calling Guava Strings methods from compiled code.
         */
        @Test
        void nameCollision_fqn_resolvesInCompile() throws Exception {
            Object result = compileCallAndInvoke(
                    com.google.common.base.Strings.class,
                    "com.google.common.base.Strings", "repeat",
                    SystemDataType.STRING,
                    LiteralExpression.of("ab"),
                    LiteralExpression.of(IntLiteral.of(3)));
            assertEquals("ababab", result);
        }

        @Test
        void nameCollision_fqn_isNullOrEmpty_compile() throws Exception {
            Object result = compileCallAndInvoke(
                    com.google.common.base.Strings.class,
                    "com.google.common.base.Strings", "isNullOrEmpty",
                    SystemDataType.BOOLEAN,
                    LiteralExpression.of(""));
            assertEquals(true, result);
        }

        @Test
        void nameCollision_fqn_nullToEmpty_compile() throws Exception {
            Object result = compileCallAndInvoke(
                    com.google.common.base.Strings.class,
                    "com.google.common.base.Strings", "nullToEmpty",
                    SystemDataType.STRING,
                    LiteralExpression.of("hello"));
            assertEquals("hello", result);
        }

        @Test
        void nameCollision_fqn_resolvesInEval() {
            JcpModuleRegistry registry = new JcpModuleRegistry();
            registry.registerClass(com.google.common.base.Strings.class, "guava");

            assertDoesNotThrow(() -> evalCall(registry,
                    "com.google.common.base.Strings", "repeat",
                    LiteralExpression.of("x"),
                    LiteralExpression.of(IntLiteral.of(3))));
        }

        @Test
        void nameCollision_fqn_isNullOrEmpty_eval() {
            JcpModuleRegistry registry = new JcpModuleRegistry();
            registry.registerClass(com.google.common.base.Strings.class, "guava");

            assertDoesNotThrow(() -> evalCall(registry,
                    "com.google.common.base.Strings", "isNullOrEmpty",
                    LiteralExpression.of("")));
        }
    }

    // =========================================================================
    // Apache Commons Lang — StringUtils (compile-mode end-to-end)
    // =========================================================================

    @Nested
    class CommonsLangCompile {

        @Test
        void capitalize_stringToString() throws Exception {
            Object result = compileCallAndInvoke(
                    StringUtils.class,
                    "StringUtils", "capitalize",
                    SystemDataType.STRING,
                    LiteralExpression.of("hello world"));
            assertEquals("Hello world", result);
        }

        @Test
        void reverse_stringToString() throws Exception {
            Object result = compileCallAndInvoke(
                    StringUtils.class,
                    "StringUtils", "reverse",
                    SystemDataType.STRING,
                    LiteralExpression.of("abcde"));
            assertEquals("edcba", result);
        }

        @Test
        void swapCase_stringToString() throws Exception {
            Object result = compileCallAndInvoke(
                    StringUtils.class,
                    "StringUtils", "swapCase",
                    SystemDataType.STRING,
                    LiteralExpression.of("Hello World"));
            assertEquals("hELLO wORLD", result);
        }

        @Test
        void trim_stringToString() throws Exception {
            Object result = compileCallAndInvoke(
                    StringUtils.class,
                    "StringUtils", "trim",
                    SystemDataType.STRING,
                    LiteralExpression.of("  hello  "));
            assertEquals("hello", result);
        }

        @Test
        void defaultString_stringToString() throws Exception {
            Object result = compileCallAndInvoke(
                    StringUtils.class,
                    "StringUtils", "defaultString",
                    SystemDataType.STRING,
                    LiteralExpression.of("test"));
            assertEquals("test", result);
        }

        @Test
        void abbreviate_stringIntToString() throws Exception {
            Object result = compileCallAndInvoke(
                    StringUtils.class,
                    "StringUtils", "abbreviate",
                    SystemDataType.STRING,
                    LiteralExpression.of("Hello World, this is a long string"),
                    LiteralExpression.of(IntLiteral.of(10)));
            assertEquals("Hello W...", result);
        }
    }

    // =========================================================================
    // Apache Commons Lang — StringUtils (eval-mode end-to-end)
    // =========================================================================

    @Nested
    class CommonsLangEval {

        private JcpModuleRegistry registryWithStringUtils() {
            JcpModuleRegistry registry = new JcpModuleRegistry();
            registry.registerClass(StringUtils.class, "commons");
            return registry;
        }

        @Test
        void capitalize_stringToString() {
            assertDoesNotThrow(() -> evalCall(
                    registryWithStringUtils(),
                    "StringUtils", "capitalize",
                    LiteralExpression.of("hello")));
        }

        @Test
        void reverse_stringToString() {
            assertDoesNotThrow(() -> evalCall(
                    registryWithStringUtils(),
                    "StringUtils", "reverse",
                    LiteralExpression.of("abc")));
        }

        @Test
        void trim_stringToString() {
            assertDoesNotThrow(() -> evalCall(
                    registryWithStringUtils(),
                    "StringUtils", "trim",
                    LiteralExpression.of("  hello  ")));
        }
    }

    // =========================================================================
    // Google Guava — IntMath (compile-mode end-to-end)
    // =========================================================================

    @Nested
    class GuavaIntMathCompile {

        @Test
        void gcd_intIntToInt() throws Exception {
            Object result = compileCallAndInvoke(
                    IntMath.class,
                    "IntMath", "gcd",
                    SystemDataType.INT,
                    LiteralExpression.of(IntLiteral.of(12)),
                    LiteralExpression.of(IntLiteral.of(8)));
            assertEquals(4, result);
        }

        @Test
        void factorial_intToInt() throws Exception {
            Object result = compileCallAndInvoke(
                    IntMath.class,
                    "IntMath", "factorial",
                    SystemDataType.INT,
                    LiteralExpression.of(IntLiteral.of(5)));
            assertEquals(120, result);
        }

        @Test
        void checkedAdd_intIntToInt() throws Exception {
            Object result = compileCallAndInvoke(
                    IntMath.class,
                    "IntMath", "checkedAdd",
                    SystemDataType.INT,
                    LiteralExpression.of(IntLiteral.of(100)),
                    LiteralExpression.of(IntLiteral.of(200)));
            assertEquals(300, result);
        }

        @Test
        void isPrime_prime_returnsTrue() throws Exception {
            Object result = compileCallAndInvoke(
                    IntMath.class,
                    "IntMath", "isPrime",
                    SystemDataType.BOOLEAN,
                    LiteralExpression.of(IntLiteral.of(7)));
            assertEquals(true, result);
        }

        @Test
        void isPrime_composite_returnsFalse() throws Exception {
            Object result = compileCallAndInvoke(
                    IntMath.class,
                    "IntMath", "isPrime",
                    SystemDataType.BOOLEAN,
                    LiteralExpression.of(IntLiteral.of(6)));
            assertEquals(false, result);
        }

        @Test
        void mod_intIntToInt() throws Exception {
            Object result = compileCallAndInvoke(
                    IntMath.class,
                    "IntMath", "mod",
                    SystemDataType.INT,
                    LiteralExpression.of(IntLiteral.of(17)),
                    LiteralExpression.of(IntLiteral.of(5)));
            assertEquals(2, result);
        }

        @Test
        void mean_intIntToInt() throws Exception {
            Object result = compileCallAndInvoke(
                    IntMath.class,
                    "IntMath", "mean",
                    SystemDataType.INT,
                    LiteralExpression.of(IntLiteral.of(10)),
                    LiteralExpression.of(IntLiteral.of(20)));
            assertEquals(15, result);
        }
    }

    // =========================================================================
    // Google Guava — IntMath (eval-mode end-to-end)
    // =========================================================================

    @Nested
    class GuavaIntMathEval {

        private JcpModuleRegistry registryWithIntMath() {
            JcpModuleRegistry registry = new JcpModuleRegistry();
            registry.registerClass(IntMath.class, "guava");
            return registry;
        }

        @Test
        void gcd_intIntToInt() {
            assertDoesNotThrow(() -> evalCall(
                    registryWithIntMath(),
                    "IntMath", "gcd",
                    LiteralExpression.of(IntLiteral.of(12)),
                    LiteralExpression.of(IntLiteral.of(8))));
        }

        @Test
        void isPrime_intToBoolean() {
            assertDoesNotThrow(() -> evalCall(
                    registryWithIntMath(),
                    "IntMath", "isPrime",
                    LiteralExpression.of(IntLiteral.of(7))));
        }

        @Test
        void factorial_intToInt() {
            assertDoesNotThrow(() -> evalCall(
                    registryWithIntMath(),
                    "IntMath", "factorial",
                    LiteralExpression.of(IntLiteral.of(5))));
        }
    }

    // =========================================================================
    // Google Guava — Ints (compile-mode end-to-end)
    // =========================================================================

    @Nested
    class GuavaIntsCompile {

        @Test
        void compare_intIntToInt() throws Exception {
            Object result = compileCallAndInvoke(
                    Ints.class,
                    "Ints", "compare",
                    SystemDataType.INT,
                    LiteralExpression.of(IntLiteral.of(5)),
                    LiteralExpression.of(IntLiteral.of(3)));
            assertEquals(1, ((Integer) result).compareTo(0) > 0 ? 1 : -1);
            assertTrue((int) result > 0);
        }

        @Test
        void compare_equal_returnsZero() throws Exception {
            Object result = compileCallAndInvoke(
                    Ints.class,
                    "Ints", "compare",
                    SystemDataType.INT,
                    LiteralExpression.of(IntLiteral.of(7)),
                    LiteralExpression.of(IntLiteral.of(7)));
            assertEquals(0, result);
        }

        @Test
        void constrainToRange_withinRange_returnsValue() throws Exception {
            Object result = compileCallAndInvoke(
                    Ints.class,
                    "Ints", "constrainToRange",
                    SystemDataType.INT,
                    LiteralExpression.of(IntLiteral.of(5)),
                    LiteralExpression.of(IntLiteral.of(1)),
                    LiteralExpression.of(IntLiteral.of(10)));
            assertEquals(5, result);
        }

        @Test
        void constrainToRange_belowMin_returnsMin() throws Exception {
            Object result = compileCallAndInvoke(
                    Ints.class,
                    "Ints", "constrainToRange",
                    SystemDataType.INT,
                    LiteralExpression.of(IntLiteral.of(-5)),
                    LiteralExpression.of(IntLiteral.of(0)),
                    LiteralExpression.of(IntLiteral.of(100)));
            assertEquals(0, result);
        }

        @Test
        void constrainToRange_aboveMax_returnsMax() throws Exception {
            Object result = compileCallAndInvoke(
                    Ints.class,
                    "Ints", "constrainToRange",
                    SystemDataType.INT,
                    LiteralExpression.of(IntLiteral.of(200)),
                    LiteralExpression.of(IntLiteral.of(0)),
                    LiteralExpression.of(IntLiteral.of(100)));
            assertEquals(100, result);
        }

        @Test
        void hashCode_intToInt() throws Exception {
            Object result = compileCallAndInvoke(
                    Ints.class,
                    "Ints", "hashCode",
                    SystemDataType.INT,
                    LiteralExpression.of(IntLiteral.of(42)));
            assertEquals(Integer.hashCode(42), result);
        }
    }

    // =========================================================================
    // Google Guava — Ints (eval-mode end-to-end)
    // =========================================================================

    @Nested
    class GuavaIntsEval {

        private JcpModuleRegistry registryWithInts() {
            JcpModuleRegistry registry = new JcpModuleRegistry();
            registry.registerClass(Ints.class, "guava");
            return registry;
        }

        @Test
        void compare_intIntToInt() {
            assertDoesNotThrow(() -> evalCall(
                    registryWithInts(),
                    "Ints", "compare",
                    LiteralExpression.of(IntLiteral.of(5)),
                    LiteralExpression.of(IntLiteral.of(3))));
        }

        @Test
        void constrainToRange_intIntIntToInt() {
            assertDoesNotThrow(() -> evalCall(
                    registryWithInts(),
                    "Ints", "constrainToRange",
                    LiteralExpression.of(IntLiteral.of(5)),
                    LiteralExpression.of(IntLiteral.of(1)),
                    LiteralExpression.of(IntLiteral.of(10))));
        }
    }
}
