package com.elminster.jcp.module;

import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.ExternalClassType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class JcpModuleRegistryJarTest {

    @TempDir
    Path tempDir;

    @Test
    void registerJar_publicClass_registersInEval() throws IOException {
        Path jar = buildTestJar("pub/Helper", true, false);
        try (JcpModuleRegistry registry = new JcpModuleRegistry()) {
            registry.registerJar(jar, "test");
            RootEvalContext ctx = new RootEvalContext();
            registry.initEval(ctx);
            assertNotNull(ctx.getDataType("Helper"));
            assertInstanceOf(ExternalClassType.class, ctx.getDataType("Helper"));
        }
    }

    @Test
    void registerJar_publicClass_registersInCompile() throws IOException {
        Path jar = buildTestJar("pub/Helper", true, false);
        try (JcpModuleRegistry registry = new JcpModuleRegistry()) {
            registry.registerJar(jar, "test");
            CompileContext ctx = new CompileContext();
            ctx.setClassName("Test");
            registry.initCompile(ctx);
            assertNotNull(ctx.getDataType("Helper"));
            assertInstanceOf(ExternalClassType.class, ctx.getDataType("Helper"));
        }
    }

    @Test
    void registerJar_innerClassSkipped() throws IOException {
        Path jar = buildJarWithInnerClass();
        try (JcpModuleRegistry registry = new JcpModuleRegistry()) {
            registry.registerJar(jar, "test");
            CompileContext ctx = new CompileContext();
            ctx.setClassName("Test");
            registry.initCompile(ctx);
            assertNotNull(ctx.getDataType("Outer"));
            assertNull(ctx.getDataTypeByFqn("inner.Outer$Inner"));
        }
    }

    @Test
    void registerJar_nonPublicClassSkipped() throws IOException {
        Path jar = buildTestJar("pkg/Internal", false, false);
        try (JcpModuleRegistry registry = new JcpModuleRegistry()) {
            registry.registerJar(jar, "test");
            CompileContext ctx = new CompileContext();
            ctx.setClassName("Test");
            registry.initCompile(ctx);
            assertNull(ctx.getDataTypeByFqn("pkg.Internal"));
        }
    }

    @Test
    void getJarLoaders_containsUrlClassLoader() throws IOException {
        Path jar = buildTestJar("pub/Helper", true, false);
        try (JcpModuleRegistry registry = new JcpModuleRegistry()) {
            assertTrue(registry.getJarLoaders().isEmpty());
            registry.registerJar(jar, "test");
            assertEquals(1, registry.getJarLoaders().size());
        }
    }

    @Test
    void close_closesUrlClassLoaders() throws IOException {
        Path jar = buildTestJar("pub/Helper", true, false);
        JcpModuleRegistry registry = new JcpModuleRegistry();
        registry.registerJar(jar, "test");
        assertEquals(1, registry.getJarLoaders().size());
        registry.close();
        assertTrue(registry.getJarLoaders().isEmpty());
    }

    private Path buildTestJar(String internalName, boolean isPublic, boolean addInner) throws IOException {
        byte[] bytecode = buildClassBytecode(internalName, isPublic);
        Path jarPath = tempDir.resolve("test-" + System.nanoTime() + ".jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry(internalName + ".class"));
            jos.write(bytecode);
            jos.closeEntry();
        }
        return jarPath;
    }

    private Path buildJarWithInnerClass() throws IOException {
        byte[] outerBytecode = buildClassBytecode("inner/Outer", true);
        byte[] innerBytecode = buildClassBytecode("inner/Outer$Inner", true);
        Path jarPath = tempDir.resolve("inner-" + System.nanoTime() + ".jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry("inner/Outer.class"));
            jos.write(outerBytecode);
            jos.closeEntry();
            jos.putNextEntry(new JarEntry("inner/Outer$Inner.class"));
            jos.write(innerBytecode);
            jos.closeEntry();
        }
        return jarPath;
    }

    private byte[] buildClassBytecode(String internalName, boolean isPublic) {
        ClassWriter cw = new ClassWriter(0);
        int access = Opcodes.ACC_SUPER | (isPublic ? Opcodes.ACC_PUBLIC : 0);
        cw.visit(Opcodes.V1_8, access, internalName, null, "java/lang/Object", null);

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();

        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "hello", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitLdcInsn("hello from " + internalName);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
