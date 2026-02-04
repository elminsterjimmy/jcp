package com.elminster.jcp.compile.operator.arithmetic;

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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Base compiler for arithmetic expressions.
 * Similar to {@link com.elminster.jcp.eval.operator.arithmetic.ArithmeticEvaluator}.
 */
public abstract class ArithmeticCompiler extends AbstractAstCompiler {

    protected boolean useDouble;

    public ArithmeticCompiler(Node astNode) {
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

        // Compile left operand (pushes value onto stack)
        Compilable leftCompiler = AstCompilerFactory.getCompiler(left);
        leftCompiler.compile(mv, ctx);
        // Promote int to double if needed
        if (useDouble && leftType == SystemDataType.INT) {
            mv.visitInsn(Opcodes.I2D);
        }

        // Compile right operand (pushes value onto stack)
        Compilable rightCompiler = AstCompilerFactory.getCompiler(right);
        rightCompiler.compile(mv, ctx);
        // Promote int to double if needed
        if (useDouble && rightType == SystemDataType.INT) {
            mv.visitInsn(Opcodes.I2D);
        }

        // Apply the arithmetic operation
        emitOperation(mv);
    }

    /**
     * Emit the specific arithmetic operation instruction.
     *
     * @param mv the method visitor
     */
    protected abstract void emitOperation(MethodVisitor mv);
}
