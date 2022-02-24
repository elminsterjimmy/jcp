package com.elminster.jcp.eval.operator.assignment;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.base.AssignmentExpression;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.excpetion.CannotCastException;
import com.elminster.jcp.eval.excpetion.UndeclaredException;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

import static com.elminster.jcp.ast.expression.operator.AssignmentOperator.PLUS_ASSIGNMENT;

public class AssignmentEvaluator extends AbstractAstEvaluator {

    public AssignmentEvaluator(Node astNode) {
        super(astNode);
    }

    @Override
    public Data eval(EvalContext evalContext) {
        AssignmentExpression assignmentExpression = (AssignmentExpression) this.astNode;
        Identifier id = assignmentExpression.getId();
        Data variable = evalContext.getVariable(id.getId());
        if (null == variable) {
            UndeclaredException.throwVariableUndeclaredException(id);
        }
        Expression expression = assignmentExpression.getExpression();
        Evaluable evaluator = AstEvaluatorFactory.getEvaluator(expression);
        Data eval = evaluator.eval(evalContext);

        Data calculated = doEval(evalContext, eval, variable);
        variable.set(calculated.get());
        return AnyData.EMPTY;
    }

    protected Data doEval(EvalContext evalContext, Data eval, Data variable) {
        if (!eval.getDataType().isCastableTo(variable.getDataType())) {
            throw new CannotCastException(eval.getDataType(), variable.getDataType());
        }
        return eval;
    }

    static void checkOperands(Data eval, Data variable) {
        if (!variable.getDataType().isCastableTo(DataType.SystemDataType.INT)) {
            throw new UnsupportedOperationException(String.format("%s not support %s", variable.getDataType(),
                    PLUS_ASSIGNMENT.getName()));
        }
        if (!variable.getDataType().isCastableTo(DataType.SystemDataType.INT)) {
            throw new UnsupportedOperationException(String.format("%s not support %s by %s", variable.getDataType(),
                    PLUS_ASSIGNMENT.getName(), eval.getDataType()));
        }
    }

}
