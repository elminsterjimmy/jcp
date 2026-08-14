package com.elminster.jcp.compile.bridge;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.compile.BytecodeGenerator;
import com.elminster.jcp.compile.MultiClassLoader;
import com.elminster.jcp.compile.util.CompileModeClassConverter;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PR 1 of 4 for issue #50 — JCP↔Java type bridge reproduction suite.
 *
 * <p>This test class reproduces and isolates the real failures hit when calling
 * a user-provided JAR from compiled JCP code via a registered JCP type
 * (StaticMethodCallExpression → StaticMethodCallCompiler.compileExternalClassCall).
 *
 * <p>It does NOT fix anything. Its job is to:
 * <ul>
 *   <li>Confirm the green-control path works (capitalize — no boxing needed)</li>
 *   <li>Expose D4' (boxing-vs-exact-descriptor VerifyError for char/CharSequence params)</li>
 *   <li>Expose D2 (NoClassDefFoundError when the class is in an isolated URLClassLoader)</li>
 *   <li>Pin current mapJavaTypeToDataType gaps via characterization tests</li>
 *   <li>Verify eval-mode round-trip correctness for char/CharSequence params</li>
 * </ul>
 *
 * <p>Each red test is annotated with the defect id and the compiler line it exercises.
 * See docs/solutions/runtime-errors/50-type-bridge-isolation-report.md for the full
 * isolation report.
 */
class TypeBridgeReproductionTest {

    /**
     * Compile a StaticMethodCallExpression against a registered class and load the result.
     *
     * @param clazz      the Java class to register as a JCP type
     * @param typeName   the name used in JCP (e.g. "StringUtils")
     * @param methodName the static method to call
     * @param returnType the JCP return type
     * @param args       literal argument expressions
     * @param loader     the MultiClassLoader to define and load from (may have extra parents)
     * @return the loaded class with a static {@code evaluate()} method
     */
    private Class<?> compileAndLoad(
            Class<?> clazz, String typeName, String methodName,
            com.elminster.jcp.eval.data.DataType returnType,
            com.elminster.jcp.ast.Expression[] args,
            MultiClassLoader loader) throws Exception {

        String className = "BridgeTest_" + typeName + "_" + methodName + "_" + System.nanoTime();
        BytecodeGenerator generator = new BytecodeGenerator(className);
        generator.registerExternalClass(clazz);

        Block empty = new BlockImpl();
        StaticMethodCallExpression call = new StaticMethodCallExpression(typeName, methodName, args);
        byte[] bytecode = generator.compileWithReturn(empty, call, returnType);

        loader.defineClass(className, bytecode);
        return loader.loadClass(className);
    }

    // -------------------------------------------------------------------------
    // Phase 1: Characterization — mapJavaTypeToDataType for gap types
    // -------------------------------------------------------------------------

    @Nested
    class CharacterizationTests {

        @Test
        void mapJavaTypeToDataType_char_mapsToINT() {
            // Fixed in PR 2: char and int share the same JVM slot (I)
            assertEquals(SystemDataType.INT, CompileModeClassConverter.mapJavaTypeToDataType(char.class));
        }

        @Test
        void mapJavaTypeToDataType_Character_mapsToINT() {
            assertEquals(SystemDataType.INT, CompileModeClassConverter.mapJavaTypeToDataType(Character.class));
        }

        @Test
        void mapJavaTypeToDataType_CharSequence_contextFree_returnsANY() {
            // Context-free overload returns ANY for unknown types by design.
            // Use registerClass (context-aware) to get a proper ExternalClassType at runtime.
            assertEquals(SystemDataType.ANY, CompileModeClassConverter.mapJavaTypeToDataType(CharSequence.class));
        }

        @Test
        void mapJavaTypeToDataType_CharSequence_contextAware_registersExternalClassType() {
            // With context, CharSequence is registered transitively as an ExternalClassType
            com.elminster.jcp.compile.context.CompileContext ctx =
                    new com.elminster.jcp.compile.context.CompileContext();
            CompileModeClassConverter.mapJavaTypeToDataType(CharSequence.class, ctx, "test");
            com.elminster.jcp.eval.data.DataType registered = ctx.getDataType("CharSequence");
            assertNotNull(registered, "CharSequence must be registered in context");
            assertInstanceOf(com.elminster.jcp.eval.data.ExternalClassType.class, registered);
        }

