package com.elminster.jcp.eval.data;

public class AnyData<T extends Object> extends DataBase<T> {

  public static AnyData EMPTY = new AnyData(null, true);

  public AnyData() {
    super(null);
  }

  public AnyData(T data) {
    super(data);
  }

  public AnyData(String name, T data) {
    super(name, data);
  }

  public AnyData(T data, boolean isConst) {
    super(data, isConst);
  }

  public AnyData(String name, T data, boolean isConst) {
    super(name, data, isConst);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "name=" + name +
        ", data=" + data +
        ", isConst=" + isConst +
        '}';
  }

  @Override
  public DataType getDataType() {
    return DataType.SystemDataType.ANY;
  }
}
