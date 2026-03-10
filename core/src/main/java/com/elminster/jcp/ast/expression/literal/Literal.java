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
   * Creates the most specific literal for the given value.
   *
   * <p>Dispatches to the typed subclass based on the value's runtime type:
   * {@code Integer} → {@link IntLiteral}, {@code Double} → {@link DoubleLiteral},
   * {@code Boolean} → {@link BooleanLiteral}, {@code String} → {@link StringLiteral},
   * {@code null} → {@link NullLiteral}. Falls back to {@link GenericLiteral} for
   * any other type.
   *
   * @param value the literal value
   * @param <T>   the type of the value
   * @return the most specific {@link Literal} subclass for the value
   */
  @SuppressWarnings("unchecked")
  public static <T> Literal<T> of(T value) {
    if (value == null)              return (Literal<T>) NullLiteral.INSTANCE;
    if (value instanceof Integer)   return (Literal<T>) IntLiteral.of((Integer) value);
    if (value instanceof Double)    return (Literal<T>) DoubleLiteral.of((Double) value);
    if (value instanceof Boolean)   return (Literal<T>) BooleanLiteral.of((Boolean) value);
    if (value instanceof String)    return (Literal<T>) StringLiteral.of((String) value);
    return GenericLiteral.of(value);
  }
}
