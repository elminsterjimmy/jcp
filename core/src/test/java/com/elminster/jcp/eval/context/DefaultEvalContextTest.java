package com.elminster.jcp.eval.context;

import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.ExternalClassType;
import com.elminster.jcp.eval.excpetion.EvaluationException;
import com.elminster.jcp.util.ClassConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Branch-coverage tests for DefaultEvalContext.
 *
 * Covers:
 *  - convertSystemDataTypeName(): each alias (Object, int, double, boolean, void, unknown)
 *  - getDataType() with module::SimpleName syntax (found and not-found)
 *  - getCallStack() throws UnsupportedOperationException
 */
class DefaultEvalContextTest {

    private RootEvalContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new RootEvalContext();
    }

    // ── convertSystemDataTypeName aliases ─────────────────────────────────────

    @Test
    void getDataType_Object_mapsToAny() {
        DataType type = ctx.getDataType("Object");
        assertEquals(SystemDataType.ANY, type);
    }

    @Test
    void getDataType_intAlias_mapsToInt() {
        DataType type = ctx.getDataType("int");
        assertEquals(SystemDataType.INT, type);
    }

    @Test
    void getDataType_doubleAlias_mapsToDouble() {
        DataType type = ctx.getDataType("double");
        assertEquals(SystemDataType.DOUBLE, type);
    }

    @Test
    void getDataType_booleanAlias_mapsToBoolean() {
        DataType type = ctx.getDataType("boolean");
        assertEquals(SystemDataType.BOOLEAN, type);
    }

    @Test
    void getDataType_voidAlias_mapsToVoid() {
        DataType type = ctx.getDataType("void");
        assertEquals(SystemDataType.VOID, type);
    }

    @Test
    void getDataType_unknownName_returnsNull() {
        // Non-alias names are looked up as-is; unknown names return null (not exception)
        assertNull(ctx.getDataType("NonExistentType"));
    }

    // ── module::SimpleName disambiguation ────────────────────────────────────

    @Test
    void getDataType_moduleQualified_returnsCorrectType() {
        // Register java.util.Date under module "test"
        ClassConverter.registerClass(java.util.Date.class, ctx, "test");

        DataType type = ctx.getDataType("test::Date");
        assertNotNull(type);
        assertInstanceOf(ExternalClassType.class, type);
        assertEquals("java.util.Date", ((ExternalClassType) type).getFqn());
    }

    @Test
    void getDataType_moduleQualified_notFound_throwsEvaluationException() {
        EvaluationException ex = assertThrows(EvaluationException.class,
            () -> ctx.getDataType("test::NoSuchClass"));
        assertTrue(ex.getMessage().contains("NoSuchClass"));
        assertTrue(ex.getMessage().contains("test"));
    }

    // ── getCallStack() on DefaultEvalContext (not Root) ───────────────────────

    @Test
    void getCallStack_onDefaultEvalContext_throwsUnsupportedOperation() {
        DefaultEvalContext plain = new DefaultEvalContext();
        assertThrows(UnsupportedOperationException.class, plain::getCallStack);
    }
}
