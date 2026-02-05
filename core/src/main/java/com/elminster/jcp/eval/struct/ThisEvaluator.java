package com.elminster.jcp.eval.struct;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;

/**
 * Evaluator for 'this' keyword.
 * Returns the current instance in constructors and instance methods.
 */
public class ThisEvaluator extends AbstractAstEvaluator {

  public ThisEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    Data thisRef = evalContext.getVariable("this");
    if (thisRef == null) {
      throw new IllegalStateException("'this' can only be used in instance methods or constructors");
    }
    return thisRef;
  }
}
