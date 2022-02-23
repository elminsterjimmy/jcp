package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.Identifier;

public class StringData extends AnyData<String> {

  public StringData(Identifier identifier, String data, boolean isConst) {
    super(identifier, data, isConst);
  }

  public StringData(String data) {
    super(data);
  }

  public StringData(String data, boolean isConst) {
    super(data, isConst);
  }

  public static Data newString(String string) {
    return new StringData(string);
  }

  @Override
  public DataType getDataType() {
    return DataType.SystemDataType.STRING;
  }
}
