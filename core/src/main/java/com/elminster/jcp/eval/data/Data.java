package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.eval.Evaluable;

public interface Data<T> extends Evaluable {

  Identifier getIdentifier();

  DataType getDataType();

  boolean isConst();

  T get();

  void set(T data);
}
