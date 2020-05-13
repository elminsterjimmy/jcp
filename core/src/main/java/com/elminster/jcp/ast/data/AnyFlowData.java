package com.elminster.jcp.ast.data;

public class AnyFlowData<T extends Object> extends FlowDataBase<T> {

  public static AnyFlowData EMPTY = new AnyFlowData(null, true);

  public AnyFlowData(T data) {
    super(data);
  }

  public AnyFlowData(String name, T data) {
    super(name, data);
  }

  public AnyFlowData(T data, boolean isConst) {
    super(data, isConst);
  }

  public AnyFlowData(String name, T data, boolean isConst) {
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
    return DataType.ANY;
  }
}
