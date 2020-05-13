package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.data.AnyFlowData;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.eval.context.LoopContext;
import com.elminster.jcp.eval.context.EvalContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class BlockImpl extends AbstractStatement implements Block {

  private static final Logger logger = LoggerFactory.getLogger(BlockImpl.class);

  protected List<Statement> body = new LinkedList<>();

  public BlockImpl(Statement... statements) {
    for (Statement statement : statements) {
      body.add(statement);
    }
  }

  @Override
  public List<Statement> getBody() {
    return body;
  }

  @Override
  public void addStatement(Statement statement) {
    this.body.add(statement);
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    FlowData result = AnyFlowData.EMPTY;
    for (Statement statement : body) {
      LoopContext loopContext = evalContext.getLoopContext();
      if (null != loopContext && loopContext.isBreakBlock()) {
        break;
      }
      result = statement.eval(evalContext);
    }
    return result;
  }

  @Override
  public String getName() {
    return "BLOCK";
  }
}
