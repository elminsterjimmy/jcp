package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.Identifier;

public class AnyData<T extends Object> extends DataBase<T> {

  public static AnyData EMPTY = new AnyData(null, true);

  public AnyData() {
    super(null);
  }

  public AnyData(T data) {
    super(data);
  }

  public AnyData(Identifier identifier, T data) {
    super(identifier, data);
  }

  public AnyData(T data, boolean isConst) {
    super(data, isConst);
  }

  public AnyData(Identifier identifier, T data, boolean isConst) {
    super(identifier, data, isConst);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "name=" + identifier.getId() +
        ", data=" + data +
        ", isConst=" + isConst +
        '}';
  }

  @Override
  public DataType getDataType() {
    return DataType.SystemDataType.ANY;
  }
}
