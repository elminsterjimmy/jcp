package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.Statement;
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
  public Block addStatement(Statement statement) {
    this.body.add(statement);
    return this;
  }

  @Override
  public String getName() {
    return "BLOCK";
  }
}
