package com.elminster.jcp.util;

import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClassConverter.
 * Tests are minimal since ClassConverter requires complex registration.
 */
class ClassConverterTest {

    private EvalContext context;

    @BeforeEach
    void setUp() {
        context = new RootEvalContext();
    }

    /**
     * Simple test class with a simple static method.
     */
    public static class SimpleHelper {
        public static int twice(int x) {
            return x * 2;
        }

        public int addOne(int x) {
            return x + 1;
        }

        public void doNothing() {
        }
    }

    @Nested
    class BasicTests {

        /**
         * Tests registering SimpleHelper class.
         * <pre>
         * registerClass(SimpleHelper.class)
         * // should register SimpleHelper as DataType
         * </pre>
         */
        @Test
        void testRegisterSimpleHelper() {
            ClassConverter.registerClass(SimpleHelper.class, context, "test");

            // Should have registered SimpleHelper as a DataType
            DataType helperType = context.getDataType("SimpleHelper");
            assertNotNull(helperType);
        }

        /**
         * Tests that static method is registered.
         * <pre>
         * registerClass(SimpleHelper.class)
         * // twice should be registered
         * </pre>
         */
        @Test
        void testStaticMethodRegistered() {
            ClassConverter.registerClass(SimpleHelper.class, context, "test");

            // Should have static method registered
            boolean hasTwice = context.getFunctions().values().stream()
                .anyMatch(f -> f.getName().contains("twice"));
            assertTrue(hasTwice);
        }

        /**
         * Tests that instance method is registered.
         * <pre>
         * registerClass(SimpleHelper.class)
         * // addOne should be registered
         * </pre>
         */
        @Test
        void testInstanceMethodRegistered() {
            ClassConverter.registerClass(SimpleHelper.class, context, "test");

            // Should have instance method registered
            boolean hasAddOne = context.getFunctions().values().stream()
                .anyMatch(f -> f.getName().contains("addOne"));
            assertTrue(hasAddOne);
        }

        /**
         * Tests that void method is registered.
         * <pre>
         * registerClass(SimpleHelper.class)
         * // doNothing should be registered
         * </pre>
         */
        @Test
        void testVoidMethodRegistered() {
            ClassConverter.registerClass(SimpleHelper.class, context, "test");

            // Should have void method registered
            boolean hasDoNothing = context.getFunctions().values().stream()
                .anyMatch(f -> f.getName().contains("doNothing"));
            assertTrue(hasDoNothing);
        }
    }
}
