package com.elminster.jcp.ast.expression.literal;

import com.elminster.jcp.ast.AbstractNode;
import com.elminster.jcp.ast.Expression;

/**
 * Base class for all literal values in the AST.
 *
 * <p>Literals represent constant values like integers, booleans, strings, etc.
 * They extend AbstractNode to support source location tracking.
 *
 * @param <T> the type of the literal value
 */
public abstract class Literal<T> extends AbstractNode implements Expression {

  private final T value;

  protected Literal(T value) {
    this.value = value;
  }

  public T getValue() {
    return value;
  }

  @Override
  public String getName() {
    return "Literal";
  }

  /**
   * Creates a generic literal for any value type.
   *
   * <p>For specific types, prefer using the typed factory methods:
   * {@link IntLiteral#of(Integer)}, {@link BooleanLiteral#of(Boolean)}, etc.
   *
   * @param value the literal value
   * @param <T>   the type of the value
   * @return a new GenericLiteral instance
   */
  public static <T> Literal<T> of(T value) {
    return GenericLiteral.of(value);
  }
}
