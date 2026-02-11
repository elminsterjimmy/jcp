package com.elminster.jcp.compile;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.compile.context.CompileContext;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.*;

class CompileVisitorTest {

    @Test
    void getContext_ReturnsContext() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "Test", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "test", "()V", null, null);
        mv.visitCode();

        CompileContext ctx = new CompileContext();
        ctx.setClassName("Test");
        CompileVisitor visitor = new CompileVisitor(mv, ctx);

        assertSame(ctx, visitor.getContext());
    }

    @Test
    void getMethodVisitor_ReturnsMethodVisitor() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "Test", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "test", "()V", null, null);
        mv.visitCode();

        CompileContext ctx = new CompileContext();
        ctx.setClassName("Test");
        CompileVisitor visitor = new CompileVisitor(mv, ctx);

        assertSame(mv, visitor.getMethodVisitor());
    }

    @Test
    void visit_LiteralNode_CompilesSuccessfully() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "Test", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "test", "()I", null, null);
        mv.visitCode();

        CompileContext ctx = new CompileContext();
        ctx.setClassName("Test");
        CompileVisitor visitor = new CompileVisitor(mv, ctx);

        // Visit a literal expression
        visitor.visit(LiteralExpression.of(42));

        // Just verify it doesn't throw
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();
        cw.visitEnd();

        byte[] bytecode = cw.toByteArray();
        assertNotNull(bytecode);
        assertTrue(bytecode.length > 0);
    }
}
