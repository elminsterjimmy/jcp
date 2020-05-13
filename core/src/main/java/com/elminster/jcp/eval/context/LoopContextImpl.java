package com.elminster.jcp.eval.context;

import com.elminster.jcp.ast.statement.control.LoopStatement;

import java.util.concurrent.atomic.AtomicInteger;

public class LoopContextImpl implements LoopContext {

  private AtomicInteger time = new AtomicInteger(0);
  private LoopContext parent = null;
  private LoopStatement loopStatement = null;
  private boolean breakCurrentLoop = false;

  public LoopContextImpl(LoopStatement loopStatement) {
    this.loopStatement = loopStatement;
  }

  @Override
  public int getLoopTime() {
    return time.get();
  }

  @Override
  public void increaseLoopTime() {
    time.incrementAndGet();
  }

  @Override
  public void clear() {
    time.set(0);
    this.loopStatement = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LoopContext getParent() {
    return parent;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LoopStatement getLoopStatement() {
    return loopStatement;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addToParent(LoopContext parent) {
    this.parent = parent;
  }

  @Override
  public void setBreakBlock(boolean isBeaked) {
    this.breakCurrentLoop = isBeaked;
  }

  @Override
  public boolean isBreakBlock() {
    return breakCurrentLoop;
  }
}
