package com.elminster.jcp.compile.operator.postfix;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.operation.MinusMinus;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.context.CompileContext.LocalVariable;
import com.elminster.jcp.compile.exception.CompileException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for post-decrement (x--) expressions.
 */
public class MinusMinusCompiler extends AbstractAstCompiler {

    public MinusMinusCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        MinusMinus minusMinus = (MinusMinus) astNode;
        Expression operand = minusMinus.getOperand();

        String varName = extractVariableName(operand);
        LocalVariable local = ctx.getLocal(varName);
        if (local == null) {
            throw new CompileException("Undefined variable: " + varName);
        }

        // Load current value (this is the return value for post-decrement)
        mv.visitVarInsn(Opcodes.ILOAD, local.getIndex());

        // Decrement the variable
        mv.visitIincInsn(local.getIndex(), -1);
    }

    private String extractVariableName(Expression expr) {
        if (expr instanceof com.elminster.jcp.ast.expression.base.VariableExpression) {
            return ((com.elminster.jcp.ast.expression.base.VariableExpression) expr).getId().getName();
        }
        if (expr instanceof com.elminster.jcp.ast.expression.operation.IdentifierExpression) {
            return ((com.elminster.jcp.ast.expression.operation.IdentifierExpression) expr).getId().getName();
        }
        throw new CompileException("Invalid decrement target: " + expr.getClass().getSimpleName());
    }
}
