package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.eval.context.EvalContext;

abstract public class DataBase<T> implements Data<T> {

  protected final Identifier identifier;
  protected T data;
  protected boolean isConst = false;

  public DataBase() {
    this(Identifier.EMPTY_IDENTIFIER, null);
  }

  public DataBase(Identifier identifier, T data) {
    this(identifier, data, false);
  }

  public DataBase(T data) {
    this(data, true);
  }

  public DataBase(T data, boolean isConst) {
    this(Identifier.EMPTY_IDENTIFIER, data, isConst);
  }

  public DataBase(Identifier identifier, T data, boolean isConst) {
    this.identifier = identifier;
    this.data = data;
    this.isConst = isConst;
  }

  @Override
  public Identifier getIdentifier() {
    return identifier;
  }

  @Override
  public boolean isConst() {
    return isConst;
  }

  @Override
  public T get() {
    return data;
  }

  @Override
  public void set(T data) {
    if (isConst) {
      throw new IllegalStateException("cannot set a const data.");
    }
    this.data = data;
  }

  @Override
  public Data eval(EvalContext evalContext) {
    return this;
  }
}
