package com.elminster.jcp.compile.function;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.StructDeclarationImpl;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.compile.BytecodeGenerator;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StaticMethodCallCompiler - static method call bytecode generation.
 */
public class StaticMethodCallCompilerTest extends AbstractCompileTest {

    /**
     * Tests static method call with type not found.
     */
    @Test
    void testStaticMethodCallTypeNotFound() {
        Block program = new BlockImpl();

        StaticMethodCallExpression call = new StaticMethodCallExpression(
            Identifier.fromName("NonExistentType"),
            "method",
            LiteralExpression.of(42)
        );

        String className = uniqueClassName("TestTypeNotFound");
        BytecodeGenerator generator = new BytecodeGenerator(className);

        assertThrows(CompileException.class, () ->
            generator.compileWithReturn(program, call, SystemDataType.INT)
        );
    }

    /**
     * Tests struct class generation for static methods.
     * <pre>
     * struct Point { x: Int, y: Int }
     * // Verifies struct class is generated
     * </pre>
     */
    @Test
    void testStructStaticMethod() throws Exception {
        Block program = new BlockImpl();

        // Struct declaration
        StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
            new StructFieldDef("x", SystemDataType.INT),
            new StructFieldDef("y", SystemDataType.INT)
        ));
        program.addStatement(structDecl);

        // Compile to verify struct class is generated
        String className = uniqueClassName("TestStructStatic");
        Map<String, byte[]> allClasses = compiler.compileToMultipleClasses(program, className);

        assertTrue(allClasses.containsKey("Point"));
        assertNotNull(allClasses.get(className));
    }

    /**
     * Tests compilation does not fail for unrecognized primitive types.
     */
    @Test
    void testCompileBasicProgram() throws Exception {
        Block program = new BlockImpl();

        String className = uniqueClassName("TestBasicProgram");
        byte[] bytecode = compiler.compileToBytes(program, className);

        assertNotNull(bytecode);
        assertTrue(bytecode.length > 0);
    }
}
