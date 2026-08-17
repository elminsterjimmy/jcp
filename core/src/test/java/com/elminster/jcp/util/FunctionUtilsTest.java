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
 *
 * Key format: module::typeFqn#method@paramFqn1@paramFqn2
 *
 * Separators:
 *   "::"  module/type boundary
 *   "#"   type/method boundary
 *   "@"   param separator
 *
 * System types use getName() as FQN (e.g. "Integer", "String", "Boolean").
 * ExternalClassType uses javaClass.getName() as FQN.
 * User/struct types use getName() as FQN (no package).
 */
class FunctionUtilsTest {

    @Nested
    class GenerateFunctionFullNameWithParameterDefsTests {

        @Test
        void testNoParameters() {
            Identifier id = IdentifierExpression.of("myFunc");
            String fullName = FunctionUtils.generateFunctionFullName(id, new ParameterDef[]{});
            // identifier encodes the base name; params are empty → trailing @
            assertEquals("myFunc@", fullName);
        }

        @Test
        void testSingleParameter() {
            Identifier id = IdentifierExpression.of("greet");
            ParameterDef[] params = new ParameterDef[]{
                ParameterDef.of("name", SystemDataType.STRING)
            };
            String fullName = FunctionUtils.generateFunctionFullName(id, params);
            assertEquals("greet@String", fullName);
        }

        @Test
        void testMultipleParameters() {
            Identifier id = IdentifierExpression.of("add");
            ParameterDef[] params = new ParameterDef[]{
                ParameterDef.of("a", SystemDataType.INT),
                ParameterDef.of("b", SystemDataType.INT)
            };
            String fullName = FunctionUtils.generateFunctionFullName(id, params);
            assertEquals("add@Integer@Integer", fullName);
        }
    }

    @Nested
    class GenerateFunctionFullNameWithDataTypesTests {

        @Test
        void testNoParameters() {
            Identifier id = IdentifierExpression.of("getValue");
            String fullName = FunctionUtils.generateFunctionFullName(id, new DataType[]{});
            assertEquals("getValue@", fullName);
        }

        @Test
        void testWithParameters() {
            Identifier id = IdentifierExpression.of("calculate");
            DataType[] params = new DataType[]{
                SystemDataType.INT,
                SystemDataType.DOUBLE
            };
            String fullName = FunctionUtils.generateFunctionFullName(id, params);
            assertEquals("calculate@Integer@Double", fullName);
        }
    }

    @Nested
    class GenerateFunctionFullNameWithDataTests {

        @Test
        void testNoParameters() {
            Identifier id = IdentifierExpression.of("getEmpty");
            String fullName = FunctionUtils.generateFunctionFullName(id, new Data[]{});
            assertEquals("getEmpty@", fullName);
        }

        @Test
        void testWithData() {
            Identifier id = IdentifierExpression.of("process");
            Data[] dataArray = new Data[]{
                new AnyData<>(42, SystemDataType.INT),
                new AnyData<>("hello", SystemDataType.STRING)
            };
            String fullName = FunctionUtils.generateFunctionFullName(id, dataArray);
            assertEquals("process@Integer@String", fullName);
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
            // format: module::typeFqn#method
            String name = FunctionUtils.getModuleFunctionName("base", "Logger", "log");
            assertEquals("base::Logger#log", name);
        }

        @Test
        void testEmptyModule() {
            // empty module → resolved to "base"
            String name = FunctionUtils.getModuleFunctionName("", "Math", "sum");
            assertEquals("base::Math#sum", name);
        }

        @Test
        void testUserModule() {
            String name = FunctionUtils.getModuleFunctionName("user", "Counter", "get");
            assertEquals("user::Counter#get", name);
        }

        @Test
        void testOtherModule() {
            String name = FunctionUtils.getModuleFunctionName("mymodule", "MyType", "func");
            assertEquals("mymodule::MyType#func", name);
        }

        @Test
        void testWithJavaFqn() {
            // ExternalClassType uses full Java FQN as type segment
            String name = FunctionUtils.getModuleFunctionName("base", "java.util.Date", "toString");
            assertEquals("base::java.util.Date#toString", name);
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
            assertEquals("mymodule::MyType#func@Integer", fullName);
        }

        @Test
        void testWithBaseModule() {
            Data[] dataArray = new Data[]{};
            String fullName = FunctionUtils.generateFunctionFullName(
                "base", "Logger", "log", dataArray);
            assertEquals("base::Logger#log@", fullName);
        }

        @Test
        void testWithParameterDefs() {
            ParameterDef[] params = new ParameterDef[]{
                ParameterDef.of("x", SystemDataType.INT),
                ParameterDef.of("y", SystemDataType.INT)
            };
            String fullName = FunctionUtils.generateFunctionFullName(
                "user", "Math", "add", params);
            assertEquals("user::Math#add@Integer@Integer", fullName);
        }

        @Test
        void testWithNullParameterDefs() {
            String fullName = FunctionUtils.generateFunctionFullName(
                "base", "Logger", "info", (ParameterDef[]) null);
            assertEquals("base::Logger#info@", fullName);
        }

        @Test
        void testWithJavaFqnType() {
            // Simulates an ExternalClassType key with Java FQN as type segment
            Data[] dataArray = new Data[]{new AnyData<>(42, SystemDataType.INT)};
            String fullName = FunctionUtils.generateFunctionFullName(
                "base", "java.util.Date", "compareTo", dataArray);
            assertEquals("base::java.util.Date#compareTo@Integer", fullName);
        }
    }

    @Nested
    class GlobalFunctionNameTests {

        @Test
        void testGetGlobalFunctionName() {
            String name = FunctionUtils.getGlobalFunctionName("user", "abs");
            assertEquals("user::global#abs", name);
        }

        @Test
        void testGenerateGlobalFunctionFullNameWithParameterDefs() {
            ParameterDef[] params = new ParameterDef[]{
                ParameterDef.of("value", SystemDataType.INT)
            };
            String fullName = FunctionUtils.generateGlobalFunctionFullName("user", "abs", params);
            assertEquals("user::global#abs@Integer", fullName);
        }

        @Test
        void testGenerateGlobalFunctionFullNameWithData() {
            Data[] dataArray = new Data[]{
                new AnyData<>(3.14, SystemDataType.DOUBLE)
            };
            String fullName = FunctionUtils.generateGlobalFunctionFullName("user", "round", dataArray);
            assertEquals("user::global#round@Double", fullName);
        }
    }
}
