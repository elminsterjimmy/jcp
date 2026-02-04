package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.Identifier;

public class DoubleData extends AnyData<Double> {

  public static DoubleData constDouble(Double data) {
    return new DoubleData(data, true);
  }

  public DoubleData(Double data) {
    super(data);
  }

  public DoubleData(Identifier identifier, Double data) {
    super(identifier, data);
  }

  public DoubleData(Double data, boolean isConst) {
    super(data, isConst);
  }

  public DoubleData(Identifier identifier, Double data, boolean isConst) {
    super(identifier, data, isConst);
  }

  @Override
  public DataType getDataType() {
    return DataType.SystemDataType.DOUBLE;
  }
}