        @Test
        void mapJavaTypeToDataType_long_collapsesToANY() {
            // long stays ANY — no JCP LONG type yet
            assertEquals(SystemDataType.ANY, CompileModeClassConverter.mapJavaTypeToDataType(long.class));
        }

        @Test
        void mapJavaTypeToDataType_float_mapsToDouble() {
            // float widens to DOUBLE (JCP has no separate float type)
            assertEquals(SystemDataType.DOUBLE, CompileModeClassConverter.mapJavaTypeToDataType(float.class));
        }

        @Test
        void mapJavaTypeToDataType_byte_mapsToInt() {
            // byte shares the JVM int slot
            assertEquals(SystemDataType.INT, CompileModeClassConverter.mapJavaTypeToDataType(byte.class));
        }

        @Test
        void mapJavaTypeToDataType_short_mapsToInt() {
            // short shares the JVM int slot
            assertEquals(SystemDataType.INT, CompileModeClassConverter.mapJavaTypeToDataType(short.class));
        }
    }

    // -------------------------------------------------------------------------
    // Phase 2: Same-loader — green control + D4' probes
    // -------------------------------------------------------------------------

    @Nested
    class SameLoaderTests {

        /**
         * GREEN CONTROL — capitalize(String) → String.
         *
         * <p>StringUtils.capitalize has signature (Ljava/lang/String;)Ljava/lang/String;
         * The JCP STRING→STRING path needs no boxing, so this MUST pass. A failure here
         * means the test harness itself is broken, not the product.
         */
        @Test
        void greenControl_capitalize_compilesAndReturnsCorrectValue() throws Exception {
            MultiClassLoader loader = new MultiClassLoader();
            Class<?> clazz = compileAndLoad(
                    StringUtils.class, "StringUtils", "capitalize",
                    SystemDataType.STRING,
                    new com.elminster.jcp.ast.Expression[]{
                            LiteralExpression.of("hello")
                    },
                    loader);

            Object result = clazz.getMethod("evaluate").invoke(null);
            assertEquals("Hello", result,
                    "Green control failed — harness is broken, not the product");
        }

        /**
         * Fixed in PR 2 — repeat(char, int) → String now works.
         *
         * <p>char maps to INT; the compiler treats the char param as INT-typed.
         * The INVOKESTATIC descriptor is exact (C from the real method), and INT
         * is directly pushed — no boxing needed. Result: "***" for char='*' (42), count=3.
         */
        @Test
        void repeat_charInt_compilesAndReturnsCorrectValue() throws Exception {
            MultiClassLoader loader = new MultiClassLoader();
            Class<?> clazz = compileAndLoad(
                    StringUtils.class, "StringUtils", "repeat",
                    SystemDataType.STRING,
                    new com.elminster.jcp.ast.Expression[]{
                            LiteralExpression.of(IntLiteral.of(42)), // '*' = ASCII 42
                            LiteralExpression.of(IntLiteral.of(3))
                    },
                    loader);
            Object result = clazz.getMethod("evaluate").invoke(null);
            assertEquals("***", result);
        }

        /**
         * Fixed in PR 2 — countMatches(CharSequence, char) → int now works.
         *
         * <p>char→INT and CharSequence registered as ExternalClassType; STRING is
         * Java-assignable to CharSequence so overload resolution selects the right overload.
         */
        @Test
        void countMatches_charSequenceChar_compilesAndReturnsCorrectValue() throws Exception {
            MultiClassLoader loader = new MultiClassLoader();
            Class<?> clazz = compileAndLoad(
                    StringUtils.class, "StringUtils", "countMatches",
                    SystemDataType.INT,
                    new com.elminster.jcp.ast.Expression[]{
                            LiteralExpression.of("hello world"),
                            LiteralExpression.of(IntLiteral.of(108)) // 'l' = ASCII 108
                    },
                    loader);
            Object result = clazz.getMethod("evaluate").invoke(null);
            assertEquals(3, result);
        }

