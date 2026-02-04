package com.elminster.jcp.compile.operator.relational;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.BinaryExpression;
import com.elminster.jcp.compile.Compilable;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Base compiler for comparison expressions.
 * Similar to {@link com.elminster.jcp.eval.operator.relational.CompareEvaluator}.
 */
public abstract class CompareCompiler extends AbstractAstCompiler {

    public CompareCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        BinaryExpression binaryExpr = (BinaryExpression) astNode;
        Expression left = binaryExpr.getLeft();
        Expression right = binaryExpr.getRight();

        // Compile left operand
        Compilable leftCompiler = AstCompilerFactory.getCompiler(left);
        leftCompiler.compile(mv, ctx);

        // Compile right operand
        Compilable rightCompiler = AstCompilerFactory.getCompiler(right);
        rightCompiler.compile(mv, ctx);

        // Generate comparison code
        // The pattern is: compare, branch if false, push true, jump to end, push false, end
        Label trueLabel = new Label();
        Label endLabel = new Label();

        // Jump to trueLabel if condition is true
        mv.visitJumpInsn(getCompareOpcode(), trueLabel);

        // Push false (condition was false)
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitJumpInsn(Opcodes.GOTO, endLabel);

        // Push true (condition was true)
        mv.visitLabel(trueLabel);
        mv.visitInsn(Opcodes.ICONST_1);

        mv.visitLabel(endLabel);
    }

    /**
     * Get the comparison opcode (IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, etc.)
     *
     * @return the comparison opcode
     */
    protected abstract int getCompareOpcode();
}
