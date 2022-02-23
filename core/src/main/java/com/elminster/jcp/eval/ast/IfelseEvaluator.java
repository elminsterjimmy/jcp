package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.ast.control.IfElseStatement;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IfelseEvaluator extends ControlEvaluator {

  private static final Logger logger = LoggerFactory.getLogger(IfelseEvaluator.class);

  public IfelseEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    IfElseStatement ifElseStatement = (IfElseStatement) astNode;
    Expression condition = ifElseStatement.getCondition();
    Statement ifStatement = ifElseStatement.getIfStatement();
    Statement elseStatement = ifElseStatement.getElseStatement();
    if (checkCondition(condition, evalContext)) {
      logger.debug("[{}] condition [TRUE]", astNode.getName());
      Evaluable evaluable = AstEvaluatorFactory.getEvaluator(ifStatement);
      evaluable.eval(evalContext);
    } else {
      logger.debug("[{}] condition [FALSE]", astNode.getName());
      if (null != elseStatement) {
        Evaluable evaluable = AstEvaluatorFactory.getEvaluator(elseStatement);
        evaluable.eval(evalContext);
      }
    }
    return AnyData.EMPTY;
  }
}
