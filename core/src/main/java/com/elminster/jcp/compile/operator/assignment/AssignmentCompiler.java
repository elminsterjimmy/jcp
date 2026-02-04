package com.elminster.jcp.compile.operator.assignment;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.operation.AssignmentExpression;
import com.elminster.jcp.compile.Compilable;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.context.CompileContext.LocalVariable;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import com.elminster.jcp.compile.util.TypeMapper;
import org.objectweb.asm.MethodVisitor;

/**
 * Compiler for assignment expressions (=).
 */
public class AssignmentCompiler extends AbstractAstCompiler {

    public AssignmentCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        AssignmentExpression assignExpr = (AssignmentExpression) astNode;
        Expression left = assignExpr.getLeft();
        Expression right = assignExpr.getRight();

        // Get the variable name from the left side
        String varName = extractVariableName(left);
        LocalVariable local = ctx.getLocal(varName);
        if (local == null) {
            throw new CompileException("Undefined variable: " + varName);
        }

        // Compile the right-hand side expression
        Compilable rightCompiler = AstCompilerFactory.getCompiler(right);
        rightCompiler.compile(mv, ctx);

        // Duplicate the value on stack (assignment expression returns the assigned value)
        mv.visitInsn(org.objectweb.asm.Opcodes.DUP);

        // Store into the variable
        int storeOpcode = TypeMapper.getStoreOpcode(local.getType());
        mv.visitVarInsn(storeOpcode, local.getIndex());
    }

    private String extractVariableName(Expression expr) {
        if (expr instanceof com.elminster.jcp.ast.expression.base.VariableExpression) {
            return ((com.elminster.jcp.ast.expression.base.VariableExpression) expr).getId().getName();
        }
        if (expr instanceof com.elminster.jcp.ast.expression.operation.IdentifierExpression) {
            return ((com.elminster.jcp.ast.expression.operation.IdentifierExpression) expr).getId().getName();
        }
        throw new CompileException("Invalid assignment target: " + expr.getClass().getSimpleName());
    }
}
