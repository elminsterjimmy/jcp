package com.elminster.jcp.ast.statement.function;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FunctionDef.
 */
class FunctionDefTest {

    @Test
    void testFunctionDef_WithNameOnly() {
        FunctionDef funcDef = new FunctionDef("testFunc");

        assertEquals("testFunc", funcDef.id.getId());
        assertEquals(SystemDataType.ANY, funcDef.returnType);
        assertEquals(0, funcDef.parameters.length);
    }

    @Test
    void testFunctionDef_WithNameAndDataType() {
        FunctionDef funcDef = new FunctionDef("testFunc", SystemDataType.INT);

        assertEquals("testFunc", funcDef.id.getId());
        assertEquals(SystemDataType.INT, funcDef.returnType);
        assertEquals(0, funcDef.parameters.length);
    }

    @Test
    void testFunctionDef_WithNameAndParameters() {
        ParameterDef[] params = {
            ParameterDef.of("a", SystemDataType.INT),
            ParameterDef.of("b", SystemDataType.STRING)
        };
        FunctionDef funcDef = new FunctionDef("testFunc", params);

        assertEquals("testFunc", funcDef.id.getId());
        assertEquals(SystemDataType.ANY, funcDef.returnType);
        assertEquals(2, funcDef.parameters.length);
    }

    @Test
    void testFunctionDef_WithAllParams() {
        ParameterDef[] params = {
            ParameterDef.of("a", SystemDataType.INT)
        };
        FunctionDef funcDef = new FunctionDef("myFunc", SystemDataType.DOUBLE, params);

        assertEquals("myFunc", funcDef.id.getId());
        assertEquals(SystemDataType.DOUBLE, funcDef.returnType);
        assertEquals(1, funcDef.parameters.length);
    }

    @Test
    void testFunctionDef_WithIdentifier() {
        Identifier id = Identifier.fromName("funcId");
        ParameterDef[] params = {
            ParameterDef.of("x", SystemDataType.BOOLEAN)
        };
        FunctionDef funcDef = new FunctionDef(id, SystemDataType.VOID, params);

        assertEquals("funcId", funcDef.id.getId());
        assertEquals(SystemDataType.VOID, funcDef.returnType);
        assertEquals(1, funcDef.parameters.length);
    }
}
