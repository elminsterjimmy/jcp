package com.elminster.jcp.compile.operator.arithmetic;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.BinaryExpression;
import com.elminster.jcp.compile.Compilable;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import org.objectweb.asm.MethodVisitor;

/**
 * Base compiler for arithmetic expressions.
 * Similar to {@link com.elminster.jcp.eval.operator.arithmetic.ArithmeticEvaluator}.
 */
public abstract class ArithmeticCompiler extends AbstractAstCompiler {

    public ArithmeticCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        BinaryExpression binaryExpr = (BinaryExpression) astNode;
        Expression left = binaryExpr.getLeft();
        Expression right = binaryExpr.getRight();

        // Compile left operand (pushes value onto stack)
        Compilable leftCompiler = AstCompilerFactory.getCompiler(left);
        leftCompiler.compile(mv, ctx);

        // Compile right operand (pushes value onto stack)
        Compilable rightCompiler = AstCompilerFactory.getCompiler(right);
        rightCompiler.compile(mv, ctx);

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
