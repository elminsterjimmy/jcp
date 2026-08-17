package com.elminster.jcp.compile.util;

import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import com.elminster.jcp.eval.data.ExternalClassType;
import com.elminster.jcp.eval.data.ExternalMethodDef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CompileModeClassConverter.
 */
class CompileModeClassConverterTest {

    private CompileContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new CompileContext();
        ctx.setClassName("TestClass");
    }

    @Nested
    class TypeMappingTests {

        /**
         * Tests mapping int primitive to INT.
         * <pre>
         * int -> SystemDataType.INT
         * </pre>
         */
        @Test
        void testMapJavaType_IntPrimitive() {
            DataType type = CompileModeClassConverter.mapJavaTypeToDataType(int.class);
            assertEquals(SystemDataType.INT, type);
        }

        /**
         * Tests mapping Integer wrapper to INT.
         * <pre>
         * Integer -> SystemDataType.INT
         * </pre>
         */
        @Test
        void testMapJavaType_IntegerWrapper() {
            DataType type = CompileModeClassConverter.mapJavaTypeToDataType(Integer.class);
            assertEquals(SystemDataType.INT, type);
        }

        /**
         * Tests mapping double primitive to DOUBLE.
         * <pre>
         * double -> SystemDataType.DOUBLE
         * </pre>
         */
        @Test
        void testMapJavaType_DoublePrimitive() {
            DataType type = CompileModeClassConverter.mapJavaTypeToDataType(double.class);
            assertEquals(SystemDataType.DOUBLE, type);
        }

        /**
         * Tests mapping Double wrapper to DOUBLE.
         * <pre>
         * Double -> SystemDataType.DOUBLE
         * </pre>
         */
        @Test
        void testMapJavaType_DoubleWrapper() {
            DataType type = CompileModeClassConverter.mapJavaTypeToDataType(Double.class);
            assertEquals(SystemDataType.DOUBLE, type);
        }

        /**
         * Tests mapping boolean primitive to BOOLEAN.
         * <pre>
         * boolean -> SystemDataType.BOOLEAN
         * </pre>
         */
        @Test
        void testMapJavaType_BooleanPrimitive() {
            DataType type = CompileModeClassConverter.mapJavaTypeToDataType(boolean.class);
            assertEquals(SystemDataType.BOOLEAN, type);
        }

        /**
         * Tests mapping Boolean wrapper to BOOLEAN.
         * <pre>
         * Boolean -> SystemDataType.BOOLEAN
         * </pre>
         */
        @Test
        void testMapJavaType_BooleanWrapper() {
            DataType type = CompileModeClassConverter.mapJavaTypeToDataType(Boolean.class);
            assertEquals(SystemDataType.BOOLEAN, type);
        }

        /**
         * Tests mapping void to VOID.
         * <pre>
         * void -> SystemDataType.VOID
         * </pre>
         */
        @Test
        void testMapJavaType_Void() {
            DataType type = CompileModeClassConverter.mapJavaTypeToDataType(void.class);
            assertEquals(SystemDataType.VOID, type);
        }

        /**
         * Tests mapping Void wrapper to VOID.
         * <pre>
         * Void -> SystemDataType.VOID
         * </pre>
         */
        @Test
        void testMapJavaType_VoidWrapper() {
            DataType type = CompileModeClassConverter.mapJavaTypeToDataType(Void.class);
            assertEquals(SystemDataType.VOID, type);
        }

        /**
         * Tests mapping String to STRING.
         * <pre>
         * String -> SystemDataType.STRING
         * </pre>
         */
        @Test
        void testMapJavaType_String() {
            DataType type = CompileModeClassConverter.mapJavaTypeToDataType(String.class);
            assertEquals(SystemDataType.STRING, type);
        }

        /**
         * Tests mapping int[] to INT_ARRAY.
         * <pre>
         * int[] -> SystemDataType.INT_ARRAY
         * </pre>
         */
        @Test
        void testMapJavaType_IntArray() {
            DataType type = CompileModeClassConverter.mapJavaTypeToDataType(int[].class);
            assertEquals(SystemDataType.INT_ARRAY, type);
        }

        /**
         * Tests mapping double[] to DOUBLE_ARRAY.
         * <pre>
         * double[] -> SystemDataType.DOUBLE_ARRAY
         * </pre>
         */
        @Test
        void testMapJavaType_DoubleArray() {
            DataType type = CompileModeClassConverter.mapJavaTypeToDataType(double[].class);
            assertEquals(SystemDataType.DOUBLE_ARRAY, type);
        }

        /**
         * Tests mapping boolean[] to BOOLEAN_ARRAY.
         * <pre>
         * boolean[] -> SystemDataType.BOOLEAN_ARRAY
         * </pre>
         */
        @Test
        void testMapJavaType_BooleanArray() {
            DataType type = CompileModeClassConverter.mapJavaTypeToDataType(boolean[].class);
            assertEquals(SystemDataType.BOOLEAN_ARRAY, type);
        }

        /**
         * Tests mapping String[] to STRING_ARRAY.
         * <pre>
         * String[] -> SystemDataType.STRING_ARRAY
         * </pre>
         */
        @Test
        void testMapJavaType_StringArray() {
            DataType type = CompileModeClassConverter.mapJavaTypeToDataType(String[].class);
            assertEquals(SystemDataType.STRING_ARRAY, type);
        }

        /**
         * Tests mapping Object to ANY.
         * <pre>
         * Object -> SystemDataType.ANY
         * </pre>
         */
        @Test
        void testMapJavaType_Object() {
            DataType type = CompileModeClassConverter.mapJavaTypeToDataType(Object.class);
            assertEquals(SystemDataType.ANY, type);
        }

        /**
         * Tests mapping custom class to ANY.
         * <pre>
         * CustomClass -> SystemDataType.ANY
         * </pre>
         */
        @Test
        void testMapJavaType_CustomClass() {
            DataType type = CompileModeClassConverter.mapJavaTypeToDataType(CompileModeClassConverterTest.class);
            assertEquals(SystemDataType.ANY, type);
        }
    }

    @Nested
    class CollisionTests {

        /**
         * Two classes with the same simple name but different packages can coexist.
         * Both are retrievable individually; simple-name lookup is ambiguous.
         */
        @Test
        void testSameSimpleNameDifferentPackageCoexist() {
            DataType utilDate = CompileModeClassConverter.mapJavaTypeToDataType(java.util.Date.class, ctx, "test");
            DataType sqlDate  = CompileModeClassConverter.mapJavaTypeToDataType(java.sql.Date.class,  ctx, "test");

            assertInstanceOf(ExternalClassType.class, utilDate);
            assertInstanceOf(ExternalClassType.class, sqlDate);
            assertEquals("java.util.Date", ((ExternalClassType) utilDate).getFqn());
            assertEquals("java.sql.Date",  ((ExternalClassType) sqlDate).getFqn());
        }

        /**
         * After both Date classes are registered, looking up "Date" by simple name
         * throws CompileException naming both FQNs.
         */
        @Test
        void testAmbiguousSimpleNameThrowsCompileException() {
            CompileModeClassConverter.mapJavaTypeToDataType(java.util.Date.class, ctx, "test");
            CompileModeClassConverter.mapJavaTypeToDataType(java.sql.Date.class,  ctx, "test");

            com.elminster.jcp.compile.exception.CompileException ex =
                assertThrows(com.elminster.jcp.compile.exception.CompileException.class,
                    () -> ctx.getDataType("Date"));
            assertTrue(ex.getMessage().contains("java.util.Date"), "message should name java.util.Date");
            assertTrue(ex.getMessage().contains("java.sql.Date"),  "message should name java.sql.Date");
        }

        /**
         * Registering the same class twice is idempotent — no exception thrown.
         */
        @Test
        void testSameClassRegisteredTwiceIsIdempotent() {
            assertDoesNotThrow(() -> {
                CompileModeClassConverter.mapJavaTypeToDataType(java.util.Date.class, ctx, "test");
                CompileModeClassConverter.mapJavaTypeToDataType(java.util.Date.class, ctx, "test");
            });
            DataType dt = ctx.getDataType("Date");
            assertInstanceOf(ExternalClassType.class, dt);
            assertEquals("java.util.Date", ((ExternalClassType) dt).getFqn());
        }

        /**
         * A JCP struct type named "Math" shadows an ExternalClassType also named "Math".
         */
        @Test
        void testStructTypeShadowsExternalClassType() {
            // Register the external Java Math class first
            CompileModeClassConverter.registerClass(
                com.elminster.jcp.module.base.math.Math.class, ctx, "base");

            // Then register a JCP struct type named "Math"
            DataTypeImpl structMath = new DataTypeImpl("Math");
            ctx.addDataType(structMath);

            // Struct wins
            DataType resolved = ctx.getDataType("Math");
            assertSame(structMath, resolved);
        }
    }

    @Nested
    class ClassRegistrationTests {

        /**
         * Tests registering StringBuilder class.
         * <pre>
         * StringBuilder has instance methods and constructors
         * </pre>
         */
        @Test
        void testRegisterClass_StringBuilder() {
            CompileModeClassConverter.registerClass(StringBuilder.class, ctx, "core");

            DataType type = ctx.getDataType("StringBuilder");
            assertNotNull(type);
            assertTrue(type instanceof ExternalClassType);

            ExternalClassType extType = (ExternalClassType) type;
            assertEquals("StringBuilder", extType.getName());

            // Should have instance methods
            assertFalse(extType.getInstanceMethods().isEmpty());

            // Should have toString method (no overloads)
            ExternalMethodDef toStringMethod = extType.getInstanceMethod("toString", new DataType[]{});
            assertNotNull(toStringMethod);
        }

        /**
         * Tests registering String class (has both static and instance methods).
         * <pre>
         * String has valueOf (static), length (instance), etc.
         * </pre>
         */
        @Test
        void testRegisterClass_String() {
            CompileModeClassConverter.registerClass(String.class, ctx, "core");

            DataType type = ctx.getDataType("String");
            assertNotNull(type);
            assertTrue(type instanceof ExternalClassType);

            ExternalClassType extType = (ExternalClassType) type;

            // Should have static methods like valueOf
            assertFalse(extType.getStaticMethods().isEmpty());

            // Should have instance methods like length
            ExternalMethodDef lengthMethod = extType.getInstanceMethod("length", new DataType[]{});
            assertNotNull(lengthMethod);
        }

        /**
         * Tests registering Integer class (has static and instance methods).
         * <pre>
         * Integer has valueOf (static), intValue (instance)
         * </pre>
         */
        @Test
        void testRegisterClass_Integer() {
            CompileModeClassConverter.registerClass(Integer.class, ctx, "core");

            DataType type = ctx.getDataType("Integer");
            assertNotNull(type);
            assertTrue(type instanceof ExternalClassType);

            ExternalClassType extType = (ExternalClassType) type;

            // Should have intValue instance method
            ExternalMethodDef intValueMethod = extType.getInstanceMethod("intValue", new DataType[]{});
            assertNotNull(intValueMethod);
        }
    }
}
