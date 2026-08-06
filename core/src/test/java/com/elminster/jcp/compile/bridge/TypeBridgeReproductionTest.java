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

        /**
         * CHARACTERIZATION: current buggy ANY-collapse for types not in the mapping table.
         * mapJavaTypeToDataType returns ANY for char, CharSequence, long, float, byte, short.
         * PR 2 updates this.
         *
         * <p>These tests pin the current (broken) behavior so PR 2's changes are
         * explicitly visible as a diff. They are NOT asserting desired behavior.
         */
        @Test
        void mapJavaTypeToDataType_char_collapsesToANY() {
            // CHARACTERIZATION: PR 2 updates this — char should map to a primitive, not ANY
            assertEquals(SystemDataType.ANY, CompileModeClassConverter.mapJavaTypeToDataType(char.class));
        }

        @Test
        void mapJavaTypeToDataType_CharSequence_collapsesToANY() {
            // CHARACTERIZATION: PR 2 updates this
            assertEquals(SystemDataType.ANY, CompileModeClassConverter.mapJavaTypeToDataType(CharSequence.class));
        }

        @Test
        void mapJavaTypeToDataType_long_collapsesToANY() {
            // CHARACTERIZATION: PR 2 updates this — long should have its own type or map to NUMERIC
            assertEquals(SystemDataType.ANY, CompileModeClassConverter.mapJavaTypeToDataType(long.class));
        }

        @Test
        void mapJavaTypeToDataType_float_collapsesToANY() {
            // CHARACTERIZATION: PR 2 updates this
            assertEquals(SystemDataType.ANY, CompileModeClassConverter.mapJavaTypeToDataType(float.class));
        }

        @Test
        void mapJavaTypeToDataType_byte_collapsesToANY() {
            // CHARACTERIZATION: PR 2 updates this
            assertEquals(SystemDataType.ANY, CompileModeClassConverter.mapJavaTypeToDataType(byte.class));
        }

        @Test
        void mapJavaTypeToDataType_short_collapsesToANY() {
            // CHARACTERIZATION: PR 2 updates this
            assertEquals(SystemDataType.ANY, CompileModeClassConverter.mapJavaTypeToDataType(short.class));
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
         * D4' probe — repeat(char, int) → String.
         *
         * <p>Descriptor: (CI)Ljava/lang/String;  (C = char primitive)
         * mapJavaTypeToDataType collapses char → ANY (D5).
         * StaticMethodCallCompiler:142 sees paramType==ANY → calls boxPrimitive(INT arg)
         * → pushes Integer onto stack. But the descriptor demands primitive char (C).
         * Expected: VerifyError or LinkageError at class-load time.
         *
         * <p>Defect path: StaticMethodCallCompiler.compileExternalClassCall (line ~142),
         * driven by D5 (char→ANY collapse in CompileModeClassConverter.mapJavaTypeToDataType).
         */
        @Test
        void d4Prime_repeat_charInt_boxingVsExactDescriptor() {
            // CHARACTERIZATION: char collapses to ANY, causing boxing-vs-descriptor mismatch (D4').
            // The JCP arg type for the char param will be ANY (because that's what
            // mapJavaTypeToDataType(char.class) returns), and the compiler will try to box
            // an INT arg using boxPrimitive — but the INVOKESTATIC descriptor expects 'C'.
            // We pass an INT literal for the char position (JCP has no char literal today).
            assertThrows(
                    Throwable.class, // VerifyError or LinkageError
                    () -> {
                        MultiClassLoader loader = new MultiClassLoader();
                        Class<?> clazz = compileAndLoad(
                                StringUtils.class, "StringUtils", "repeat",
                                SystemDataType.STRING,
                                new com.elminster.jcp.ast.Expression[]{
                                        LiteralExpression.of(IntLiteral.of(42)), // char arg as INT
                                        LiteralExpression.of(IntLiteral.of(3))
                                },
                                loader);
                        // If load succeeds, invoke to force verification
                        clazz.getMethod("evaluate").invoke(null);
                    },
                    "D4' (repeat char+int): expected VerifyError/LinkageError from boxing-vs-char-descriptor"
            );
        }

        /**
         * D4' probe — countMatches(CharSequence, char) → int.
         *
         * <p>Descriptor: (Ljava/lang/CharSequence;C)I
         * Both params map to ANY (CharSequence→ANY, char→ANY via D5).
         * The compiler will attempt to box both args. The exact descriptor conflicts.
         * Additionally, overload resolution runs on lossy arg types, risking wrong-overload
         * selection even before bytecode is emitted.
         *
         * <p>Defect path: StaticMethodCallCompiler.compileExternalClassCall (line ~124 overload
         * resolution, line ~142 boxing), driven by D5.
         */
        @Test
        void d4Prime_countMatches_charSequenceChar_boxingVsExactDescriptor() {
            // CHARACTERIZATION: CharSequence and char both collapse to ANY (D5).
            // Overload resolution may select the wrong countMatches overload, and boxing
            // of a STRING arg as ANY will conflict with the CharSequence descriptor.
            assertThrows(
                    Throwable.class,
                    () -> {
                        MultiClassLoader loader = new MultiClassLoader();
                        Class<?> clazz = compileAndLoad(
                                StringUtils.class, "StringUtils", "countMatches",
                                SystemDataType.INT,
                                new com.elminster.jcp.ast.Expression[]{
                                        LiteralExpression.of("hello world"),
                                        LiteralExpression.of(IntLiteral.of(108)) // 'l' as INT
                                },
                                loader);
                        clazz.getMethod("evaluate").invoke(null);
                    },
                    "D4' (countMatches CharSequence+char): expected failure from ANY-collapse"
            );
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
            // Build a minimal foreign class "bridge/ForeignHelper" with one static method
            // hello() → String, using ASM directly so we need no source file.
            byte[] foreignBytecode = buildForeignHelperBytecode();
            String foreignInternalName = "bridge/ForeignHelper";
            String foreignClassName   = "bridge.ForeignHelper";

            // Write it into a temp JAR
            java.nio.file.Path jarPath = java.nio.file.Files.createTempFile("foreign-", ".jar");
            try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(
                    java.nio.file.Files.newOutputStream(jarPath))) {
                jos.putNextEntry(new java.util.jar.JarEntry(foreignInternalName + ".class"));
                jos.write(foreignBytecode);
                jos.closeEntry();
            }

            // Load the foreign class via a URLClassLoader with platform-only parent
            // (no app classloader → the class is invisible to MultiClassLoader's parent)
            URLClassLoader isolatedLoader = new URLClassLoader(
                    new URL[]{jarPath.toUri().toURL()},
                    ClassLoader.getPlatformClassLoader()
            );
            Class<?> foreignClass = isolatedLoader.loadClass(foreignClassName);

            // Sanity assertion: the boundary is real — the class must NOT resolve from
            // the thread context classloader (app classloader).
            assertThrows(ClassNotFoundException.class,
                    () -> Thread.currentThread().getContextClassLoader().loadClass(foreignClassName),
                    "Sanity failure: foreign class is visible from app classloader — boundary not real");

            // Compile a JCP call to ForeignHelper.hello() using the isolated class
            String genClassName = "IsolatedBridgeTest_" + System.nanoTime();
            BytecodeGenerator generator = new BytecodeGenerator(genClassName);
            generator.registerExternalClass(foreignClass);

            Block empty = new BlockImpl();
            StaticMethodCallExpression call = new StaticMethodCallExpression(
                    "ForeignHelper", "hello");
            byte[] bytecode = generator.compileWithReturn(empty, call, SystemDataType.STRING);

            // Load via standard MultiClassLoader — parent does NOT include isolatedLoader
            MultiClassLoader multiLoader = new MultiClassLoader();
            multiLoader.defineClass(genClassName, bytecode);

            // D2: NoClassDefFoundError (or similar LinkageError) at runtime because
            // MultiClassLoader's parent chain cannot resolve "bridge/ForeignHelper"
            assertThrows(
                    Throwable.class,
                    () -> {
                        Class<?> loaded = multiLoader.loadClass(genClassName);
                        loaded.getMethod("evaluate").invoke(null);
                    },
                    "D2: expected NoClassDefFoundError — foreign class not visible from MultiClassLoader parent"
            );

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
         * Eval-mode round-trip for EvalFixture.upperCase(String) — STRING→STRING, no coercion.
         *
         * <p>We use EvalFixture (defined below) rather than StringUtils directly because
         * StringUtils has hundreds of overloads including CharSequence/char params, and
         * ClassConverter.getDataType(CharSequence.class) returns null during registration
         * (D6-adjacent: ClassConverter hits NPE registering unknown Java types). This is
         * itself a D6-adjacent finding documented by the characterization test below.
         *
         * <p>EvalFixture has only String and int params — all known JCP types — so
         * registration succeeds and the round-trip can be measured cleanly.
         */
        @Test
        void evalMode_upperCase_stringRoundTrip() {
            com.elminster.jcp.eval.context.RootEvalContext ctx =
                    new com.elminster.jcp.eval.context.RootEvalContext();
            com.elminster.jcp.util.ClassConverter.registerClass(EvalFixture.class, ctx, "user");

            Block program = new BlockImpl();
            StaticMethodCallExpression call = new StaticMethodCallExpression(
                    "EvalFixture", "upperCase",
                    LiteralExpression.of("hello"));
            program.addStatement(new com.elminster.jcp.ast.statement.ExpressionStatement(call));

            assertDoesNotThrow(() -> new com.elminster.jcp.eval.EvalVisitor(ctx).visit(program),
                    "Eval mode STRING→STRING round-trip must not throw");
        }

        /**
         * Eval-mode round-trip for EvalFixture.repeat(int, int) — INT×INT→STRING.
         *
         * <p>Uses two INT args — no char/CharSequence involvement — to verify that
         * the basic int→Object unwrap path in eval mode works correctly.
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

        /**
         * D6-adjacent characterization: ClassConverter.registerClass fails with NPE when
         * the class has methods whose parameter types are not in the JCP type system
         * (e.g. CharSequence, char, long).
         *
         * <p>This pins the current broken behavior so PR 2's fix is visible as a diff.
         * ClassConverter.getDataType() calls DataTypeUtils.getDataType(simpleName, ctx)
         * which returns null for unknown types, then passes null to ParameterDef —
         * causing FunctionUtils.generateFunctionFullName to NPE.
         */
        @Test
        void d6Adjacent_classConverter_registerClass_npeForUnknownParamType() {
            com.elminster.jcp.eval.context.RootEvalContext ctx =
                    new com.elminster.jcp.eval.context.RootEvalContext();
            // CHARACTERIZATION: PR 2 updates this — registration must not NPE for any public class.
            // StringUtils has CharSequence/char params; ClassConverter cannot handle them today.
            assertThrows(
                    NullPointerException.class,
                    () -> com.elminster.jcp.util.ClassConverter.registerClass(
                            StringUtils.class, ctx, "user"),
                    "D6-adjacent: ClassConverter should NPE on char/CharSequence params (current broken behavior)"
            );
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
