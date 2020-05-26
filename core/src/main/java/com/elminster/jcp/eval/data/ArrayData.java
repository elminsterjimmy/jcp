package com.elminster.jcp.ast.data;

public class ArrayData<T> extends AnyData<T> {

  protected DataType baseDataType;

  public ArrayData(DataType baseDataType, T data) {
    super(data);
    this.baseDataType = baseDataType;
  }

  public ArrayData(DataType baseDataType, String name, T data) {
    super(name, data);
    this.baseDataType = baseDataType;
  }

  public ArrayData(DataType baseDataType, T data, boolean isConst) {
    super(data, isConst);
    this.baseDataType = baseDataType;
  }

  public ArrayData(DataType baseDataType, String name, T data, boolean isConst) {
    super(name, data, isConst);
    this.baseDataType = baseDataType;
  }

  public DataType getBaseDataType() {
    return baseDataType;
  }

  @Override
  public String getName() {
    return baseDataType.getName() + "[]";
  }
}
