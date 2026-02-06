package com.elminster.jcp.compile.control;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.statement.control.ReturnStatement;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ReturnCompiler.
 */
class ReturnCompilerTest {

    private CompileContext ctx;
    private MethodVisitor mv;

    @BeforeEach
    void setUp() {
        ctx = new CompileContext();
        ctx.setClassName("TestClass");

        // Create a dummy method visitor for testing
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "TestClass", null, "java/lang/Object", null);
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "test", "()V", null, null);
        mv.visitCode();
    }

    @Nested
    class ValidationTests {

        /**
         * Tests that return without function context throws CompileException.
         * <pre>
         * return 42  // throws: Return statement outside function context
         * </pre>
         */
        @Test
        void testReturnOutsideFunctionContext() {
            // Return type not set - simulates return outside function
            ReturnStatement returnStmt = new ReturnStatement(LiteralExpression.of(42));
            ReturnCompiler compiler = new ReturnCompiler(returnStmt);

            assertThrows(CompileException.class, () -> compiler.compile(mv, ctx));
        }

        /**
         * Tests that void function returning value throws CompileException.
         * <pre>
         * fn void test() {
         *   return 42  // throws: Void function cannot return a value
         * }
         * </pre>
         */
        @Test
        void testVoidFunctionReturningValue() {
            ctx.setCurrentFunctionReturnType(SystemDataType.VOID);

            ReturnStatement returnStmt = new ReturnStatement(LiteralExpression.of(42));
            ReturnCompiler compiler = new ReturnCompiler(returnStmt);

            CompileException ex = assertThrows(CompileException.class, () -> compiler.compile(mv, ctx));
            assertTrue(ex.getMessage().contains("Void function cannot return a value"));
        }

        /**
         * Tests that value return in non-void function compiles without error.
         * <pre>
         * fn Int test() {
         *   return 42  // OK
         * }
         * </pre>
         */
        @Test
        void testValueReturnInNonVoidFunction() {
            ctx.setCurrentFunctionReturnType(SystemDataType.INT);

            ReturnStatement returnStmt = new ReturnStatement(LiteralExpression.of(42));
            ReturnCompiler compiler = new ReturnCompiler(returnStmt);

            // Should not throw
            assertDoesNotThrow(() -> compiler.compile(mv, ctx));
        }

        /**
         * Tests double return compiles without error.
         * <pre>
         * fn Double test() {
         *   return 3.14  // OK
         * }
         * </pre>
         */
        @Test
        void testDoubleReturnCompiles() {
            ctx.setCurrentFunctionReturnType(SystemDataType.DOUBLE);

            ReturnStatement returnStmt = new ReturnStatement(LiteralExpression.of(3.14));
            ReturnCompiler compiler = new ReturnCompiler(returnStmt);

            // Should not throw
            assertDoesNotThrow(() -> compiler.compile(mv, ctx));
        }

        /**
         * Tests boolean return compiles without error.
         * <pre>
         * fn Boolean test() {
         *   return true  // OK
         * }
         * </pre>
         */
        @Test
        void testBooleanReturnCompiles() {
            ctx.setCurrentFunctionReturnType(SystemDataType.BOOLEAN);

            ReturnStatement returnStmt = new ReturnStatement(LiteralExpression.of(true));
            ReturnCompiler compiler = new ReturnCompiler(returnStmt);

            // Should not throw
            assertDoesNotThrow(() -> compiler.compile(mv, ctx));
        }

        /**
         * Tests string return compiles without error.
         * <pre>
         * fn String test() {
         *   return "hello"  // OK
         * }
         * </pre>
         */
        @Test
        void testStringReturnCompiles() {
            ctx.setCurrentFunctionReturnType(SystemDataType.STRING);

            ReturnStatement returnStmt = new ReturnStatement(LiteralExpression.of("hello"));
            ReturnCompiler compiler = new ReturnCompiler(returnStmt);

            // Should not throw
            assertDoesNotThrow(() -> compiler.compile(mv, ctx));
        }
    }
}
