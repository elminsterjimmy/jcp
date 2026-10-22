package com.elminster.jcp.compile.util;

import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.ExternalClassType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Branch-coverage tests for CompileModeClassConverter.mapJavaTypeToDataType().
 *
 * Covers branches missed by the existing test: char, byte, short, long, float,
 * Integer[], Double[], Boolean[], Object[], anonymous class, and the
 * context-aware overloads (already-registered FQN, stub-upgrade, primitive/array
 * early returns).
 */
class CompileModeClassConverterBranchTest {

    private CompileContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new CompileContext();
        ctx.setClassName("TestClass");
    }

    // ── context-free overload: remaining primitive/wrapper branches ───────────

    @Test
    void mapJavaType_charPrimitive_returnsString() {
        assertEquals(SystemDataType.STRING, CompileModeClassConverter.mapJavaTypeToDataType(char.class));
    }

    @Test
    void mapJavaType_charWrapper_returnsString() {
        assertEquals(SystemDataType.STRING, CompileModeClassConverter.mapJavaTypeToDataType(Character.class));
    }

    @Test
    void mapJavaType_bytePrimitive_returnsInt() {
        assertEquals(SystemDataType.INT, CompileModeClassConverter.mapJavaTypeToDataType(byte.class));
    }

    @Test
    void mapJavaType_byteWrapper_returnsInt() {
        assertEquals(SystemDataType.INT, CompileModeClassConverter.mapJavaTypeToDataType(Byte.class));
    }

    @Test
    void mapJavaType_shortPrimitive_returnsInt() {
        assertEquals(SystemDataType.INT, CompileModeClassConverter.mapJavaTypeToDataType(short.class));
    }

    @Test
    void mapJavaType_shortWrapper_returnsInt() {
        assertEquals(SystemDataType.INT, CompileModeClassConverter.mapJavaTypeToDataType(Short.class));
    }

    @Test
    void mapJavaType_longPrimitive_returnsLong() {
        assertEquals(SystemDataType.LONG, CompileModeClassConverter.mapJavaTypeToDataType(long.class));
    }

    @Test
    void mapJavaType_longWrapper_returnsLong() {
        assertEquals(SystemDataType.LONG, CompileModeClassConverter.mapJavaTypeToDataType(Long.class));
    }

    @Test
    void mapJavaType_floatPrimitive_returnsDouble() {
        assertEquals(SystemDataType.DOUBLE, CompileModeClassConverter.mapJavaTypeToDataType(float.class));
    }

    @Test
    void mapJavaType_floatWrapper_returnsDouble() {
        assertEquals(SystemDataType.DOUBLE, CompileModeClassConverter.mapJavaTypeToDataType(Float.class));
    }

    @Test
    void mapJavaType_charArray_returnsStringArray() {
        assertEquals(SystemDataType.STRING_ARRAY, CompileModeClassConverter.mapJavaTypeToDataType(char[].class));
    }

    @Test
    void mapJavaType_charSequence_returnsString() {
        assertEquals(SystemDataType.STRING, CompileModeClassConverter.mapJavaTypeToDataType(CharSequence.class));
    }

    @Test
    void mapJavaType_byteArray_returnsIntArray() {
        assertEquals(SystemDataType.INT_ARRAY, CompileModeClassConverter.mapJavaTypeToDataType(byte[].class));
    }

    @Test
    void mapJavaType_shortArray_returnsIntArray() {
        assertEquals(SystemDataType.INT_ARRAY, CompileModeClassConverter.mapJavaTypeToDataType(short[].class));
    }

    @Test
    void mapJavaType_longArray_returnsLongArray() {
        assertEquals(SystemDataType.LONG_ARRAY, CompileModeClassConverter.mapJavaTypeToDataType(long[].class));
    }

    @Test
    void mapJavaType_floatArray_returnsDoubleArray() {
        assertEquals(SystemDataType.DOUBLE_ARRAY, CompileModeClassConverter.mapJavaTypeToDataType(float[].class));
    }

    @Test
    void mapJavaType_integerBoxedArray_returnsIntArray() {
        assertEquals(SystemDataType.INT_ARRAY, CompileModeClassConverter.mapJavaTypeToDataType(Integer[].class));
    }

    @Test
    void mapJavaType_doubleBoxedArray_returnsDoubleArray() {
        assertEquals(SystemDataType.DOUBLE_ARRAY, CompileModeClassConverter.mapJavaTypeToDataType(Double[].class));
    }

    @Test
    void mapJavaType_booleanBoxedArray_returnsBooleanArray() {
        assertEquals(SystemDataType.BOOLEAN_ARRAY, CompileModeClassConverter.mapJavaTypeToDataType(Boolean[].class));
    }

    @Test
    void mapJavaType_objectArray_returnsAnyArray() {
        assertEquals(SystemDataType.ANY_ARRAY, CompileModeClassConverter.mapJavaTypeToDataType(Object[].class));
    }

    // ── context-aware overload: primitive is returned without registration ────

    @Test
    void mapJavaTypeWithCtx_int_returnsIntWithoutRegistering() {
        DataType type = CompileModeClassConverter.mapJavaTypeToDataType(int.class, ctx, "test");
        assertEquals(SystemDataType.INT, type);
        // Nothing registered in context
        assertNull(ctx.getDataTypeByFqn("int"));
    }

    @Test
    void mapJavaTypeWithCtx_intArray_returnsIntArrayWithoutRegistering() {
        DataType type = CompileModeClassConverter.mapJavaTypeToDataType(int[].class, ctx, "test");
        assertEquals(SystemDataType.INT_ARRAY, type);
    }

    // ── Object.class → ANY (no stub) ─────────────────────────────────────────

    @Test
    void mapJavaTypeWithCtx_objectClass_returnsAny() {
        DataType type = CompileModeClassConverter.mapJavaTypeToDataType(Object.class, ctx, "test");
        assertEquals(SystemDataType.ANY, type);
    }

    // ── already-registered type returned from FQN cache ──────────────────────

    @Test
    void mapJavaTypeWithCtx_alreadyRegistered_returnsCachedType() {
        DataType first = CompileModeClassConverter.mapJavaTypeToDataType(java.util.Date.class, ctx, "test");
        DataType second = CompileModeClassConverter.mapJavaTypeToDataType(java.util.Date.class, ctx, "test");
        assertSame(first, second);
    }

    // ── transitive dependency registration ────────────────────────────────────

    public static class UsesList {
        public static int size(java.util.ArrayList<?> list) { return list.size(); }
    }

    @Test
    void registerClass_transitiveParam_getsRegisteredAsStub() {
        CompileModeClassConverter.registerClass(UsesList.class, ctx, "test");
        // ArrayList should have been registered as a dependency
        assertNotNull(ctx.getDataTypeByFqn("java.util.ArrayList"));
    }

    @Test
    void registerClass_stubUpgradedToFullRegistration() {
        // CircularA → CircularB → CircularA forms a cycle.
        // Registering CircularA creates a stub for CircularB (cycle guard prevents full registration).
        CompileModeClassConverter.registerClass(CircularA.class, ctx, "test");
        ExternalClassType bType = (ExternalClassType) ctx.getDataTypeByFqn(CircularB.class.getName());
        assertNotNull(bType, "CircularB should be registered");

        // Now explicitly register CircularB — should upgrade the stub in-place.
        CompileModeClassConverter.registerClass(CircularB.class, ctx, "test");
        ExternalClassType upgraded = (ExternalClassType) ctx.getDataTypeByFqn(CircularB.class.getName());
        assertSame(bType, upgraded, "should reuse the same ExternalClassType object");
        assertFalse(upgraded.getStaticMethods().isEmpty(), "stub should now have methods");
    }

    @Test
    void registerClass_alreadyFullyRegistered_skipsSecondTime() {
        CompileModeClassConverter.registerClass(CircularA.class, ctx, "test");
        ExternalClassType aType = (ExternalClassType) ctx.getDataTypeByFqn(CircularA.class.getName());
        assertNotNull(aType);
        assertFalse(aType.getStaticMethods().isEmpty(), "should have static methods");

        // Second registration should hit the "already fully registered" early return
        CompileModeClassConverter.registerClass(CircularA.class, ctx, "test");
        ExternalClassType aType2 = (ExternalClassType) ctx.getDataTypeByFqn(CircularA.class.getName());
        assertSame(aType, aType2);
    }

    @Test
    void registerClass_anonymousClass_skippedOrAny() {
        // Anonymous classes have an empty simpleName
        Runnable anon = new Runnable() {
            @Override
            public void run() {}
        };
        CompileModeClassConverter.registerClass(anon.getClass(), ctx, "test");
    }

    public static class HasInstanceMethods {
        public int getValue() { return 42; }
    }

    @Test
    void registerClass_alreadyFullyRegisteredWithInstanceMethods_skipsSecondTime() {
        CompileModeClassConverter.registerClass(HasInstanceMethods.class, ctx, "test");
        ExternalClassType type = (ExternalClassType) ctx.getDataTypeByFqn(HasInstanceMethods.class.getName());
        assertNotNull(type);
        assertFalse(type.getInstanceMethods().isEmpty(), "should have instance methods");

        // Second registration should hit !getInstanceMethods().isEmpty() → true → early return
        CompileModeClassConverter.registerClass(HasInstanceMethods.class, ctx, "test");
        ExternalClassType type2 = (ExternalClassType) ctx.getDataTypeByFqn(HasInstanceMethods.class.getName());
        assertSame(type, type2);
    }

    public static class CircularA {
        public static CircularB getB() { return null; }
    }

    public static class CircularB {
        public static CircularA getA() { return null; }
        public static int value() { return 42; }
    }
}
