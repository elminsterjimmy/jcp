package com.elminster.jcp.compile.base;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.exception.CompileException;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VariableCompiler.
 */
public class VariableCompilerTest {

    /**
     * Tests that compiling an undefined variable throws CompileException.
     */
    @Test
    void testUndefinedVariable_ThrowsCompileException() {
        CompileContext ctx = new CompileContext();
        ctx.setClassName("TestClass");
        // Don't allocate 'undefinedVar'

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "TestClass", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "test", "()I", null, null);
        mv.visitCode();

        VariableExpression varExpr = new VariableExpression(Identifier.fromName("undefinedVar"));
        VariableCompiler varCompiler = new VariableCompiler(varExpr);

        CompileException ex = assertThrows(CompileException.class, () -> varCompiler.compile(mv, ctx));
        assertTrue(ex.getMessage().contains("Undefined variable"));
    }
}
