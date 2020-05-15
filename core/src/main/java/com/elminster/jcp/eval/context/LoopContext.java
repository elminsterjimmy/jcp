package com.elminster.jcp.eval.context;

import com.elminster.jcp.eval.ast.LoopEvaluator;

public interface LoopContext {

  int getLoopTime();

  void increaseLoopTime();

  /**
   * Get the loop component.
   * @return the loop component
   */
  LoopEvaluator getLoopStatement();

  /**
   * Get parent loop context.
   * @return the parent loop context
   */
  LoopContext getParent();

  void clear();

  /**
   * Add the loop context to the parent.
   * @param parent the parent loop context to add to
   */
  void addToParent(LoopContext parent);

  void setBreakBlock(boolean isBreak);

  boolean isBreakBlock();
}
