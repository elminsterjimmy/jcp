package com.elminster.jcp.ast.expression.literal;

/**
 * Null literal value.
 *
 * <p>Represents the {@code null} constant. Singleton because null carries no value.
 */
public class NullLiteral extends Literal<Void> {

  public static final NullLiteral INSTANCE = new NullLiteral();

  private NullLiteral() {
    super(null);
  }

  @Override
  public String getName() {
    return "NullLiteral";
  }
}
