package com.elminster.jcp.eval.operator.assignment;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.operation.Mod;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.operator.arithmetic.ModEvaluator;

public class ModAssignmentEvaluator extends AssignmentEvaluator {

    public ModAssignmentEvaluator(Node astNode) {
        super(astNode);
    }

    @Override
    protected Data doEval(EvalContext evalContext, Data eval, Data variable) {
        checkOperands(eval, variable);

        ModEvaluator modEvaluator = new ModEvaluator(new Mod(
                new LiteralExpression(IntLiteral.of(((Integer) variable.get()))),
                new LiteralExpression(IntLiteral.of((Integer) eval.get())))
        );
        return modEvaluator.eval(evalContext);
    }
}