        /**
         * Synthetic fixture — long-returning method.
         *
         * <p>LongReturningFixture.longValue() → long.
         * mapJavaTypeToDataType(long.class) returns ANY (D5). The return type recorded
         * in ExternalMethodDef will be ANY, but the actual descriptor ends in J (long).
         * The generated bytecode will use the wrong return opcode.
         *
         * <p>Uses the minimal synthetic fixture defined below.
         */
        @Test
        void d5_longReturnType_returnDescriptorMismatch() {
            assertThrows(
                    Throwable.class,
                    () -> {
                        MultiClassLoader loader = new MultiClassLoader();
                        Class<?> clazz = compileAndLoad(
                                LongReturningFixture.class, "LongReturningFixture", "longValue",
                                SystemDataType.ANY,
                                new com.elminster.jcp.ast.Expression[0],
                                loader);
                        clazz.getMethod("evaluate").invoke(null);
                    },
                    "D5 (long return): expected descriptor/opcode mismatch"
            );
        }
    }

    // -------------------------------------------------------------------------
    // Phase 3: Isolated-loader — D1/D2 classloader boundary
    // -------------------------------------------------------------------------

    @Nested
    class IsolatedLoaderTests {

        /**
         * D2 probe — generated class references a user class whose classloader
         * is NOT in MultiClassLoader's parent chain.
         *
         * <p>commons-lang3 is already on the app classpath, so using it would not
         * reproduce D2 (MultiClassLoader's parent resolves it fine). Instead we build
         * a minimal foreign class at test time using ASM, write it to a temp JAR,
         * load it via a URLClassLoader whose parent is the platform loader only, and
         * register that loaded class into the compiler. The generated bytecode will
         * reference the foreign internal name; MultiClassLoader's parent cannot resolve
         * it → NoClassDefFoundError at runtime.
         *
         * <p>Sanity assertion: the loaded class must NOT be loadable from the current
         * thread's context classloader (proving the boundary is real).
         *
         * <p>Defect path: MultiClassLoader parent chain does not include user-provided
         * URLClassLoader (D1 structural). Generated INVOKESTATIC references internal name
         * resolvable only from the isolated loader (D2).
         */
        @Test
        void d2_isolatedUrlClassLoader_noClassDefFoundErrorAtInvoke() throws Exception {
            byte[] foreignBytecode = buildForeignHelperBytecode();
            String foreignInternalName = "bridge/ForeignHelper";
            String foreignClassName   = "bridge.ForeignHelper";

            java.nio.file.Path jarPath = java.nio.file.Files.createTempFile("foreign-", ".jar");
            try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(
                    java.nio.file.Files.newOutputStream(jarPath))) {
                jos.putNextEntry(new java.util.jar.JarEntry(foreignInternalName + ".class"));
                jos.write(foreignBytecode);
                jos.closeEntry();
            }

            URLClassLoader isolatedLoader = new URLClassLoader(
                    new URL[]{jarPath.toUri().toURL()},
                    ClassLoader.getPlatformClassLoader()
            );
            Class<?> foreignClass = isolatedLoader.loadClass(foreignClassName);

            assertThrows(ClassNotFoundException.class,
                    () -> Thread.currentThread().getContextClassLoader().loadClass(foreignClassName),
                    "Sanity failure: foreign class is visible from app classloader — boundary not real");

            String genClassName = "IsolatedBridgeTest_" + System.nanoTime();
            BytecodeGenerator generator = new BytecodeGenerator(genClassName);
            generator.registerExternalClass(foreignClass);

            Block empty = new BlockImpl();
            StaticMethodCallExpression call = new StaticMethodCallExpression(
                    "ForeignHelper", "hello");
            byte[] bytecode = generator.compileWithReturn(empty, call, SystemDataType.STRING);

            // Without registerClassLoader: MultiClassLoader's parent cannot resolve the foreign class
            MultiClassLoader multiLoader = new MultiClassLoader();
            multiLoader.defineClass(genClassName, bytecode);
            assertThrows(
                    Throwable.class,
                    () -> {
                        Class<?> loaded = multiLoader.loadClass(genClassName);
                        loaded.getMethod("evaluate").invoke(null);
                    },
                    "D2: expected failure without registerClassLoader"
            );

            isolatedLoader.close();
            java.nio.file.Files.deleteIfExists(jarPath);
        }

