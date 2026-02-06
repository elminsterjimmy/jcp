package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.MethodDef;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StructType.
 */
class StructTypeTest {

    private List<StructFieldDef> testFields;
    private StructType basicStruct;

    @BeforeEach
    void setUp() {
        testFields = Arrays.asList(
            new StructFieldDef("x", SystemDataType.INT),
            new StructFieldDef("y", SystemDataType.INT)
        );
        basicStruct = new StructType("Point", testFields);
    }

    @Nested
    class BasicStructTests {

        @Test
        void testGetName() {
            assertEquals("Point", basicStruct.getName());
        }

        @Test
        void testGetParent() {
            assertEquals(SystemDataType.ANY, basicStruct.getParent());
        }

        @Test
        void testGetFields() {
            assertEquals(2, basicStruct.getFields().size());
        }

        @Test
        void testGetField_Exists() {
            StructFieldDef field = basicStruct.getField("x");
            assertNotNull(field);
            assertEquals("x", field.getName().getId());
        }

        @Test
        void testGetField_NotExists() {
            StructFieldDef field = basicStruct.getField("z");
            assertNull(field);
        }

        @Test
        void testGetConstructor_None() {
            assertNull(basicStruct.getConstructor());
        }
    }

    @Nested
    class ConstructorTests {

        @Test
        void testStructWithConstructor() {
            Block constructorBody = new BlockImpl();
            MethodDef constructor = MethodDef.constructor(
                constructorBody,
                ParameterDef.of("x", SystemDataType.INT),
                ParameterDef.of("y", SystemDataType.INT)
            );

            StructType structWithCtor = new StructType("Point", testFields, constructor);
            assertNotNull(structWithCtor.getConstructor());
        }
    }

    @Nested
    class InstanceMethodTests {

        private StructType structWithMethods;
        private MethodDef getXMethod;
        private MethodDef setXMethod;

        @BeforeEach
        void setUp() {
            Block methodBody = new BlockImpl();

            // Instance method: getX() -> Int
            getXMethod = new MethodDef("getX", SystemDataType.INT, methodBody);

            // Instance method: setX(value: Int) -> Void
            setXMethod = new MethodDef("setX", SystemDataType.VOID, methodBody,
                ParameterDef.of("value", SystemDataType.INT));

            structWithMethods = new StructType(
                "Point",
                testFields,
                null,  // no explicit constructor
                Arrays.asList(getXMethod, setXMethod),  // instance methods
                Collections.emptyList()  // no static methods
            );
        }

        @Test
        void testGetInstanceMethod_NoArgs() {
            MethodDef method = structWithMethods.getInstanceMethod("getX", new DataType[]{});
            assertNotNull(method);
            assertEquals("getX", method.getId().getId());
        }

        @Test
        void testGetInstanceMethod_WithArgs() {
            MethodDef method = structWithMethods.getInstanceMethod("setX", new DataType[]{SystemDataType.INT});
            assertNotNull(method);
            assertEquals("setX", method.getId().getId());
        }

        @Test
        void testGetInstanceMethod_NotFound() {
            MethodDef method = structWithMethods.getInstanceMethod("nonExistent", new DataType[]{});
            assertNull(method);
        }

        @Test
        void testGetInstanceMethod_WrongArgCount() {
            MethodDef method = structWithMethods.getInstanceMethod("setX", new DataType[]{});
            assertNull(method);
        }

        @Test
        void testGetInstanceMethod_WrongArgType() {
            // setX expects Int, but we pass String
            MethodDef method = structWithMethods.getInstanceMethod("setX", new DataType[]{SystemDataType.STRING});
            assertNull(method);
        }

        @Test
        void testGetInstanceMethod_CompatibleArgType() {
            // Test with INT which is definitely compatible with INT parameter
            MethodDef method = structWithMethods.getInstanceMethod("setX", new DataType[]{SystemDataType.INT});
            assertNotNull(method);
            assertEquals("setX", method.getId().getId());
        }

        @Test
        void testGetInstanceMethods() {
            assertFalse(structWithMethods.getInstanceMethods().isEmpty());
            assertEquals(2, structWithMethods.getInstanceMethods().size());
        }
    }

    @Nested
    class StaticMethodTests {

        private StructType structWithStaticMethods;
        private MethodDef createMethod;

        @BeforeEach
        void setUp() {
            Block methodBody = new BlockImpl();

            // Static method: create(x: Int, y: Int) -> Point
            createMethod = MethodDef.staticMethod("create", new DataTypeImpl("Point"), methodBody,
                ParameterDef.of("x", SystemDataType.INT),
                ParameterDef.of("y", SystemDataType.INT)
            );

            structWithStaticMethods = new StructType(
                "Point",
                testFields,
                null,  // no explicit constructor
                Collections.emptyList(),  // no instance methods
                Arrays.asList(createMethod)  // static methods
            );
        }

