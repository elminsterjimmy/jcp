package com.elminster.jcp.eval.data;

import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExternalClassType.
 */
class ExternalClassTypeTest {

    private ExternalClassType classType;

    @BeforeEach
    void setUp() {
        classType = new ExternalClassType("TestClass", String.class);
    }

    @Test
    void testGetName() {
        assertEquals("TestClass", classType.getName());
    }

    @Test
    void testGetParent() {
        assertEquals(SystemDataType.ANY, classType.getParent());
    }

    @Test
    void testGetJavaClass() {
        assertEquals(String.class, classType.getJavaClass());
    }

    @Test
    void testGetInternalName() {
        assertEquals("java/lang/String", classType.getInternalName());
    }

    @Nested
    class StaticMethodTests {

        @Test
        void testAddAndGetStaticMethod() throws Exception {
            Method valueOf = String.class.getMethod("valueOf", int.class);
            ExternalMethodDef method = new ExternalMethodDef(
                valueOf,
                "valueOf",
                new DataType[]{SystemDataType.INT},
                SystemDataType.STRING,
                "(I)Ljava/lang/String;",
                true
            );
            classType.addStaticMethod(method);

            ExternalMethodDef found = classType.getStaticMethod("valueOf", new DataType[]{SystemDataType.INT});
            assertNotNull(found);
            assertEquals("valueOf", found.getName());
        }

        @Test
        void testGetStaticMethod_NotFound() {
            ExternalMethodDef found = classType.getStaticMethod("nonExistent", new DataType[]{});
            assertNull(found);
        }

        @Test
        void testGetStaticMethod_WrongParamCount() throws Exception {
            Method valueOf = String.class.getMethod("valueOf", int.class);
            ExternalMethodDef method = new ExternalMethodDef(
                valueOf,
                "valueOf",
                new DataType[]{SystemDataType.INT},
                SystemDataType.STRING,
                "(I)Ljava/lang/String;",
                true
            );
            classType.addStaticMethod(method);

            // Try with no args when method expects one
            ExternalMethodDef found = classType.getStaticMethod("valueOf", new DataType[]{});
            assertNull(found);
        }

        @Test
        void testGetStaticMethods() throws Exception {
            Method valueOf = String.class.getMethod("valueOf", int.class);
            ExternalMethodDef method = new ExternalMethodDef(
                valueOf,
                "test",
                new DataType[]{},
                SystemDataType.VOID,
                "()V",
                true
            );
            classType.addStaticMethod(method);

            assertFalse(classType.getStaticMethods().isEmpty());
            assertTrue(classType.getStaticMethods().containsKey("test"));
        }
    }

    @Nested
    class InstanceMethodTests {

        @Test
        void testAddAndGetInstanceMethod() throws Exception {
            Method length = String.class.getMethod("length");
            ExternalMethodDef method = new ExternalMethodDef(
                length,
                "length",
                new DataType[]{},
                SystemDataType.INT,
                "()I",
                false
            );
            classType.addInstanceMethod(method);

            ExternalMethodDef found = classType.getInstanceMethod("length", new DataType[]{});
            assertNotNull(found);
            assertEquals("length", found.getName());
        }

        @Test
        void testGetInstanceMethod_NotFound() {
            ExternalMethodDef found = classType.getInstanceMethod("nonExistent", new DataType[]{});
            assertNull(found);
        }

        @Test
        void testGetInstanceMethods() throws Exception {
            Method charAt = String.class.getMethod("charAt", int.class);
            ExternalMethodDef method = new ExternalMethodDef(
                charAt,
                "charAt",
                new DataType[]{SystemDataType.INT},
                SystemDataType.INT,
                "(I)C",
                false
            );
            classType.addInstanceMethod(method);

            assertFalse(classType.getInstanceMethods().isEmpty());
            assertTrue(classType.getInstanceMethods().containsKey("charAt"));
        }
    }

    @Nested
    class ConstructorTests {

        @Test
        void testAddAndGetConstructor() throws Exception {
            Method valueOf = String.class.getMethod("valueOf", int.class);  // Use any Method for testing
            ExternalMethodDef ctor = new ExternalMethodDef(
                valueOf,
                "<init>",
                new DataType[]{SystemDataType.STRING},
                SystemDataType.VOID,
                "(Ljava/lang/String;)V",
                false
            );
            classType.addConstructor(ctor);

            ExternalMethodDef found = classType.getConstructor(new DataType[]{SystemDataType.STRING});
            assertNotNull(found);
        }