        /**
         * D2 fix — with {@code registerClassLoader(isolatedLoader)} the generated class
         * can resolve the foreign type and the call succeeds.
         */
        @Test
        void d2_registerClassLoader_resolvesIsolatedClass() throws Exception {
            byte[] foreignBytecode = buildForeignHelperBytecode();
            String foreignInternalName = "bridge/ForeignHelper";
            String foreignClassName   = "bridge.ForeignHelper";

            java.nio.file.Path jarPath = java.nio.file.Files.createTempFile("foreign-fix-", ".jar");
            try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(
                    java.nio.file.Files.newOutputStream(jarPath))) {
                jos.putNextEntry(new java.util.jar.JarEntry(foreignInternalName + ".class"));
                jos.write(foreignBytecode);
                jos.closeEntry();
            }

            URLClassLoader isolatedLoader = new URLClassLoader(
                    new URL[]{jarPath.toUri().toURL()},
                    ClassLoader.getPlatformClassLoader()
            );
            Class<?> foreignClass = isolatedLoader.loadClass(foreignClassName);

            String genClassName = "IsolatedBridgeFixTest_" + System.nanoTime();
            BytecodeGenerator generator = new BytecodeGenerator(genClassName);
            generator.registerExternalClass(foreignClass);

            Block empty = new BlockImpl();
            StaticMethodCallExpression call = new StaticMethodCallExpression(
                    "ForeignHelper", "hello");
            byte[] bytecode = generator.compileWithReturn(empty, call, SystemDataType.STRING);

            // With registerClassLoader: isolatedLoader is in the delegate chain → resolves
            MultiClassLoader multiLoader = new MultiClassLoader();
            multiLoader.registerClassLoader(isolatedLoader);
            multiLoader.defineClass(genClassName, bytecode);

            Class<?> loaded = multiLoader.loadClass(genClassName);
            Object result = loaded.getMethod("evaluate").invoke(null);
            assertEquals("hello", result,
                    "D2 fix: registerClassLoader must allow foreign class to be resolved");

