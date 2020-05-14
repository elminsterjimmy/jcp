package com.elminster.jcp.ast.data;

public enum DataType {
  ANY,
  STRING,
  BOOLEAN,
  INT,
  VOID
  ;

  public boolean isCastableTo(DataType dataType) {
    if (dataType == this) {
      return true;
    }
    return false;
  }

  public boolean isNumberic() {
    return INT == this;
  }
}