        @Test
        void testGetConstructor_NotFound() throws Exception {
            Method valueOf = String.class.getMethod("valueOf", int.class);
            ExternalMethodDef ctor = new ExternalMethodDef(
                valueOf,
                "<init>",
                new DataType[]{SystemDataType.STRING},
                SystemDataType.VOID,
                "(Ljava/lang/String;)V",
                false
            );
            classType.addConstructor(ctor);

            // No constructor that takes BOOLEAN
            ExternalMethodDef found = classType.getConstructor(new DataType[]{SystemDataType.BOOLEAN});
            assertNull(found);
        }

        @Test
        void testGetConstructor_WrongParamCount() throws Exception {
            Method valueOf = String.class.getMethod("valueOf", int.class);
            ExternalMethodDef ctor = new ExternalMethodDef(
                valueOf,
                "<init>",
                new DataType[]{SystemDataType.STRING},
                SystemDataType.VOID,
                "(Ljava/lang/String;)V",
                false
            );
            classType.addConstructor(ctor);

            // No args when constructor expects one
            ExternalMethodDef found = classType.getConstructor(new DataType[]{});
            assertNull(found);
        }
    }

    @Nested
    class AmbiguityTests {

        /**
         * Tests that ambiguous static method call throws exception.
         * <pre>
         * // Two methods match: process(ANY) and process(NUMERIC)
         * // Call with INT - INT is castable to both, causing ambiguity
         * classType.getStaticMethod("process", new DataType[]{INT})  // throws
         * </pre>
         */
        @Test
        void testAmbiguousStaticMethodCall() throws Exception {
            Method valueOf = String.class.getMethod("valueOf", int.class);

            // Method accepting ANY
            ExternalMethodDef method1 = new ExternalMethodDef(
                valueOf,
                "process",
                new DataType[]{SystemDataType.ANY},
                SystemDataType.STRING,
                "(Ljava/lang/Object;)Ljava/lang/String;",
                true
            );

            // Method accepting NUMERIC
            ExternalMethodDef method2 = new ExternalMethodDef(
                valueOf,
                "process",
                new DataType[]{SystemDataType.NUMERIC},
                SystemDataType.STRING,
                "(Ljava/lang/Number;)Ljava/lang/String;",
                true
            );

            classType.addStaticMethod(method1);
            classType.addStaticMethod(method2);

            // Call with INT - castable to both ANY and NUMERIC
            assertThrows(IllegalArgumentException.class, () ->
                classType.getStaticMethod("process", new DataType[]{SystemDataType.INT})
            );
        }

        /**
         * Two overloads {@code abs(int)} and {@code abs(double)} both match an INT
         * argument (INT widens to DOUBLE), but the exact-match resolver must prefer
         * the {@code abs(int)} overload rather than throwing an ambiguity error.
         */
        @Test
        void testExactMatchPreferredOverWidening() throws Exception {
            Method valueOf = String.class.getMethod("valueOf", int.class);

            ExternalMethodDef absInt = new ExternalMethodDef(
                valueOf, "abs",
                new DataType[]{SystemDataType.INT},
                SystemDataType.INT, "(I)I", true);
            ExternalMethodDef absDouble = new ExternalMethodDef(
                valueOf, "abs",
                new DataType[]{SystemDataType.DOUBLE},
                SystemDataType.DOUBLE, "(D)D", true);
            classType.addStaticMethod(absInt);
            classType.addStaticMethod(absDouble);

            // INT arg → exact match is abs(int)
            ExternalMethodDef foundInt =
                classType.getStaticMethod("abs", new DataType[]{SystemDataType.INT});
            assertNotNull(foundInt);
            assertEquals(SystemDataType.INT, foundInt.getReturnType());

            // DOUBLE arg → exact match is abs(double)
            ExternalMethodDef foundDouble =
                classType.getStaticMethod("abs", new DataType[]{SystemDataType.DOUBLE});
            assertNotNull(foundDouble);
            assertEquals(SystemDataType.DOUBLE, foundDouble.getReturnType());
        }
    }

    @Nested
    class EqualsHashCodeTests {

        @Test
        void testEquals_SameObject() {
            assertEquals(classType, classType);
        }

        @Test
        void testEquals_SameName() {
            ExternalClassType other = new ExternalClassType("TestClass", Integer.class);
            assertEquals(classType, other);
        }

        @Test
        void testEquals_DifferentName() {
            ExternalClassType other = new ExternalClassType("OtherClass", String.class);
            assertNotEquals(classType, other);
        }

        @Test
        void testEquals_Null() {
            assertNotEquals(null, classType);
        }

        @Test
        void testEquals_DifferentClass() {
            assertNotEquals("String", classType);
        }

        @Test
        void testHashCode_SameName() {
            ExternalClassType other = new ExternalClassType("TestClass", Integer.class);
            assertEquals(classType.hashCode(), other.hashCode());
        }

        @Test
        void testToString() {
            assertTrue(classType.toString().contains("TestClass"));
        }
    }
}
