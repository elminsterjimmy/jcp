package com.elminster.jcp.eval.data;

public interface DataType {

  enum SystemDataType implements DataType {
    ANY("Any", null),
    ANY_ARRAY("Any[]", ANY),
    STRING("String", ANY),
    STRING_ARRAY("String[]", ANY),
    BOOLEAN("Boolean", ANY),
    BOOLEAN_ARRAY("Boolean[]", ANY),
    NUMERIC("Numeric", ANY),
    NUMERIC_ARRAY("Numeric[]", ANY),
    INT("Integer", ANY),
    INT_ARRAY("Integer[]", ANY),
    VOID("Void", ANY),
    ;


    SystemDataType(String name, DataType parent) {
      this.name = name;
      this.parent = parent;
    }

    private String name;
    private DataType parent;

    @Override
    public String getName() {
      return name;
    }

    @Override
    public DataType getParent() {
      return this.parent;
    }
  }

  String getName();

  DataType getParent();

  default boolean isCastableTo(DataType dataType) {
    if (dataType == this) {
      return true;
    }
    if (dataType.getName().equals(getName())) {
      return true;
    }
    if (SystemDataType.ANY == dataType) {
      return true;
    }
    DataType parent = this.getParent();
    do {
      if (dataType == parent) {
        return true;
      }
    } while (null != (parent = parent.getParent()));
    return false;
  }

  default boolean isArray() {
    return this.getName().endsWith("[]");
  }
}
