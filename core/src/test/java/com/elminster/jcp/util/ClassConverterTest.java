package com.elminster.jcp.util;

import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import com.elminster.jcp.eval.data.ExternalClassType;
import com.elminster.jcp.eval.data.NamespacedTypeTable;
import com.elminster.jcp.eval.excpetion.EvaluationException;
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
    class CollisionTests {

        /**
         * Two classes sharing a simple name coexist — both register without error.
         */
        @Test
        void testSameSimpleNameDifferentPackageCoexist() {
            // Register java.util.Date and java.sql.Date — both named "Date"
            context.addDataType(new ExternalClassType("Date", java.util.Date.class));
            assertDoesNotThrow(() ->
                context.addDataType(new ExternalClassType("Date", java.sql.Date.class)));
        }

        /**
         * After both Date classes are registered, simple-name lookup throws EvaluationException.
         */
        @Test
        void testAmbiguousSimpleNameThrowsEvaluationException() {
            context.addDataType(new ExternalClassType("Date", java.util.Date.class));
            context.addDataType(new ExternalClassType("Date", java.sql.Date.class));

            EvaluationException ex = assertThrows(EvaluationException.class,
                () -> context.getDataType("Date"));
            assertTrue(ex.getMessage().contains("java.util.Date"), "message should name java.util.Date");
            assertTrue(ex.getMessage().contains("java.sql.Date"),  "message should name java.sql.Date");
        }

        /**
         * Registering the same class twice is idempotent — no exception thrown.
         */
        @Test
        void testSameClassRegisteredTwiceIsIdempotent() {
            context.addDataType(new ExternalClassType("Date", java.util.Date.class));
            assertDoesNotThrow(() ->
                context.addDataType(new ExternalClassType("Date", java.util.Date.class)));

            DataType dt = context.getDataType("Date");
            assertInstanceOf(ExternalClassType.class, dt);
            assertEquals("java.util.Date", ((ExternalClassType) dt).getFullName());
        }

        /**
         * A JCP struct type shadows an ExternalClassType with the same simple name.
         */
        @Test
        void testStructTypeShadowsExternalClassType() {
            context.addDataType(new ExternalClassType("Date", java.util.Date.class));
            DataTypeImpl structDate = new DataTypeImpl("Date");
            context.addDataType(structDate);

            DataType resolved = context.getDataType("Date");
            assertSame(structDate, resolved);
        }

        /**
         * ClassConverter.registerClass with two same-simple-name classes registers both.
         */
        @Test
        void testRegisterClassCoexistenceViaClassConverter() {
            ClassConverter.registerClass(java.util.Date.class, context, "test");
            assertDoesNotThrow(() ->
                ClassConverter.registerClass(java.sql.Date.class, context, "test"));
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