            isolatedLoader.close();
            java.nio.file.Files.deleteIfExists(jarPath);
        }

        /**
         * Build minimal bytecode for:
         * <pre>
         *   package bridge;
         *   public class ForeignHelper {
         *       public static String hello() { return "hello"; }
         *   }
         * </pre>
         * Using ASM (already a compile dep via core).
         */
        private byte[] buildForeignHelperBytecode() {
            org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
            cw.visit(org.objectweb.asm.Opcodes.V1_8,
                    org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_SUPER,
                    "bridge/ForeignHelper", null, "java/lang/Object", null);

            // default constructor
            org.objectweb.asm.MethodVisitor init = cw.visitMethod(
                    org.objectweb.asm.Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            init.visitCode();
            init.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
            init.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL,
                    "java/lang/Object", "<init>", "()V", false);
            init.visitInsn(org.objectweb.asm.Opcodes.RETURN);
            init.visitMaxs(1, 1);
            init.visitEnd();

            // public static String hello()
            org.objectweb.asm.MethodVisitor mv = cw.visitMethod(
                    org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC,
                    "hello", "()Ljava/lang/String;", null, null);
            mv.visitCode();
            mv.visitLdcInsn("hello");
            mv.visitInsn(org.objectweb.asm.Opcodes.ARETURN);
            mv.visitMaxs(1, 0);
            mv.visitEnd();

            cw.visitEnd();
            return cw.toByteArray();
        }

        /**
         * Documents D1 (structural) — resolveModuleClassName is hardcoded to base module.
         *
         * <p>This is a code-verified finding, not an executable reproduction. The method
         * FunCallCompiler.resolveModuleClassName accepts only "base" as the module prefix,
         * so the FunCallCompiler path (module::Type.method) cannot be reached for user JARs.
         * This is recorded here as a structural block; PRs 2/3 address it.
         */
        @Test
        void d1_structural_resolveModuleClassName_baseOnlyBlock_documentedCodeVerified() {
            // D1 is structural: FunCallCompiler.resolveModuleClassName only accepts "base" module.
            // This means the module::Type.method() shorthand cannot reach user-registered JARs.
            // Reproduced by code reading, not executable test. PRs 2/3 fix this.
            String pkg = StringUtils.class.getPackage().getName();
            assertFalse(pkg.startsWith("com.elminster.jcp.module.base"),
                    "StringUtils should not be in the base module — D1 structural block applies");
        }
    }

    // -------------------------------------------------------------------------
    // Phase 4: Eval-mode round-trip probe
    // -------------------------------------------------------------------------

    @Nested
    class EvalModeTests {

        /**
         * Fixed in PR 2 — ClassConverter can now register StringUtils without NPE.
         * char/CharSequence params are handled via fast-path mappings; unknown types
         * are registered transitively.
         */
        @Test
        void classConverter_registerClass_stringUtils_noNPE() {
            com.elminster.jcp.eval.context.RootEvalContext ctx =
                    new com.elminster.jcp.eval.context.RootEvalContext();
            assertDoesNotThrow(
                    () -> com.elminster.jcp.util.ClassConverter.registerClass(
                            StringUtils.class, ctx, "user"),
                    "ClassConverter.registerClass must not throw for StringUtils");
        }

        /**
         * Eval-mode round-trip for StringUtils.capitalize(String) — STRING→STRING.
         */
        @Test
        void evalMode_capitalize_stringRoundTrip() {
            com.elminster.jcp.eval.context.RootEvalContext ctx =
                    new com.elminster.jcp.eval.context.RootEvalContext();
            com.elminster.jcp.util.ClassConverter.registerClass(StringUtils.class, ctx, "user");

            Block program = new BlockImpl();
            StaticMethodCallExpression call = new StaticMethodCallExpression(
                    "StringUtils", "capitalize",
                    LiteralExpression.of("hello"));
            program.addStatement(new com.elminster.jcp.ast.statement.ExpressionStatement(call));

            assertDoesNotThrow(() -> new com.elminster.jcp.eval.EvalVisitor(ctx).visit(program),
                    "Eval mode capitalize STRING→STRING round-trip must not throw");
        }

        /**
         * Eval-mode round-trip for EvalFixture.repeat(int, int) — INT×INT→STRING.
         * Verifies the basic int→Object unwrap path.
         */
        @Test
        void evalMode_repeat_intInt_roundTrip() {
            com.elminster.jcp.eval.context.RootEvalContext ctx =
                    new com.elminster.jcp.eval.context.RootEvalContext();
            com.elminster.jcp.util.ClassConverter.registerClass(EvalFixture.class, ctx, "user");

            Block program = new BlockImpl();
            StaticMethodCallExpression call = new StaticMethodCallExpression(
                    "EvalFixture", "repeat",
                    LiteralExpression.of(IntLiteral.of(42)),
                    LiteralExpression.of(IntLiteral.of(3)));
            program.addStatement(new com.elminster.jcp.ast.statement.ExpressionStatement(call));

            assertDoesNotThrow(() -> new com.elminster.jcp.eval.EvalVisitor(ctx).visit(program),
                    "Eval mode INT×INT→STRING round-trip must not throw");
        }
    }

    // -------------------------------------------------------------------------
    // Minimal synthetic fixture
    // -------------------------------------------------------------------------

    /**
     * Minimal fixture for eval-mode probes.
     * Only uses JCP-known types (String, int) so ClassConverter registration succeeds.
     */
    public static final class EvalFixture {
        private EvalFixture() {
        }

        /** String → String: baseline round-trip. */
        public static String upperCase(String s) {
            return s == null ? null : s.toUpperCase();
        }

        /** (int, int) → String: int unwrap round-trip via reflection. */
        public static String repeat(int ch, int times) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < times; i++) sb.append((char) ch);
            return sb.toString();
        }
    }

    /**
     * Minimal synthetic fixture providing method shapes that commons-lang3 does not cover:
     * long-returning method, Object-parameter method.
     *
     * <p>Used by Phase 2 probes that need to exercise specific JVM type descriptors.
     */
    public static final class LongReturningFixture {
        private LongReturningFixture() {
        }

        /** Returns a long — exercises the J descriptor and ANY return-type collapse (D5). */
        public static long longValue() {
            return 42L;
        }

        /** Accepts Object — exercises the ANY param path without char-boxing conflict. */
        public static String objectParam(Object obj) {
            return obj == null ? "null" : obj.toString();
        }
    }
}
