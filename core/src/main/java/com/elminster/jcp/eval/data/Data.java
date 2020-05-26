package com.elminster.jcp.eval.data;

import com.elminster.jcp.eval.Evaluable;

public interface Data<T> extends Evaluable {

  String getName();

  DataType getDataType();

  boolean isConst();

  T get();

  void set(T data);
}
