package com.elminster.jcp.ast.statement.declaration;

import com.elminster.jcp.ast.Identifier;

import java.util.List;

/**
 * Interface for type declarations (formerly struct).
 * Defines a named type with fields, constructors, and methods.
 */
public interface StructDeclaration extends Declaration {

  /**
   * Get the list of field definitions for this type.
   * @return list of field definitions
   */
  List<StructFieldDef> getFields();

  /**
   * Get the explicit constructor, or null for auto-generated constructor.
   * @return constructor definition or null
   */
  MethodDef getConstructor();

  /**
   * Get instance methods defined on this type.
   * @return list of instance method definitions
   */
  List<MethodDef> getInstanceMethods();

  /**
   * Get static methods defined on this type.
   * @return list of static method definitions
   */
  List<MethodDef> getStaticMethods();
}
