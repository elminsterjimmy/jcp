package com.elminster.jcp.ast.statement.declaration;

import com.elminster.jcp.ast.Identifier;

import java.util.List;

/**
 * Interface for struct type declarations.
 * Defines a named type with typed fields.
 */
public interface StructDeclaration extends Declaration {

  /**
   * Get the list of field definitions for this struct.
   * @return list of field definitions
   */
  List<StructFieldDef> getFields();
}
