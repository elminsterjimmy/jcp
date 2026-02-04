package com.elminster.jcp.compile.operator.postfix;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.UnaryExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
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
        UnaryExpression unaryExpr = (UnaryExpression) astNode;
        Expression operand = unaryExpr.getExpress();

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
        if (expr instanceof VariableExpression) {
            Identifier id = ((VariableExpression) expr).getId();
            return id.getId();
        }
        if (expr instanceof IdentifierExpression) {
            return ((IdentifierExpression) expr).getId();
        }
        throw new CompileException("Invalid decrement target: " + expr.getClass().getSimpleName());
    }
}