        @Test
        void testGetStaticMethod_Found() {
            MethodDef method = structWithStaticMethods.getStaticMethod("create",
                new DataType[]{SystemDataType.INT, SystemDataType.INT});
            assertNotNull(method);
            assertEquals("create", method.getId().getId());
        }

        @Test
        void testGetStaticMethod_NotFound() {
            MethodDef method = structWithStaticMethods.getStaticMethod("nonExistent", new DataType[]{});
            assertNull(method);
        }

        @Test
        void testGetStaticMethods() {
            assertFalse(structWithStaticMethods.getStaticMethods().isEmpty());
            assertEquals(1, structWithStaticMethods.getStaticMethods().size());
        }
    }

    @Nested
    class MethodOverloadTests {

        private StructType structWithOverloads;

        @BeforeEach
        void setUp() {
            Block methodBody = new BlockImpl();

            // Overloaded methods: add(Int) and add(Int, Int)
            MethodDef addOne = new MethodDef("add", SystemDataType.INT, methodBody,
                ParameterDef.of("value", SystemDataType.INT));

            MethodDef addTwo = new MethodDef("add", SystemDataType.INT, methodBody,
                ParameterDef.of("a", SystemDataType.INT),
                ParameterDef.of("b", SystemDataType.INT));

            structWithOverloads = new StructType(
                "Calculator",
                Collections.emptyList(),
                null,
                Arrays.asList(addOne, addTwo),
                Collections.emptyList()
            );
        }

        @Test
        void testOverloadResolution_OneArg() {
            MethodDef method = structWithOverloads.getInstanceMethod("add", new DataType[]{SystemDataType.INT});
            assertNotNull(method);
            assertEquals(1, method.getParameters().length);
        }

        @Test
        void testOverloadResolution_TwoArgs() {
            MethodDef method = structWithOverloads.getInstanceMethod("add",
                new DataType[]{SystemDataType.INT, SystemDataType.INT});
            assertNotNull(method);
            assertEquals(2, method.getParameters().length);
        }
    }

    @Nested
    class AmbiguityTests {

        @Test
        void testAmbiguousMethodCall() {
            Block methodBody = new BlockImpl();

            // Two methods that both accept ANY as parameter but with different param type names
            // This way they have different signatures in the cache, but both match when
            // we call with INT (since INT.isCastableTo(ANY) is true)
            MethodDef method1 = new MethodDef("process", SystemDataType.ANY, methodBody,
                ParameterDef.of("x", SystemDataType.ANY));

            MethodDef method2 = new MethodDef("process", SystemDataType.ANY, methodBody,
                ParameterDef.of("x", SystemDataType.NUMERIC)); // Different param type, creates different signature

            StructType structWithAmbiguity = new StructType(
                "Ambiguous",
                Collections.emptyList(),
                null,
                Arrays.asList(method1, method2),
                Collections.emptyList()
            );

            // Call with INT - INT is castable to both ANY and NUMERIC, so both match
            assertThrows(IllegalArgumentException.class, () ->
                structWithAmbiguity.getInstanceMethod("process", new DataType[]{SystemDataType.INT})
            );
        }
    }

    @Nested
    class SignatureGenerationTests {

        @Test
        void testGenerateMethodSignature_NoParams() {
            String sig = StructType.generateMethodSignature("getX", new DataType[]{});
            assertEquals("getX#", sig);
        }

        @Test
        void testGenerateMethodSignature_OneParam() {
            String sig = StructType.generateMethodSignature("setX", new DataType[]{SystemDataType.INT});
            assertEquals("setX#Integer", sig);
        }

        @Test
        void testGenerateMethodSignature_MultipleParams() {
            String sig = StructType.generateMethodSignature("calculate",
                new DataType[]{SystemDataType.INT, SystemDataType.DOUBLE, SystemDataType.STRING});
            assertEquals("calculate#Integer@Double@String", sig);
        }
    }

    @Nested
    class EqualsHashCodeTests {

        @Test
        void testEquals_SameObject() {
            assertEquals(basicStruct, basicStruct);
        }

        @Test
        void testEquals_SameName() {
            StructType other = new StructType("Point", Collections.emptyList());
            assertEquals(basicStruct, other);
        }

        @Test
        void testEquals_DifferentName() {
            StructType other = new StructType("Size", testFields);
            assertNotEquals(basicStruct, other);
        }

        @Test
        void testEquals_Null() {
            assertNotEquals(null, basicStruct);
        }

        @Test
        void testEquals_DifferentClass() {
            assertNotEquals("Point", basicStruct);
        }

        @Test
        void testHashCode_SameName() {
            StructType other = new StructType("Point", Collections.emptyList());
            assertEquals(basicStruct.hashCode(), other.hashCode());
        }

        @Test
        void testToString() {
            assertTrue(basicStruct.toString().contains("Point"));
        }
    }
}
