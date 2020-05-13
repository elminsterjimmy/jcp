package com.elminster.jcp.ast.data;

import com.elminster.jcp.eval.Evaluable;

public interface FlowData<T> extends Evaluable {

  String getName();

  DataType getDataType();

  boolean isConst();

  T get();

  void set(T data);
}
