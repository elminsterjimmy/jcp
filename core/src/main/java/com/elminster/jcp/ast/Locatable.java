package com.elminster.jcp.ast;

/**
 * Marks AST nodes that can have source location information.
 *
 * <p>Location may be null for synthetic/generated nodes that don't
 * correspond to specific source code.
 *
 * <p>Per Interface Segregation Principle, this interface is read-only.
 * Location is set via {@code AbstractNode.setLocation()} during AST construction.
 *
 * @see SourceLocation
 * @see AbstractNode
 */
public interface Locatable {

  /**
   * Returns the source location of this node.
   *
   * @return source location, or null if not available (synthetic nodes)
   */
  SourceLocation getLocation();
}
