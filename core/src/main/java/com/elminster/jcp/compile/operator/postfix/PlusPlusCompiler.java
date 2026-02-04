package com.elminster.jcp.compile.operator.postfix;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.operation.PlusPlus;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.context.CompileContext.LocalVariable;
import com.elminster.jcp.compile.exception.CompileException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for post-increment (x++) expressions.
 */
public class PlusPlusCompiler extends AbstractAstCompiler {

    public PlusPlusCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        PlusPlus plusPlus = (PlusPlus) astNode;
        Expression operand = plusPlus.getOperand();

        String varName = extractVariableName(operand);
        LocalVariable local = ctx.getLocal(varName);
        if (local == null) {
            throw new CompileException("Undefined variable: " + varName);
        }

        // Load current value (this is the return value for post-increment)
        mv.visitVarInsn(Opcodes.ILOAD, local.getIndex());

        // Increment the variable
        mv.visitIincInsn(local.getIndex(), 1);
    }

    private String extractVariableName(Expression expr) {
        if (expr instanceof com.elminster.jcp.ast.expression.base.VariableExpression) {
            return ((com.elminster.jcp.ast.expression.base.VariableExpression) expr).getId().getName();
        }
        if (expr instanceof com.elminster.jcp.ast.expression.operation.IdentifierExpression) {
            return ((com.elminster.jcp.ast.expression.operation.IdentifierExpression) expr).getId().getName();
        }
        throw new CompileException("Invalid increment target: " + expr.getClass().getSimpleName());
    }
}
