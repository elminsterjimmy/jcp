package com.elminster.jcp.util;

import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.module.ModuleFunction;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ModuleLoader.
 */
class ModuleLoaderTest {

    /**
     * Tests loading functions from non-existent module returns empty set.
     * <pre>
     * loadModuleFunctions("nonExistent")  // returns empty set
     * </pre>
     */
    @Test
    void testLoadModuleFunctions_NonExistent() {
        Set<Function> functions = ModuleLoader.INSTANCE.loadModuleFunctions("nonExistent_" + System.nanoTime());
        assertNotNull(functions);
        assertTrue(functions.isEmpty());
    }

    /**
     * Tests loading classes from non-existent module returns empty set.
     * <pre>
     * loadModuleClasses("nonExistent")  // returns empty set
     * </pre>
     */
    @Test
    void testLoadModuleClasses_NonExistent() {
        Set<Class<?>> classes = ModuleLoader.INSTANCE.loadModuleClasses("nonExistent_" + System.nanoTime());
        assertNotNull(classes);
        assertTrue(classes.isEmpty());
    }

    /**
     * Tests registering and loading module class.
     * <pre>
     * registerModuleClass(String.class, "test")
     * loadModuleClasses("test")  // returns set with String.class
     * </pre>
     */
    @Test
    void testRegisterAndLoadModuleClass() {
        String moduleName = "testModule_" + System.nanoTime();
        ModuleLoader.INSTANCE.registerModuleClass(String.class, moduleName);

        Set<Class<?>> classes = ModuleLoader.INSTANCE.loadModuleClasses(moduleName);
        assertNotNull(classes);
        assertTrue(classes.contains(String.class));
    }

    /**
     * Tests registering class to existing module adds to set.
     * <pre>
     * registerModuleClass(String.class, "test")
     * registerModuleClass(Integer.class, "test")
     * loadModuleClasses("test")  // returns set with both
     * </pre>
     */
    @Test
    void testRegisterMultipleClassesToSameModule() {
        String moduleName = "multiClassModule_" + System.nanoTime();
        ModuleLoader.INSTANCE.registerModuleClass(String.class, moduleName);
        ModuleLoader.INSTANCE.registerModuleClass(Integer.class, moduleName);

        Set<Class<?>> classes = ModuleLoader.INSTANCE.loadModuleClasses(moduleName);
        assertEquals(2, classes.size());
        assertTrue(classes.contains(String.class));
        assertTrue(classes.contains(Integer.class));
    }
}
