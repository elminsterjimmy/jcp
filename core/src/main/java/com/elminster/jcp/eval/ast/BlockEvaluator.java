package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.LoopContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

import java.util.List;

public class BlockEvaluator extends AbstractAstEvaluator {
  public BlockEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    Block block = (Block) astNode;
    List<Statement> body =  block.getBody();
    Data result = AnyData.EMPTY;
    for (Statement statement : body) {
      LoopContext loopContext = evalContext.getLoopContext();
      if (null != loopContext && loopContext.isBreakBlock()) {
        break;
      }
      Evaluable evaluable = AstEvaluatorFactory.getEvaluator(statement);
      result = evaluable.eval(evalContext);
    }
    return result;
  }
}
