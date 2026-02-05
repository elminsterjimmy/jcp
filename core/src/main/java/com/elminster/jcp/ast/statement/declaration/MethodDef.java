package com.elminster.jcp.ast.statement.declaration;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.data.DataType;

/**
 * Unified method/constructor definition for types.
 * Constructors use "<init>" as the method name.
 */
public class MethodDef {

  public static final String CONSTRUCTOR_NAME = "<init>";

  private final Identifier id;
  private final ParameterDef[] parameters;
  private final DataType returnType;
  private final Block body;
  private final boolean isStatic;

  /**
   * Create an instance method.
   */
  public MethodDef(String name, DataType returnType, Block body, ParameterDef... parameters) {
    this(Identifier.fromName(name), returnType, body, false, parameters);
  }

  /**
   * Create a static method.
   */
  public static MethodDef staticMethod(String name, DataType returnType, Block body, ParameterDef... parameters) {
    return new MethodDef(Identifier.fromName(name), returnType, body, true, parameters);
  }

  /**
   * Create a constructor.
   */
  public static MethodDef constructor(Block body, ParameterDef... parameters) {
    return new MethodDef(Identifier.fromName(CONSTRUCTOR_NAME), DataType.SystemDataType.VOID, body, false, parameters);
  }

  /**
   * Full constructor.
   */
  public MethodDef(Identifier id, DataType returnType, Block body, boolean isStatic, ParameterDef... parameters) {
    this.id = id;
    this.parameters = parameters;
    this.returnType = returnType;
    this.body = body;
    this.isStatic = isStatic;
  }

  public Identifier getId() {
    return id;
  }

  public ParameterDef[] getParameters() {
    return parameters;
  }

  public DataType getReturnType() {
    return returnType;
  }

  public Block getBody() {
    return body;
  }

  public boolean isStatic() {
    return isStatic;
  }

  public boolean isConstructor() {
    return CONSTRUCTOR_NAME.equals(id.getId());
  }
}
