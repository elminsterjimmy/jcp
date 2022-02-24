package com.elminster.jcp.eval.base;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.*;

public class LiteralEvaluator extends AbstractAstEvaluator {

  public LiteralEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    LiteralExpression literalExpression = (LiteralExpression) astNode;
    Literal literal = literalExpression.getLiteral();
    return literalToData(literal);
  }

  private Data literalToData(Literal literal) {
    if (literal instanceof StringLiteral) {
      return StringData.newString(((StringLiteral) literal).getValue());
    } else if (literal instanceof BooleanLiteral) {
      return BooleanData.newBoolean(((BooleanLiteral) literal).getValue());
    } else if (literal instanceof IntLiteral) {
      return IntegerData.constInt(((IntLiteral) literal).getValue());
    } else {
      Object value = literal.getValue();
      return DataFactory.INSTANCE.createConstValue(value);
    }
  }
}
