package com.elminster.jcp.eval.operator.assignment;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.operator.arithmetic.PlusEvaluator;

public class PlusAssignmentEvaluator extends AssignmentEvaluator {

    public PlusAssignmentEvaluator(Node astNode) {
        super(astNode);
    }

    @Override
    protected Data doEval(EvalContext evalContext, Data eval, Data variable) {
        checkOperands(eval, variable);

        PlusEvaluator plusEvaluator = new PlusEvaluator(new Plus(
                new LiteralExpression(IntLiteral.of(((Integer) variable.get()))),
                new LiteralExpression(IntLiteral.of((Integer) eval.get())))
        );
        return plusEvaluator.eval(evalContext);
    }
}
