package com.elminster.jcp.module;

import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.ExternalClassType;
import com.elminster.jcp.module.base.BaseModuleRegister;
import com.elminster.jcp.util.DataTypeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class JcpModuleRegistryTest {

    @Test
    void registerClass_thenInitEval_classAvailable() {
        JcpModuleRegistry registry = new JcpModuleRegistry();
        registry.registerClass(StringBuilder.class, "test");

        EvalContext ctx = new RootEvalContext();
        registry.initEval(ctx);

        DataType dt = DataTypeUtils.getDataType("StringBuilder", ctx);
        assertNotNull(dt);
        assertInstanceOf(ExternalClassType.class, dt);
    }

    @Test
    void registerClass_thenInitCompile_classAvailable() {
        JcpModuleRegistry registry = new JcpModuleRegistry();
        registry.registerClass(StringBuilder.class, "test");

        CompileContext ctx = new CompileContext();
        registry.initCompile(ctx);

        DataType dt = ctx.getDataType("StringBuilder");
        assertNotNull(dt);
        assertInstanceOf(ExternalClassType.class, dt);
    }

    @Test
    void registrationOrder_preserved() {
        JcpModuleRegistry registry = new JcpModuleRegistry();
        registry.registerClass(StringBuilder.class, "a");
        registry.registerClass(String.class, "b");

        assertEquals(2, registry.getEntries().size());
        assertEquals(StringBuilder.class, registry.getEntries().get(0).getClazz());
        assertEquals("a", registry.getEntries().get(0).getModule());
        assertEquals(String.class, registry.getEntries().get(1).getClazz());
        assertEquals("b", registry.getEntries().get(1).getModule());
    }

    @Test
    void defaultInstance_containsBaseModuleClasses() {
        int baseModuleSize = BaseModuleRegister.classToRegister().size();
        assertTrue(JcpModuleRegistry.DEFAULT.getEntries().size() >= baseModuleSize);
    }

    @Test
    void registerJar_scansPublicClasses(@TempDir Path tempDir) throws Exception {
        // Build a JAR containing StringBuilder.class (from the JDK)
        // by reading it from the runtime and packaging it
        Path jarPath = tempDir.resolve("test.jar");
        String classResourcePath = StringBuilder.class.getName().replace('.', '/') + ".class";
        byte[] classBytes;
        try (var in = ClassLoader.getSystemResourceAsStream(classResourcePath)) {
            assertNotNull(in, "Could not load StringBuilder.class from runtime");
            classBytes = in.readAllBytes();
        }

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            jos.putNextEntry(new JarEntry(classResourcePath));
            jos.write(classBytes);
            jos.closeEntry();
        }

        JcpModuleRegistry registry = new JcpModuleRegistry();
        registry.registerJar(jarPath, "jar-test");

        // Should have at least one entry registered from the JAR
        assertTrue(registry.getEntries().size() >= 1);
        assertEquals(1, registry.getJarClassLoaders().size());
    }

    @Test
    void registerJar_initCompile_classAvailable(@TempDir Path tempDir) throws Exception {
        Path jarPath = tempDir.resolve("test.jar");
        String classResourcePath = StringBuilder.class.getName().replace('.', '/') + ".class";
        byte[] classBytes;
        try (var in = ClassLoader.getSystemResourceAsStream(classResourcePath)) {
            assertNotNull(in);
            classBytes = in.readAllBytes();
        }

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            jos.putNextEntry(new JarEntry(classResourcePath));
            jos.write(classBytes);
            jos.closeEntry();
        }

        JcpModuleRegistry registry = new JcpModuleRegistry();
        registry.registerJar(jarPath, "jar-test");

        CompileContext ctx = new CompileContext();
        registry.initCompile(ctx);

        DataType dt = ctx.getDataType("StringBuilder");
        assertNotNull(dt);
        assertInstanceOf(ExternalClassType.class, dt);
    }
}
