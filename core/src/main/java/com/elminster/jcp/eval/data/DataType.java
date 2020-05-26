package com.elminster.jcp.ast.data;

public interface DataType {

  enum SystemDataType implements DataType {
    ANY("Any"),
    ANY_ARRAY("Any[]"),
    STRING("String"),
    STRING_ARRAY("String[]"),
    BOOLEAN("Boolean"),
    BOOLEAN_ARRAY("Boolean[]"),
    INT("Integer"),
    INT_ARRAY("Integer[]"),
    VOID("Void"),
    ;

    SystemDataType(String name) {
      this.name = name;
    }

    private String name;

    @Override
    public String getName() {
      return name;
    }
  }

  String getName();

  default boolean isCastableTo(DataType dataType) {
    if (dataType == this) {
      return true;
    }
    return false;
  }

  default boolean isArray() {
    return this.getName().endsWith("[]");
  }
}
