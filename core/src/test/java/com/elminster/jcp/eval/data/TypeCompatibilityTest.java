package com.elminster.jcp.eval.data;

import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.util.CompileModeClassConverter;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.module.JcpModuleRegistry;
import com.elminster.jcp.util.ClassConverter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TypeCompatibilityTest {

    // -- IAdaptable contract --------------------------------------------------

    @Nested
    class IAdaptableTests {

        @Test
        void defaultGetCompatibleJavaTypes_returnsEmptySet() {
            IAdaptable adaptable = new IAdaptable() {};
            assertTrue(adaptable.getCompatibleJavaTypes().isEmpty());
        }

        @Test
        void defaultIsAdaptableTo_objectAlwaysTrue() {
            IAdaptable adaptable = new IAdaptable() {};
            assertTrue(adaptable.isAdaptableTo(Object.class));
        }

        @Test
        void defaultIsAdaptableTo_nonObjectReturnsFalse() {
            IAdaptable adaptable = new IAdaptable() {};
            assertFalse(adaptable.isAdaptableTo(String.class));
        }

        @Test
        void isAdaptableTo_checksAssignability() {
            IAdaptable adaptable = new IAdaptable() {
                @Override
                public Set<Class<?>> getCompatibleJavaTypes() {
                    return Set.of(String.class);
                }
            };
            assertTrue(adaptable.isAdaptableTo(CharSequence.class));
            assertTrue(adaptable.isAdaptableTo(Comparable.class));
            assertFalse(adaptable.isAdaptableTo(Integer.class));
        }
    }

    // -- ICastable contract ---------------------------------------------------

    @Nested
    class ICastableTests {

        @Test
        void systemDataType_intCastableToNumeric() {
            assertTrue(SystemDataType.INT.isCastableTo(SystemDataType.NUMERIC));
        }

        @Test
        void systemDataType_intCastableToAny() {
            assertTrue(SystemDataType.INT.isCastableTo(SystemDataType.ANY));
        }

        @Test
        void systemDataType_stringNotCastableToInt() {
            assertFalse(SystemDataType.STRING.isCastableTo(SystemDataType.INT));
        }
    }

    // -- SystemDataType built-in compatibility --------------------------------

    @Nested
    class SystemDataTypeAdaptableTests {

        @Test
        void string_adaptableToCharSequence() {
            assertTrue(SystemDataType.STRING.isAdaptableTo(CharSequence.class));
        }

        @Test
        void string_adaptableToComparable() {
            assertTrue(SystemDataType.STRING.isAdaptableTo(Comparable.class));
        }

        @Test
        void string_adaptableToStringClass() {
            assertTrue(SystemDataType.STRING.isAdaptableTo(String.class));
        }

        @Test
        void int_adaptableToIntPrimitive() {
            assertTrue(SystemDataType.INT.isAdaptableTo(int.class));
        }

        @Test
        void int_adaptableToIntegerBoxed() {
            assertTrue(SystemDataType.INT.isAdaptableTo(Integer.class));
        }

        @Test
        void double_adaptableToDoublePrimitive() {
            assertTrue(SystemDataType.DOUBLE.isAdaptableTo(double.class));
        }

        @Test
        void double_adaptableToFloatPrimitive() {
            assertTrue(SystemDataType.DOUBLE.isAdaptableTo(float.class));
        }

        @Test
        void boolean_adaptableToBooleanPrimitive() {
            assertTrue(SystemDataType.BOOLEAN.isAdaptableTo(boolean.class));
        }

        @Test
        void any_adaptableToAnything() {
            assertTrue(SystemDataType.ANY.isAdaptableTo(String.class));
            assertTrue(SystemDataType.ANY.isAdaptableTo(int.class));
            assertTrue(SystemDataType.ANY.isAdaptableTo(java.util.List.class));
        }

        @Test
        void numeric_emptyCompatibleTypes() {
            assertTrue(SystemDataType.NUMERIC.getCompatibleJavaTypes().isEmpty());
        }

        @Test
        void void_adaptableToVoidPrimitive() {
            assertTrue(SystemDataType.VOID.isAdaptableTo(void.class));
        }
    }

    // -- ExternalClassType IAdaptable -----------------------------------------

    @Nested
    class ExternalClassTypeAdaptableTests {

        @Test
        void defaultCompatibleTypes_includesJavaClass() {
            ExternalClassType ext = new ExternalClassType("MyList", java.util.ArrayList.class);
            assertTrue(ext.isAdaptableTo(java.util.List.class));
            assertTrue(ext.isAdaptableTo(java.util.ArrayList.class));
            assertTrue(ext.isAdaptableTo(Collection.class));
        }

        @Test
        void addCompatibleJavaType_extendsCompatibility() {
            ExternalClassType ext = new ExternalClassType("Wrapper", SampleWrapper.class);
            assertFalse(ext.isAdaptableTo(Comparable.class));

            ext.addCompatibleJavaType(Comparable.class);

            assertTrue(ext.isAdaptableTo(Comparable.class));
            assertTrue(ext.isAdaptableTo(SampleWrapper.class));
        }

        @Test
        void getCompatibleJavaTypes_withNoAdditional_returnsSingleton() {
            ExternalClassType ext = new ExternalClassType("Str", String.class);
            Set<Class<?>> types = ext.getCompatibleJavaTypes();
            assertEquals(1, types.size());
            assertTrue(types.contains(String.class));
        }

        @Test
        void getCompatibleJavaTypes_withAdditional_returnsUnion() {
            ExternalClassType ext = new ExternalClassType("Wrapper", SampleWrapper.class);
            ext.addCompatibleJavaType(Serializable.class);
            ext.addCompatibleJavaType(Comparable.class);

            Set<Class<?>> types = ext.getCompatibleJavaTypes();
            assertEquals(3, types.size());
            assertTrue(types.contains(SampleWrapper.class));
            assertTrue(types.contains(Serializable.class));
            assertTrue(types.contains(Comparable.class));
        }
    }

    // -- TypeCompatibilityUtil ------------------------------------------------

    @Nested
    class TypeCompatibilityUtilTests {

        @Test
        void findJcpTypeForJava_stringClass_returnsSTRING() {
            assertEquals(SystemDataType.STRING, TypeCompatibilityUtil.findJcpTypeForJava(String.class));
        }

        @Test
        void findJcpTypeForJava_intClass_returnsINT() {
            assertEquals(SystemDataType.INT, TypeCompatibilityUtil.findJcpTypeForJava(int.class));
        }

        @Test
        void findJcpTypeForJava_unknownClass_returnsNull() {
            assertNull(TypeCompatibilityUtil.findJcpTypeForJava(SampleWrapper.class));
        }

        @Test
        void findJcpTypeForJava_withRegisteredTypes_findsExternal() {
            ExternalClassType ext = new ExternalClassType("Wrapper", SampleWrapper.class);
            DataType result = TypeCompatibilityUtil.findJcpTypeForJava(
                    SampleWrapper.class, Collections.singletonList(ext));
            assertSame(ext, result);
        }

        @Test
        void systemDataType_hasPriorityOverExternal() {
            ExternalClassType ext = new ExternalClassType("MyString", String.class);
            DataType result = TypeCompatibilityUtil.findJcpTypeForJava(
                    String.class, Collections.singletonList(ext));
            assertEquals(SystemDataType.STRING, result);
        }

        @Test
        void skipsSystemDataType_inRegisteredCollection() {
            DataType result = TypeCompatibilityUtil.findJcpTypeForJava(
                    SampleWrapper.class, Collections.singletonList(SystemDataType.ANY));
            assertNull(result);
        }
    }

    // -- StructType / DataTypeImpl default interface inheritance ---------------

    @Nested
    class DefaultInheritanceTests {

        @Test
        void structType_defaultCompatibleTypes_empty() {
            StructType struct = new StructType("Point", Collections.emptyList());
            assertTrue(struct.getCompatibleJavaTypes().isEmpty());
        }

        @Test
        void structType_defaultIsAdaptableTo_objectOnly() {
            StructType struct = new StructType("Point", Collections.emptyList());
            assertTrue(struct.isAdaptableTo(Object.class));
            assertFalse(struct.isAdaptableTo(String.class));
        }

        @Test
        void dataTypeImpl_defaultCompatibleTypes_empty() {
            DataTypeImpl impl = new DataTypeImpl("Custom");
            assertTrue(impl.getCompatibleJavaTypes().isEmpty());
        }

        @Test
        void dataTypeImpl_defaultIsAdaptableTo_objectOnly() {
            DataTypeImpl impl = new DataTypeImpl("Custom");
            assertTrue(impl.isAdaptableTo(Object.class));
            assertFalse(impl.isAdaptableTo(Comparable.class));
        }
    }

    // -- Context accessors ----------------------------------------------------

    @Nested
    class ContextAccessorTests {

        @Test
        void evalContext_getAllRegisteredTypes_includesRegisteredTypes() {
            RootEvalContext ctx = new RootEvalContext();
            ClassConverter.registerClass(SampleWrapper.class, ctx, "test");
            Collection<DataType> all = ctx.getAllRegisteredTypes();
            boolean found = all.stream()
                    .anyMatch(dt -> dt instanceof ExternalClassType
                            && ((ExternalClassType) dt).getJavaClass() == SampleWrapper.class);
            assertTrue(found);
        }

        @Test
        void compileContext_getAllRegisteredTypes_includesRegisteredTypes() {
            CompileContext ctx = new CompileContext();
            ctx.setClassName("TestClass");
            CompileModeClassConverter.registerClass(SampleWrapper.class, ctx, "test");
            Collection<DataType> all = ctx.getAllRegisteredTypes();
            boolean found = all.stream()
                    .anyMatch(dt -> dt instanceof ExternalClassType
                            && ((ExternalClassType) dt).getJavaClass() == SampleWrapper.class);
            assertTrue(found);
        }
    }

    // -- JcpModuleRegistry customizer end-to-end ------------------------------

    @Nested
    class RegistryCustomizerTests {

        @Test
        void customizer_appliedInEvalMode() {
            JcpModuleRegistry registry = new JcpModuleRegistry();
            registry.registerClass(SampleWrapper.class, "test", type -> {
                type.addCompatibleJavaType(Comparable.class);
                type.addCompatibleJavaType(Serializable.class);
            });

            EvalContext ctx = new RootEvalContext();
            registry.initEval(ctx);

            DataType dt = ctx.getDataType("SampleWrapper");
            assertInstanceOf(ExternalClassType.class, dt);
            ExternalClassType ext = (ExternalClassType) dt;
            assertTrue(ext.isAdaptableTo(Comparable.class));
            assertTrue(ext.isAdaptableTo(Serializable.class));
            assertTrue(ext.isAdaptableTo(SampleWrapper.class));
        }

        @Test
        void customizer_appliedInCompileMode() {
            JcpModuleRegistry registry = new JcpModuleRegistry();
            registry.registerClass(SampleWrapper.class, "test", type -> {
                type.addCompatibleJavaType(Comparable.class);
            });

            CompileContext ctx = new CompileContext();
            ctx.setClassName("TestClass");
            registry.initCompile(ctx);

            DataType dt = ctx.getDataType("SampleWrapper");
            assertInstanceOf(ExternalClassType.class, dt);
            assertTrue(dt.isAdaptableTo(Comparable.class));
        }

        @Test
        void noCustomizer_typeRegisteredNormally() {
            JcpModuleRegistry registry = new JcpModuleRegistry();
            registry.registerClass(SampleWrapper.class, "test");

            EvalContext ctx = new RootEvalContext();
            registry.initEval(ctx);

            DataType dt = ctx.getDataType("SampleWrapper");
            assertInstanceOf(ExternalClassType.class, dt);
            assertFalse(dt.isAdaptableTo(Comparable.class));
        }
    }

    // -- Backward compatibility: unknown types still get opaque stubs ---------

    @Nested
    class BackwardCompatibilityTests {

        @Test
        void unknownType_getsOpaqueStub_evalMode() {
            RootEvalContext ctx = new RootEvalContext();
            ClassConverter.registerClass(SampleWrapper.class, ctx, "test");

            DataType dt = ctx.getDataType("SampleWrapper");
            assertInstanceOf(ExternalClassType.class, dt);
            assertEquals(SampleWrapper.class, ((ExternalClassType) dt).getJavaClass());
        }

        @Test
        void unknownType_getsOpaqueStub_compileMode() {
            CompileContext ctx = new CompileContext();
            ctx.setClassName("TestClass");
            CompileModeClassConverter.registerClass(SampleWrapper.class, ctx, "test");

            DataType dt = ctx.getDataType("SampleWrapper");
            assertInstanceOf(ExternalClassType.class, dt);
            assertEquals(SampleWrapper.class, ((ExternalClassType) dt).getJavaClass());
        }
    }

    // -- Test helper classes ---------------------------------------------------

    public static class SampleWrapper {
        private final String value;

        public SampleWrapper(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static SampleWrapper of(String value) {
            return new SampleWrapper(value);
        }
    }
}
