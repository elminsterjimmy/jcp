package com.elminster.jcp.eval.operator.assignment;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.Minus;
import com.elminster.jcp.ast.expression.base.Plus;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.operator.arithmetic.MinusEvaluator;
import com.elminster.jcp.eval.operator.arithmetic.PlusEvaluator;

import static com.elminster.jcp.ast.expression.operator.AssignmentOperator.PLUS_ASSIGNMENT;

public class MinusAssignmentEvaluator extends AssignmentEvaluator {

    public MinusAssignmentEvaluator(Node astNode) {
        super(astNode);
    }

    @Override
    protected Data doEval(EvalContext evalContext, Data eval, Data variable) {
        checkOperands(eval, variable);

        MinusEvaluator minusEvaluator = new MinusEvaluator(new Plus(
                new LiteralExpression(IntLiteral.of(((Integer) variable.get()))),
                new LiteralExpression(IntLiteral.of((Integer) eval.get())))
        );
        return minusEvaluator.eval(evalContext);
    }
}
