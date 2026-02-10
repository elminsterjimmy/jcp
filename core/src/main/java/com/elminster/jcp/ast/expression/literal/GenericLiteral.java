package com.elminster.jcp.ast.expression.literal;

/**
 * Generic literal for any value type.
 *
 * <p>Used when the specific literal type is not known at compile time.
 *
 * @param <T> the type of the literal value
 */
public class GenericLiteral<T> extends Literal<T> {

  private GenericLiteral(T value) {
    super(value);
  }

  public static <T> GenericLiteral<T> of(T value) {
    return new GenericLiteral<>(value);
  }

  @Override
  public String getName() {
    return "Literal";
  }
}
