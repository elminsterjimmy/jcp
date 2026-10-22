package com.elminster.jcp.compile.module;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.MethodCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.compile.BytecodeGenerator;
import com.elminster.jcp.compile.MultiClassLoader;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DataTypeImpl;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DictionaryCompileTest extends AbstractCompileTest {

    @Test
    void testDictionaryNew() throws Exception {
        Block program = new BlockImpl();

        FunctionCallExpression ctorCall = new FunctionCallExpression(
            Identifier.fromName("Dictionary.new")
        );
        VariableDeclarationImpl decl = new VariableDeclarationImpl(
            "dict",
            new DataTypeImpl("Dictionary"),
            ctorCall
        );
        program.addStatement(decl);

        String className = uniqueClassName("TestDictionaryNew");
        Class<?> clazz = compiler.compileAndLoad(program, className);
        Method mainMethod = clazz.getMethod("main", String[].class);
        assertDoesNotThrow(() -> mainMethod.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testDictionaryPutAndSize() throws Exception {
        Block program = new BlockImpl();

        // Dictionary dict = Dictionary.new()
        program.addStatement(new VariableDeclarationImpl(
            "dict",
            new DataTypeImpl("Dictionary"),
            new FunctionCallExpression(Identifier.fromName("Dictionary.new"))
        ));

        // dict.put("a", 1)
        program.addStatement(ExpressionStatement.of(
            new MethodCallExpression(VariableExpression.of("dict"),
                "put",
                LiteralExpression.of(Literal.of("a")),
                LiteralExpression.of(Literal.of(1)))
        ));

        // dict.put("b", 2)
        program.addStatement(ExpressionStatement.of(
            new MethodCallExpression(VariableExpression.of("dict"),
                "put",
                LiteralExpression.of(Literal.of("b")),
                LiteralExpression.of(Literal.of(2)))
        ));

        // int sz = dict.size()
        MethodCallExpression sizeCall = new MethodCallExpression(
            VariableExpression.of("dict"),
            "size"
        );

        String className = uniqueClassName("TestDictionaryPutAndSize");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, sizeCall, SystemDataType.INT);

        Map<String, byte[]> genClasses = generator.getGeneratedClasses();
        MultiClassLoader loader = new MultiClassLoader();
        for (Map.Entry<String, byte[]> entry : genClasses.entrySet()) {
            loader.defineClass(entry.getKey(), entry.getValue());
        }
        loader.defineClass(className, bytecode);
        Class<?> clazz = loader.loadClass(className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(2, result);
    }

    @Test
    void testDictionaryContainsKey() throws Exception {
        Block program = new BlockImpl();

        // Dictionary dict = Dictionary.new()
        program.addStatement(new VariableDeclarationImpl(
            "dict",
            new DataTypeImpl("Dictionary"),
            new FunctionCallExpression(Identifier.fromName("Dictionary.new"))
        ));

        // dict.put("found", 42)
        program.addStatement(ExpressionStatement.of(
            new MethodCallExpression(VariableExpression.of("dict"),
                "put",
                LiteralExpression.of(Literal.of("found")),
                LiteralExpression.of(Literal.of(42)))
        ));

        // boolean result = dict.containsKey("found")
        MethodCallExpression containsCall = new MethodCallExpression(
            VariableExpression.of("dict"),
            "containsKey",
            LiteralExpression.of(Literal.of("found"))
        );

        String className = uniqueClassName("TestDictionaryContainsKey");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, containsCall, SystemDataType.BOOLEAN);

        Map<String, byte[]> genClasses = generator.getGeneratedClasses();
        MultiClassLoader loader = new MultiClassLoader();
        for (Map.Entry<String, byte[]> entry : genClasses.entrySet()) {
            loader.defineClass(entry.getKey(), entry.getValue());
        }
        loader.defineClass(className, bytecode);
        Class<?> clazz = loader.loadClass(className);

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result);
    }
}
