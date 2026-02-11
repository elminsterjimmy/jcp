package com.elminster.jcp.compile;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeGeneratorTest {

    @Test
    void setSourceFile_WithNull_UsesDefaultSourceFile() {
        BytecodeGenerator generator = new BytecodeGenerator("Test");
        generator.setSourceFile(null);
        assertEquals("program.jcp", generator.getSourceFile());
    }

    @Test
    void setSourceFile_WithValue_SetsValue() {
        BytecodeGenerator generator = new BytecodeGenerator("Test");
        generator.setSourceFile("custom.jcp");
        assertEquals("custom.jcp", generator.getSourceFile());
    }

    @Test
    void getGeneratedClasses_BeforeCompile_ReturnsEmptyMap() {
        BytecodeGenerator generator = new BytecodeGenerator("Test");
        // Before compile(), rootContext is null
        Map<String, byte[]> classes = generator.getGeneratedClasses();
        assertNotNull(classes);
        assertTrue(classes.isEmpty());
    }

    @Test
    void compileWithReturn_NullProgram_CompilesExpression() throws Exception {
        BytecodeGenerator generator = new BytecodeGenerator("TestNullProgram");

        // Compile with null program
        byte[] bytecode = generator.compileWithReturn(
            null,
            LiteralExpression.of(42),
            SystemDataType.INT
        );

        assertNotNull(bytecode);
        assertTrue(bytecode.length > 0);

        // Load and execute
        Class<?> clazz = new TestClassLoader().defineClass("TestNullProgram", bytecode);
        Object result = clazz.getMethod("evaluate").invoke(null);
        assertEquals(42, result);
    }

    @Test
    void registerExternalClass_AddsClassForCompilation() throws Exception {
        BytecodeGenerator generator = new BytecodeGenerator("TestExternal");
        generator.registerExternalClass(StringBuilder.class);

        Block program = new BlockImpl();
        byte[] bytecode = generator.compile(program);

        assertNotNull(bytecode);
    }

    @Test
    void getClassName_ReturnsClassName() {
        BytecodeGenerator generator = new BytecodeGenerator("MyClass");
        assertEquals("MyClass", generator.getClassName());
    }

    @Test
    void compile_EmptyProgram_GeneratesBytecode() throws Exception {
        BytecodeGenerator generator = new BytecodeGenerator("EmptyProgram");
        Block program = new BlockImpl();

        byte[] bytecode = generator.compile(program);

        assertNotNull(bytecode);
        assertTrue(bytecode.length > 0);

        // Should be loadable
        Class<?> clazz = new TestClassLoader().defineClass("EmptyProgram", bytecode);
        assertNotNull(clazz);
        assertNotNull(clazz.getMethod("main", String[].class));
    }

    @Test
    void compileWithReturn_WithProgramStatements_ExecutesStatements() throws Exception {
        BytecodeGenerator generator = new BytecodeGenerator("TestWithStatements");

        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl("x", SystemDataType.INT, LiteralExpression.of(10)));

        // Return x
        byte[] bytecode = generator.compileWithReturn(
            program,
            new com.elminster.jcp.ast.expression.base.VariableExpression(
                com.elminster.jcp.ast.Identifier.fromName("x")
            ),
            SystemDataType.INT
        );

        assertNotNull(bytecode);

        Class<?> clazz = new TestClassLoader().defineClass("TestWithStatements", bytecode);
        Object result = clazz.getMethod("evaluate").invoke(null);
        assertEquals(10, result);
    }

    private static class TestClassLoader extends ClassLoader {
        public Class<?> defineClass(String name, byte[] bytecode) {
            return defineClass(name, bytecode, 0, bytecode.length);
        }
    }
}
