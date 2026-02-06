package com.elminster.jcp.eval.excpetion;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.ast.statement.function.FunctionFactory;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.excpetion.AlreadyDeclaredException.FunctionAlreadyDeclaredException;
import com.elminster.jcp.eval.excpetion.AlreadyDeclaredException.VariableAlreadyDeclaredException;
import com.elminster.jcp.eval.excpetion.AlreadyDeclaredException.DataTypeAlreadyDeclaredException;
import com.elminster.jcp.eval.excpetion.UndeclaredException.FunctionUndeclaredException;
import com.elminster.jcp.eval.excpetion.UndeclaredException.VariableUndeclaredException;
import com.elminster.jcp.eval.excpetion.UndeclaredException.DataTypeUndeclaredException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for declaration exceptions.
 */
class ExceptionTest {

    @Nested
    class AlreadyDeclaredExceptionTests {

        @Test
        void testFunctionAlreadyDeclaredException() {
            ParameterDef[] params = {
                ParameterDef.of("a", SystemDataType.INT),
                ParameterDef.of("b", SystemDataType.STRING)
            };
            Function function = FunctionFactory.createFunction("testFunc", params, SystemDataType.VOID);

            FunctionAlreadyDeclaredException exception = new FunctionAlreadyDeclaredException(function);
            // getName() returns AST node type "FUNCTION", not the function identifier
            assertTrue(exception.getMessage().contains("FUNCTION"));
            assertTrue(exception.getMessage().contains("already declared"));
        }

        @Test
        void testThrowFunctionAlreadyDeclaredException() {
            Function function = FunctionFactory.createFunction("myFunc", SystemDataType.VOID);

            assertThrows(FunctionAlreadyDeclaredException.class, () ->
                AlreadyDeclaredException.throwFunctionAlreadyDeclaredException(function)
            );
        }

        @Test
        void testVariableAlreadyDeclaredException() {
            Identifier id = Identifier.fromName("myVar");
            VariableAlreadyDeclaredException exception = new VariableAlreadyDeclaredException(id);

            assertTrue(exception.getMessage().contains("myVar"));
            assertTrue(exception.getMessage().contains("already declared"));
        }

        @Test
        void testThrowVariableAlreadyDeclaredException() {
            Identifier id = Identifier.fromName("testVar");

            assertThrows(VariableAlreadyDeclaredException.class, () ->
                AlreadyDeclaredException.throwVariableAlreadyDeclaredException(id)
            );
        }

        @Test
        void testDataTypeAlreadyDeclaredException() {
            Identifier id = Identifier.fromName("MyType");
            DataTypeAlreadyDeclaredException exception = new DataTypeAlreadyDeclaredException(id);

            assertTrue(exception.getMessage().contains("MyType"));
            assertTrue(exception.getMessage().contains("already declared"));
        }

        @Test
        void testThrowDataTypeAlreadyDeclaredException() {
            Identifier id = Identifier.fromName("TestType");

            assertThrows(DataTypeAlreadyDeclaredException.class, () ->
                AlreadyDeclaredException.throwDataTypeAlreadyDeclaredException(id)
            );
        }
    }

    @Nested
    class UndeclaredExceptionTests {

        @Test
        void testFunctionUndeclaredException() {
            Identifier id = Identifier.fromName("unknownFunc");
            FunctionUndeclaredException exception = new FunctionUndeclaredException(
                id, SystemDataType.INT, SystemDataType.BOOLEAN
            );

            assertTrue(exception.getMessage().contains("unknownFunc"));
            assertTrue(exception.getMessage().contains("undeclared"));
            assertTrue(exception.getMessage().contains("Integer"));
            assertTrue(exception.getMessage().contains("Boolean"));
        }

        @Test
        void testThrowFunctionUndeclaredException() {
            Identifier id = Identifier.fromName("missingFunc");

            assertThrows(FunctionUndeclaredException.class, () ->
                UndeclaredException.throwFunctionUndeclaredException(id, SystemDataType.INT)
            );
        }

        @Test
        void testVariableUndeclaredException() {
            Identifier id = Identifier.fromName("unknownVar");
            VariableUndeclaredException exception = new VariableUndeclaredException(id);

            assertTrue(exception.getMessage().contains("unknownVar"));
            assertTrue(exception.getMessage().contains("undeclared"));
        }

        @Test
        void testThrowVariableUndeclaredException() {
            Identifier id = Identifier.fromName("missingVar");

            assertThrows(VariableUndeclaredException.class, () ->
                UndeclaredException.throwVariableUndeclaredException(id)
            );
        }

        @Test
        void testDataTypeUndeclaredException() {
            Identifier id = Identifier.fromName("UnknownType");
            DataTypeUndeclaredException exception = new DataTypeUndeclaredException(id);

            assertTrue(exception.getMessage().contains("UnknownType"));
            assertTrue(exception.getMessage().contains("undeclared"));
        }

        @Test
        void testThrowDataTypeUndeclaredException() {
            Identifier id = Identifier.fromName("MissingType");

            assertThrows(DataTypeUndeclaredException.class, () ->
                UndeclaredException.throwDataTypeUndeclaredException(id)
            );
        }
    }
}
