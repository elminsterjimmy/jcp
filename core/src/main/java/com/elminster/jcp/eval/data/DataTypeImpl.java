package com.elminster.jcp.eval.data;

import static com.elminster.jcp.eval.data.DataType.SystemDataType.ANY;

public class DataTypeImpl implements DataType {

  protected String name;
  protected DataType parentDataType;

  public DataTypeImpl(String name, DataType parentDataType) {
    this.name = name;
    this.parentDataType = parentDataType;
  }

  public DataTypeImpl(String name) {
    this(name, ANY);
  }

  public static DataType newArrayType(String name) {
    return new DataTypeImpl(name + "[]");
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public DataType getParent() {
    return this.parentDataType;
  }

  public String toString() {
    return getName();
  }
}
