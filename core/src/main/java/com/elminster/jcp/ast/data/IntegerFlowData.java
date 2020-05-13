package com.elminster.jcp.ast.data;

public class IntegerFlowData extends AnyFlowData<Integer> {

  public static IntegerFlowData constInt(Integer data) {
    return new IntegerFlowData(data, true);
  }

  public IntegerFlowData(Integer data) {
    super(data);
  }

  public IntegerFlowData(String name, Integer data) {
    super(name, data);
  }

  public IntegerFlowData(Integer data, boolean isConst) {
    super(data, isConst);
  }

  public IntegerFlowData(String name, Integer data, boolean isConst) {
    super(name, data, isConst);
  }

  @Override
  public DataType getDataType() {
    return DataType.INT;
  }
}
