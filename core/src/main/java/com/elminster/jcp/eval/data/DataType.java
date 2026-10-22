package com.elminster.jcp.eval.data;

import java.util.Set;

public interface DataType extends IAdaptable, ICastable<DataType> {

  // Declaration order matters: TypeCompatibilityUtil.findJcpTypeForJava() returns the first match.
  enum SystemDataType implements DataType {
    ANY("Any", null, Object.class) {
      @Override
      public boolean isAdaptableTo(Class<?> javaType) {
        return true;
      }
    },
    ANY_ARRAY("Any[]", ANY, Object[].class),
    STRING("String", ANY, String.class, CharSequence.class,
            char.class, Character.class),
    STRING_ARRAY("String[]", ANY, String[].class, char[].class),
    BOOLEAN("Boolean", ANY, boolean.class, Boolean.class),
    BOOLEAN_ARRAY("Boolean[]", ANY, boolean[].class, Boolean[].class),
    NUMERIC("Numeric", ANY),
    NUMERIC_ARRAY("Numeric[]", ANY),
    INT("Integer", NUMERIC, int.class, Integer.class,
            byte.class, Byte.class, short.class, Short.class),
    INT_ARRAY("Integer[]", ANY, int[].class, Integer[].class,
            byte[].class, short[].class),
    LONG("Long", NUMERIC, long.class, Long.class),
    LONG_ARRAY("Long[]", ANY, long[].class),
    DOUBLE("Double", NUMERIC, double.class, Double.class,
            float.class, Float.class),
    DOUBLE_ARRAY("Double[]", ANY, double[].class, Double[].class,
            float[].class),
    VOID("Void", ANY, void.class, Void.class),
    ;

    private final String name;
    private final DataType parent;
    private final Set<Class<?>> compatibleJavaTypes;

    SystemDataType(String name, DataType parent, Class<?>... compatibleJavaTypes) {
      this.name = name;
      this.parent = parent;
      this.compatibleJavaTypes = compatibleJavaTypes.length > 0
              ? Set.of(compatibleJavaTypes) : Set.of();
    }

    @Override
    public Set<Class<?>> getCompatibleJavaTypes() {
      return compatibleJavaTypes;
    }

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

  /**
   * Return the fully-qualified name used as the registry key.
   * For system and user-declared types this is the same as {@link #getName()} since
   * they have no Java package. For {@link ExternalClassType} this returns the backing
   * {@code javaClass.getName()} (e.g. {@code "java.util.Date"}).
   */
  default String getFqn() {
    return getName();
  }

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
    if (parent == null) {
      return false;
    }
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

  /**
   * Check if this type is an <em>exact</em> match for the target type, i.e. the same
   * type by name — no hierarchy walk, no numeric widening.
   *
   * <p>Used by overload resolution to prefer an exact-type overload over a
   * widening/hierarchy-compatible one. For example, given {@code abs(int)} and
   * {@code abs(double)}, an INT argument is compatible with both (INT widens to
   * DOUBLE) but is an exact match only for {@code abs(int)}.
   *
   * @param target the target data type (e.g., parameter type)
   * @return true if this type equals the target type by name
   */
  default boolean isExactMatch(DataType target) {
    return getName().equals(target.getName());
  }

  /**
   * Check whether every argument type is an {@link #isExactMatch(DataType) exact match}
   * for the corresponding parameter type. Array lengths must already be equal.
   *
   * <p>Shared by the eval and compile overload resolvers so both narrow candidates
   * identically.
   *
   * @param argTypes   the argument types of a call
   * @param paramTypes the parameter types of a candidate method/function
   * @return true if all positions match exactly by name
   */
  static boolean allExactMatch(DataType[] argTypes, DataType[] paramTypes) {
    if (argTypes.length != paramTypes.length) {
      return false;
    }
    for (int i = 0; i < argTypes.length; i++) {
      if (!argTypes[i].isExactMatch(paramTypes[i])) {
        return false;
      }
    }
    return true;
  }
}
