package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.Identifier;

public class IntegerData extends AnyData<Integer> {

  public static IntegerData constInt(Integer data) {
    return new IntegerData(data, true);
  }

  public IntegerData(Integer data) {
    super(data);
  }

  public IntegerData(Identifier identifier, Integer data) {
    super(identifier, data);
  }

  public IntegerData(Integer data, boolean isConst) {
    super(data, isConst);
  }

  public IntegerData(Identifier identifier, Integer data, boolean isConst) {
    super(identifier, data, isConst);
  }

  @Override
  public DataType getDataType() {
    return DataType.SystemDataType.INT;
  }
}
