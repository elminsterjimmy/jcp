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
    class GenerateSignatureWithParameterDefsTests {

        @Test
        void testNoParameters() {
            Identifier id = IdentifierExpression.of("myFunc");
            String signature = FunctionUtils.generateSignature(id, new ParameterDef[]{});
            // identifier encodes the base name; params are empty → trailing @
            assertEquals("myFunc@", signature);
        }

        @Test
        void testSingleParameter() {
            Identifier id = IdentifierExpression.of("greet");
            ParameterDef[] params = new ParameterDef[]{
                ParameterDef.of("name", SystemDataType.STRING)
            };
            String signature = FunctionUtils.generateSignature(id, params);
            assertEquals("greet@String", signature);
        }

        @Test
        void testMultipleParameters() {
            Identifier id = IdentifierExpression.of("add");
            ParameterDef[] params = new ParameterDef[]{
                ParameterDef.of("a", SystemDataType.INT),
                ParameterDef.of("b", SystemDataType.INT)
            };
            String signature = FunctionUtils.generateSignature(id, params);
            assertEquals("add@Integer@Integer", signature);
        }
    }

    @Nested
    class GenerateSignatureWithDataTypesTests {

        @Test
        void testNoParameters() {
            Identifier id = IdentifierExpression.of("getValue");
            String signature = FunctionUtils.generateSignature(id, new DataType[]{});
            assertEquals("getValue@", signature);
        }

        @Test
        void testWithParameters() {
            Identifier id = IdentifierExpression.of("calculate");
            DataType[] params = new DataType[]{
                SystemDataType.INT,
                SystemDataType.DOUBLE
            };
            String signature = FunctionUtils.generateSignature(id, params);
            assertEquals("calculate@Integer@Double", signature);
        }
    }

    @Nested
    class GenerateSignatureWithDataTests {

        @Test
        void testNoParameters() {
            Identifier id = IdentifierExpression.of("getEmpty");
            String signature = FunctionUtils.generateSignature(id, new Data[]{});
            assertEquals("getEmpty@", signature);
        }

        @Test
        void testWithData() {
            Identifier id = IdentifierExpression.of("process");
            Data[] dataArray = new Data[]{
                new AnyData<>(42, SystemDataType.INT),
                new AnyData<>("hello", SystemDataType.STRING)
            };
            String signature = FunctionUtils.generateSignature(id, dataArray);
            assertEquals("process@Integer@String", signature);
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
    class GenerateSignatureWithModuleTests {

        @Test
        void testWithDataArray() {
            Data[] dataArray = new Data[]{
                new AnyData<>(42, SystemDataType.INT)
            };
            String signature = FunctionUtils.generateSignature(
                "mymodule", "MyType", "func", dataArray);
            assertEquals("mymodule::MyType#func@Integer", signature);
        }

        @Test
        void testWithBaseModule() {
            Data[] dataArray = new Data[]{};
            String signature = FunctionUtils.generateSignature(
                "base", "Logger", "log", dataArray);
            assertEquals("base::Logger#log@", signature);
        }

        @Test
        void testWithParameterDefs() {
            ParameterDef[] params = new ParameterDef[]{
                ParameterDef.of("x", SystemDataType.INT),
                ParameterDef.of("y", SystemDataType.INT)
            };
            String signature = FunctionUtils.generateSignature(
                "user", "Math", "add", params);
            assertEquals("user::Math#add@Integer@Integer", signature);
        }

        @Test
        void testWithNullParameterDefs() {
            String signature = FunctionUtils.generateSignature(
                "base", "Logger", "info", (ParameterDef[]) null);
            assertEquals("base::Logger#info@", signature);
        }

        @Test
        void testWithJavaFqnType() {
            // Simulates an ExternalClassType key with Java FQN as type segment
            Data[] dataArray = new Data[]{new AnyData<>(42, SystemDataType.INT)};
            String signature = FunctionUtils.generateSignature(
                "base", "java.util.Date", "compareTo", dataArray);
            assertEquals("base::java.util.Date#compareTo@Integer", signature);
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
        void testGenerateGlobalSignatureWithParameterDefs() {
            ParameterDef[] params = new ParameterDef[]{
                ParameterDef.of("value", SystemDataType.INT)
            };
            String signature = FunctionUtils.generateGlobalSignature("user", "abs", params);
            assertEquals("user::global#abs@Integer", signature);
        }

        @Test
        void testGenerateGlobalSignatureWithData() {
            Data[] dataArray = new Data[]{
                new AnyData<>(3.14, SystemDataType.DOUBLE)
            };
            String signature = FunctionUtils.generateGlobalSignature("user", "round", dataArray);
            assertEquals("user::global#round@Double", signature);
        }
    }
}
