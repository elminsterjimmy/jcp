package com.elminster.jcp.ast.data;

import com.elminster.jcp.eval.context.EvalContext;

abstract public class DataBase<T> implements Data<T> {

  protected final static String EMPTY_NAME = "";
  protected final String name;
  protected T data;
  protected boolean isConst = false;

  public DataBase() {
    this(EMPTY_NAME, null);
  }

  public DataBase(String name, T data) {
    this(name, data, false);
  }

  public DataBase(T data) {
    this(data, false);
  }

  public DataBase(T data, boolean isConst) {
    this(EMPTY_NAME, data, isConst);
  }

  public DataBase(String name, T data, boolean isConst) {
    this.name = name;
    this.data = data;
    this.isConst = isConst;
  }

  @Override
  public String getName() {
    return name;
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
