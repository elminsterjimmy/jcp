package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.Identifier;

public class ArrayData<T> extends AnyData<T> {

  protected DataType baseDataType;

  public ArrayData(DataType baseDataType, T data) {
    super(data);
    this.baseDataType = baseDataType;
  }

  public ArrayData(DataType baseDataType, Identifier identifier, T data) {
    super(identifier, data);
    this.baseDataType = baseDataType;
  }

  public ArrayData(DataType baseDataType, T data, boolean isConst) {
    super(data, isConst);
    this.baseDataType = baseDataType;
  }

  public ArrayData(DataType baseDataType, Identifier identifier, T data, boolean isConst) {
    super(identifier, data, isConst);
    this.baseDataType = baseDataType;
  }

  public DataType getBaseDataType() {
    return baseDataType;
  }

  @Override
  public Identifier getIdentifier() {
    return Identifier.fromName(baseDataType.getName() + "[]");
  }
}
