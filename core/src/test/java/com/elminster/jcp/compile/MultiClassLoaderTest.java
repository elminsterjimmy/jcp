package com.elminster.jcp.compile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MultiClassLoader.
 */
public class MultiClassLoaderTest {

    /**
     * Tests that loading a class that doesn't exist in definitions
     * falls through to the parent class loader.
     * This covers the bytecode == null branch in findClass.
     */
    @Test
    void testLoadClass_NotInDefinitions_DelegatesToParent() throws ClassNotFoundException {
        MultiClassLoader loader = new MultiClassLoader();

        // Try to load a standard JDK class (not in our definitions)
        Class<?> stringClass = loader.loadClass("java.lang.String");

        assertNotNull(stringClass);
        assertEquals(String.class, stringClass);
    }

    /**
     * Tests that findClass with undefined class throws ClassNotFoundException.
     */
    @Test
    void testFindClass_UndefinedClass_ThrowsException() {
        MultiClassLoader loader = new MultiClassLoader();

        // Try to find a class that doesn't exist anywhere
        assertThrows(ClassNotFoundException.class, () ->
            loader.loadClass("com.nonexistent.FakeClass")
        );
    }

    /**
     * Tests loading a defined class works correctly.
     */
    @Test
    void testLoadClass_DefinedClass_ReturnsClass() {
        MultiClassLoader loader = new MultiClassLoader();

        // Create a minimal valid class bytecode for testing
        // This is a simple empty class
        byte[] bytecode = createMinimalClassBytecode("TestClass");

        Class<?> clazz = loader.loadClass("TestClass", bytecode);

        assertNotNull(clazz);
        assertEquals("TestClass", clazz.getName());
    }

    /**
     * Creates minimal valid bytecode for an empty class.
     */
    private byte[] createMinimalClassBytecode(String className) {
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
        cw.visit(
            org.objectweb.asm.Opcodes.V1_8,
            org.objectweb.asm.Opcodes.ACC_PUBLIC,
            className,
            null,
            "java/lang/Object",
            null
        );
        cw.visitEnd();
        return cw.toByteArray();
    }
}
