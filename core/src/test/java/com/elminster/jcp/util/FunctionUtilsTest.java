package com.elminster.jcp.util;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FunctionUtils.
 */
class FunctionUtilsTest {

    @Nested
    class GenerateFunctionFullNameWithParameterDefsTests {

        @Test
        void testNoParameters() {
            Identifier id = IdentifierExpression.of("myFunc");
            String fullName = FunctionUtils.generateFunctionFullName(id, new ParameterDef[]{});
            assertEquals("myFunc#", fullName);
        }

        @Test
        void testSingleParameter() {
            Identifier id = IdentifierExpression.of("greet");
            ParameterDef[] params = new ParameterDef[]{
                ParameterDef.of("name", SystemDataType.STRING)
            };
            String fullName = FunctionUtils.generateFunctionFullName(id, params);
            assertEquals("greet#String", fullName);
        }

        @Test
        void testMultipleParameters() {
            Identifier id = IdentifierExpression.of("add");
            ParameterDef[] params = new ParameterDef[]{
                ParameterDef.of("a", SystemDataType.INT),
                ParameterDef.of("b", SystemDataType.INT)
            };
            String fullName = FunctionUtils.generateFunctionFullName(id, params);
            assertEquals("add#Integer@Integer", fullName);
        }
    }

    @Nested
    class GenerateFunctionFullNameWithDataTypesTests {

        @Test
        void testNoParameters() {
            Identifier id = IdentifierExpression.of("getValue");
            String fullName = FunctionUtils.generateFunctionFullName(id, new DataType[]{});
            assertEquals("getValue#", fullName);
        }

        @Test
        void testWithParameters() {
            Identifier id = IdentifierExpression.of("calculate");
            DataType[] params = new DataType[]{
                SystemDataType.INT,
                SystemDataType.DOUBLE
            };
            String fullName = FunctionUtils.generateFunctionFullName(id, params);
            assertEquals("calculate#Integer@Double", fullName);
        }
    }

    @Nested
    class GenerateFunctionFullNameWithDataTests {

        @Test
        void testNoParameters() {
            Identifier id = IdentifierExpression.of("getEmpty");
            String fullName = FunctionUtils.generateFunctionFullName(id, new Data[]{});
            assertEquals("getEmpty#", fullName);
        }

        @Test
        void testWithData() {
            Identifier id = IdentifierExpression.of("process");
            Data[] dataArray = new Data[]{
                new AnyData<>(42, SystemDataType.INT),
                new AnyData<>("hello", SystemDataType.STRING)
            };
            String fullName = FunctionUtils.generateFunctionFullName(id, dataArray);
            assertEquals("process#Integer@String", fullName);
        }
    }

    @Nested
    class FunctionToStringTests {

        @Test
        void testNoParameters() {
            Identifier id = IdentifierExpression.of("noParams");
            String str = FunctionUtils.functionToString(id, new DataType[]{});
            assertEquals("noParams()", str);
        }

        @Test
        void testSingleParameter() {
            Identifier id = IdentifierExpression.of("single");
            DataType[] params = new DataType[]{SystemDataType.STRING};
            String str = FunctionUtils.functionToString(id, params);
            assertEquals("single(String)", str);
        }

        @Test
        void testMultipleParameters() {
            Identifier id = IdentifierExpression.of("multi");
            DataType[] params = new DataType[]{
                SystemDataType.INT,
                SystemDataType.STRING,
                SystemDataType.BOOLEAN
            };
            String str = FunctionUtils.functionToString(id, params);
            assertEquals("multi(Integer,String,Boolean)", str);
        }
    }

    @Nested
    class ModuleFunctionNameTests {

        @Test
        void testBaseModule() {
            String name = FunctionUtils.getModuleFunctionName("base", "Logger", "log");
            assertEquals("Logger.log", name);
        }

        @Test
        void testEmptyModule() {
            String name = FunctionUtils.getModuleFunctionName("", "Math", "sum");
            assertEquals("Math.sum", name);
        }

        @Test
        void testUserModule() {
            // Note: "user" module is NOT treated as base, so module prefix is added
            String name = FunctionUtils.getModuleFunctionName("user", "Counter", "get");
            assertEquals("user::Counter.get", name);
        }

        @Test
        void testOtherModule() {
            String name = FunctionUtils.getModuleFunctionName("mymodule", "MyType", "func");
            assertEquals("mymodule::MyType.func", name);
        }
    }

    @Nested
    class FullyQualifiedFunctionNameTests {

        @Test
        void testWithModule() {
            String name = FunctionUtils.getFullyQualifiedFunctionName("user", "Counter", "get");
            assertEquals("user::Counter::get", name);
        }

        @Test
        void testNullModule() {
            String name = FunctionUtils.getFullyQualifiedFunctionName(null, "Logger", "log");
            assertEquals("base::Logger::log", name);
        }

        @Test
        void testEmptyModule() {
            String name = FunctionUtils.getFullyQualifiedFunctionName("", "Math", "sum");
            assertEquals("base::Math::sum", name);
        }

        @Test
        void testGlobalType() {
            String name = FunctionUtils.getFullyQualifiedFunctionName("user", "global", "abs");
            assertEquals("user::global::abs", name);
        }
    }

    @Nested
    class GenerateFunctionFullNameWithModuleTests {

        @Test
        void testWithDataArray() {
            Data[] dataArray = new Data[]{
                new AnyData<>(42, SystemDataType.INT)
            };
            String fullName = FunctionUtils.generateFunctionFullName(
                "mymodule", "MyType", "func", dataArray);
            assertEquals("mymodule::MyType.func#Integer", fullName);
        }

        @Test
        void testWithBaseModule() {
            Data[] dataArray = new Data[]{};
            String fullName = FunctionUtils.generateFunctionFullName(
                "base", "Logger", "log", dataArray);
            assertEquals("Logger.log#", fullName);
        }

        @Test
        void testWithParameterDefs() {
            ParameterDef[] params = new ParameterDef[]{
                ParameterDef.of("x", SystemDataType.INT),
                ParameterDef.of("y", SystemDataType.INT)
            };
            String fullName = FunctionUtils.generateFunctionFullName(
                "user", "Math", "add", params);
            assertEquals("user::Math.add#Integer@Integer", fullName);
        }

        @Test
        void testWithNullParameterDefs() {
            String fullName = FunctionUtils.generateFunctionFullName(
                "base", "Logger", "info", (ParameterDef[]) null);
            assertEquals("Logger.info#", fullName);
        }
    }

    @Nested
    class GlobalFunctionNameTests {

        @Test
        void testGetGlobalFunctionName() {
            String name = FunctionUtils.getGlobalFunctionName("user", "abs");
            assertEquals("user::global.abs", name);
        }

        @Test
        void testGenerateGlobalFunctionFullNameWithParameterDefs() {
            ParameterDef[] params = new ParameterDef[]{
                ParameterDef.of("value", SystemDataType.INT)
            };
            String fullName = FunctionUtils.generateGlobalFunctionFullName("user", "abs", params);
            assertEquals("user::global.abs#Integer", fullName);
        }

        @Test
        void testGenerateGlobalFunctionFullNameWithData() {
            Data[] dataArray = new Data[]{
                new AnyData<>(3.14, SystemDataType.DOUBLE)
            };
            String fullName = FunctionUtils.generateGlobalFunctionFullName("user", "round", dataArray);
            assertEquals("user::global.round#Double", fullName);
        }
    }
}
