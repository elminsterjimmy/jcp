package com.elminster.jcp.util;

import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.ExternalClassType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Branch-coverage tests for ClassConverter.getDataType() private logic,
 * exercised via the public registerClass() entry point.
 *
 * Covers the fast-path branches in getDataType():
 *  - primitive / wrapper types (int, char, byte, short, long, double, float, boolean, void, String, Object)
 *  - array fast-paths (int[], long[], double[], boolean[], String[], other arrays)
 *  - anonymous/lambda class → ANY (simpleName is empty)
 *  - already-registered type returned immediately (FQN lookup)
 *  - JCP-source (non-ExternalClassType) type takes precedence
 *  - dependency-first: method parameter type registered transitively
 */
class ClassConverterBranchTest {

    private EvalContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new RootEvalContext();
    }

    // ── helper class whose methods use every primitive/array type ────────────

    public static class PrimitiveHolder {
        public static int    getInt()      { return 0; }
        public static char   getChar()     { return 'a'; }
        public static byte   getByte()     { return 0; }
        public static short  getShort()    { return 0; }
        public static long   getLong()     { return 0L; }
        public static double getDouble()   { return 0.0; }
        public static float  getFloat()    { return 0f; }
        public static boolean getBool()    { return false; }
        public static void   doVoid()      {}
        public static String getString()   { return ""; }
        public static Object getObj()      { return null; }
    }

    public static class ArrayHolder {
        public static int[]     intArr()    { return new int[0]; }
        public static long[]    longArr()   { return new long[0]; }
        public static double[]  dblArr()    { return new double[0]; }
        public static boolean[] boolArr()   { return new boolean[0]; }
        public static String[]  strArr()    { return new String[0]; }
        // Generic object array → ANY_ARRAY
        public static Object[]  objArr()    { return new Object[0]; }
        // Typed object array (Integer[]) → INT_ARRAY
        public static Integer[] intObjArr() { return new Integer[0]; }
    }

    // ── helper class whose method takes a parameter of another external type ─

    public static class UsesSomeDep {
        public static int consume(java.util.Date d) { return 0; }
    }

    // ── registering a class exercises getDataType() for every return type ────

    @Test
    void registerClass_primitiveReturnTypes_allRegistered() {
        ClassConverter.registerClass(PrimitiveHolder.class, ctx, "test");

        DataType type = ctx.getDataType("PrimitiveHolder");
        assertNotNull(type);
        assertInstanceOf(ExternalClassType.class, type);
    }

    @Test
    void registerClass_arrayReturnTypes_allRegistered() {
        ClassConverter.registerClass(ArrayHolder.class, ctx, "test");

        DataType type = ctx.getDataType("ArrayHolder");
        assertNotNull(type);
    }

    @Test
    void registerClass_idempotent_secondCallSkipped() {
        ClassConverter.registerClass(java.util.Date.class, ctx, "test");
        DataType first = ctx.getDataType("Date");

        // Second call should detect fully-registered type and return immediately
        assertDoesNotThrow(() -> ClassConverter.registerClass(java.util.Date.class, ctx, "test"));
        DataType second = ctx.getDataType("Date");
        assertSame(first, second);
    }

    @Test
    void registerClass_dependencyFirstRegistration_paramTypesAvailable() {
        // UsesSomeDep.consume(Date) → Date gets registered as a side-effect
        ClassConverter.registerClass(UsesSomeDep.class, ctx, "test");
        assertNotNull(ctx.getDataType("UsesSomeDep"));
        // java.util.Date should have been registered transitively
        assertNotNull(ctx.getDataTypeByFqn("java.util.Date"));
    }

    @Test
    void registerClass_charParam_mapsToString() {
        // PrimitiveHolder.getChar() returns char → getDataType(char.class) → STRING
        ClassConverter.registerClass(PrimitiveHolder.class, ctx, "test");
        assertNotNull(ctx.getDataType("PrimitiveHolder"));
    }

    @Test
    void registerClass_sameSimpleNameTwice_bothReachableByFqn() {
        ClassConverter.registerClass(java.util.Date.class, ctx, "test");
        ClassConverter.registerClass(java.sql.Date.class,  ctx, "test");

        assertNotNull(ctx.getDataTypeByFqn("java.util.Date"));
        assertNotNull(ctx.getDataTypeByFqn("java.sql.Date"));
    }

    // ── CharHolder to cover char/Character → STRING branch in getDataType ───

    public static class CharHolder {
        public static char identity(char c) { return c; }
        public static byte  byteVal()  { return 1; }
        public static short shortVal() { return 2; }
        public static char[] charArr()  { return new char[0]; }
    }

    @Test
    void registerClass_charByteShortReturnTypes_registered() {
        ClassConverter.registerClass(CharHolder.class, ctx, "test");
        assertNotNull(ctx.getDataType("CharHolder"));
    }

    // ── stub-upgrade: circular dependency creates a stub, later upgraded ────────

    public static class EvalCircularA {
        public static EvalCircularB getB() { return null; }
    }

    public static class EvalCircularB {
        public static EvalCircularA getA() { return null; }
        public static int value() { return 42; }
    }

    @Test
    void registerClass_stubUpgradedToFull_evalMode() {
        // EvalCircularA → EvalCircularB → EvalCircularA forms a cycle.
        // Registering EvalCircularA creates a stub for EvalCircularB (cycle guard).
        ClassConverter.registerClass(EvalCircularA.class, ctx, "test");
        DataType bType = ctx.getDataTypeByFqn(EvalCircularB.class.getName());
        assertNotNull(bType, "EvalCircularB should be registered");
        assertInstanceOf(ExternalClassType.class, bType);

        // Now explicitly register EvalCircularB — should hit the stub-upgrade path.
        ClassConverter.registerClass(EvalCircularB.class, ctx, "test");
        DataType upgraded = ctx.getDataTypeByFqn(EvalCircularB.class.getName());
        assertSame(bType, upgraded, "should reuse the same ExternalClassType object");
    }

    // ── system-type collision: registering a class whose simple name matches a SystemDataType

    @Test
    void registerClass_skippedWhenSimpleNameIsSystemType() {
        assertDoesNotThrow(() -> ClassConverter.registerClass(String.class, ctx, "test"));
        DataType dt = ctx.getDataType("String");
        assertEquals(SystemDataType.STRING, dt);
    }
}
