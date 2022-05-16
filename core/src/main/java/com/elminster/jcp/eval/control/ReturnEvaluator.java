package com.elminster.jcp.eval.control;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.statement.control.ReturnStatement;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.LoopContext;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

public class ReturnEvaluator extends ControlEvaluator {

    public ReturnEvaluator(Node astNode) {
        super(astNode);
    }

    @Override
    public Data eval(EvalContext evalContext) {
        ReturnStatement returnStatement = (ReturnStatement) astNode;
        Expression expression = returnStatement.getExpression();
        LoopContext loopContext = evalContext.getLoopContext();
        if (null != loopContext) {
            loopContext.getLoopStatement().doBreak(evalContext);
        }
        evalContext.setReturn(true);
        // the return can hold null expression
        if (null == expression) {
            return AnyData.EMPTY;
        }
        Evaluable evaluable = AstEvaluatorFactory.getEvaluator(expression);
        return evaluable.eval(evalContext);
    }
}
