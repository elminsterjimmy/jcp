package com.elminster.jcp.compile.base;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.compile.context.CompileContext;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for literal expressions (int, boolean, string).
 * Similar to {@link com.elminster.jcp.eval.base.LiteralEvaluator}.
 */
public class LiteralCompiler extends AbstractAstCompiler {

    public LiteralCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        LiteralExpression literalExpression = (LiteralExpression) astNode;
        Literal literal = literalExpression.getLiteral();
        compileLiteral(literal, mv);
    }

    private void compileLiteral(Literal literal, MethodVisitor mv) {
        if (literal instanceof IntLiteral) {
            compileInt(((IntLiteral) literal).getValue(), mv);
        } else if (literal instanceof BooleanLiteral) {
            compileBoolean(((BooleanLiteral) literal).getValue(), mv);
        } else if (literal instanceof StringLiteral) {
            compileString(((StringLiteral) literal).getValue(), mv);
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported literal type: " + literal.getClass().getSimpleName());
        }
    }

    /**
     * Compile an integer literal using the most efficient instruction.
     */
    private void compileInt(int value, MethodVisitor mv) {
        if (value >= -1 && value <= 5) {
            // Use ICONST_M1 to ICONST_5
            mv.visitInsn(Opcodes.ICONST_0 + value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            // Use BIPUSH for byte range
            mv.visitIntInsn(Opcodes.BIPUSH, value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            // Use SIPUSH for short range
            mv.visitIntInsn(Opcodes.SIPUSH, value);
        } else {
            // Use LDC for larger values
            mv.visitLdcInsn(value);
        }
    }

    /**
     * Compile a boolean literal.
     */
    private void compileBoolean(boolean value, MethodVisitor mv) {
        mv.visitInsn(value ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
    }

    /**
     * Compile a string literal.
     */
    private void compileString(String value, MethodVisitor mv) {
        mv.visitLdcInsn(value);
    }
}
