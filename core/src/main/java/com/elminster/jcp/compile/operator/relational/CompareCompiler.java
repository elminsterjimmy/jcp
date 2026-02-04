package com.elminster.jcp.compile.operator.relational;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.BinaryExpression;
import com.elminster.jcp.compile.Compilable;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import com.elminster.jcp.compile.util.TypeMapper;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Base compiler for comparison expressions.
 * Similar to {@link com.elminster.jcp.eval.operator.relational.CompareEvaluator}.
 */
public abstract class CompareCompiler extends AbstractAstCompiler {

    protected boolean useDouble;

    public CompareCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        BinaryExpression binaryExpr = (BinaryExpression) astNode;
        Expression left = binaryExpr.getLeft();
        Expression right = binaryExpr.getRight();

        DataType leftType = TypeMapper.getExpressionType(left, ctx);
        DataType rightType = TypeMapper.getExpressionType(right, ctx);
        useDouble = (leftType == SystemDataType.DOUBLE || rightType == SystemDataType.DOUBLE);

        // Compile left operand
        Compilable leftCompiler = AstCompilerFactory.getCompiler(left);
        leftCompiler.compile(mv, ctx);
        // Promote int to double if needed
        if (useDouble && leftType == SystemDataType.INT) {
            mv.visitInsn(Opcodes.I2D);
        }

        // Compile right operand
        Compilable rightCompiler = AstCompilerFactory.getCompiler(right);
        rightCompiler.compile(mv, ctx);
        // Promote int to double if needed
        if (useDouble && rightType == SystemDataType.INT) {
            mv.visitInsn(Opcodes.I2D);
        }

        // Generate comparison code
        Label trueLabel = new Label();
        Label endLabel = new Label();

        if (useDouble) {
            // For doubles: use DCMPL or DCMPG then conditional branch
            mv.visitInsn(getDoubleCompareOpcode());
            mv.visitJumpInsn(getDoubleConditionOpcode(), trueLabel);
        } else {
            // For integers: use IF_ICMPxx directly
            mv.visitJumpInsn(getCompareOpcode(), trueLabel);
        }

        // Push false (condition was false)
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitJumpInsn(Opcodes.GOTO, endLabel);

        // Push true (condition was true)
        mv.visitLabel(trueLabel);
        mv.visitInsn(Opcodes.ICONST_1);

        mv.visitLabel(endLabel);
    }

    /**
     * Get the integer comparison opcode (IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, etc.)
     *
     * @return the comparison opcode
     */
    protected abstract int getCompareOpcode();

    /**
     * Get the double compare opcode (DCMPL or DCMPG).
     * DCMPL: NaN → -1 (use for <, <=, ==, !=)
     * DCMPG: NaN → +1 (use for >, >=)
     *
     * @return DCMPL or DCMPG
     */
    protected int getDoubleCompareOpcode() {
        return Opcodes.DCMPL;
    }

    /**
     * Get the condition opcode for double comparison result.
     * After DCMPL/DCMPG, stack has -1, 0, or 1.
     *
     * @return IFLT, IFLE, IFGT, IFGE, IFEQ, or IFNE
     */
    protected abstract int getDoubleConditionOpcode();
}
