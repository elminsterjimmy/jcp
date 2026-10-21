package com.elminster.jcp.module;

import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.ExternalClassType;
import com.elminster.jcp.module.base.BaseModuleRegister;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JcpModuleRegistryTest {

    @Test
    void default_singleton_preloadsBaseModule() {
        int baseClassCount = BaseModuleRegister.classToRegister().size();
        assertEquals(baseClassCount, JcpModuleRegistry.DEFAULT.entryCount());
    }

    @Test
    void registerClass_addsEntry_availableInEval() {
        JcpModuleRegistry registry = new JcpModuleRegistry();
        registry.registerClass(TestHelper.class, "test");

        EvalContext ctx = new RootEvalContext();
        registry.initEval(ctx);

        assertNotNull(ctx.getDataType("TestHelper"));
        assertInstanceOf(ExternalClassType.class, ctx.getDataType("TestHelper"));
    }

    @Test
    void registerClass_addsEntry_availableInCompile() {
        JcpModuleRegistry registry = new JcpModuleRegistry();
        registry.registerClass(TestHelper.class, "test");

        CompileContext ctx = new CompileContext();
        ctx.setClassName("TestClass");
        registry.initCompile(ctx);

        assertNotNull(ctx.getDataType("TestHelper"));
        assertInstanceOf(ExternalClassType.class, ctx.getDataType("TestHelper"));
    }

    @Test
    void multipleRegistrations_preserveInsertionOrder() {
        JcpModuleRegistry registry = new JcpModuleRegistry();
        registry.registerClass(TestHelper.class, "a");
        registry.registerClass(TestHelper2.class, "b");
        assertEquals(2, registry.entryCount());
    }

    @Test
    void getJarLoaders_emptyByDefault() {
        JcpModuleRegistry registry = new JcpModuleRegistry();
        assertTrue(registry.getJarLoaders().isEmpty());
    }

    public static class TestHelper {
        public static String hello() { return "hello"; }
    }

    public static class TestHelper2 {
        public static int add(int a, int b) { return a + b; }
    }
}
