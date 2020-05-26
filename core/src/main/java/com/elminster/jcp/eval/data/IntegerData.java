package com.elminster.jcp.ast.data;

public class IntegerData extends AnyData<Integer> {

  public static IntegerData constInt(Integer data) {
    return new IntegerData(data, true);
  }

  public IntegerData(Integer data) {
    super(data);
  }

  public IntegerData(String name, Integer data) {
    super(name, data);
  }

  public IntegerData(Integer data, boolean isConst) {
    super(data, isConst);
  }

  public IntegerData(String name, Integer data, boolean isConst) {
    super(name, data, isConst);
  }

  @Override
  public DataType getDataType() {
    return DataType.SystemDataType.INT;
  }
}
