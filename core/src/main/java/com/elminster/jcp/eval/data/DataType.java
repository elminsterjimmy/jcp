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
    INT("Integer", NUMERIC),
    INT_ARRAY("Integer[]", ANY),
    DOUBLE("Double", NUMERIC),
    DOUBLE_ARRAY("Double[]", ANY),
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

  /**
   * Check if this type can be promoted (widened) to the target type.
   * Type promotion is different from casting: it handles numeric widening
   * conversions like int → double.
   *
   * @param target the target data type
   * @return true if this type can be promoted to the target type
   * @see TypePromotion
   */
  default boolean isTypePromotableTo(DataType target) {
    return TypePromotion.isWideningAllowed(this, target);
  }

  /**
   * Check if this type is compatible with the target type for function calls.
   * Combines both hierarchy-based casting and numeric widening promotion.
   *
   * <p>This is the primary compatibility check for parameter matching:
   * <ol>
   *   <li>Hierarchy check: INT is-a NUMERIC (via {@link #isCastableTo})</li>
   *   <li>Widening check: INT → DOUBLE (via {@link #isTypePromotableTo})</li>
   * </ol>
   *
   * @param target the target data type (e.g., parameter type)
   * @return true if this type can be used where target is expected
   */
  default boolean isCompatibleWith(DataType target) {
    return isCastableTo(target) || isTypePromotableTo(target);
  }
}
