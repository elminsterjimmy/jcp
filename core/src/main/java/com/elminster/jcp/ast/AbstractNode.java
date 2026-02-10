package com.elminster.jcp.ast;

/**
 * Abstract base class for all AST nodes.
 *
 * <p>Provides common functionality for all nodes including:
 * <ul>
 *   <li>Source location tracking via {@link Locatable}</li>
 *   <li>Default {@link #toString()} implementation</li>
 * </ul>
 *
 * <p>All concrete AST node classes should extend this class to inherit
 * location tracking support.
 */
abstract public class AbstractNode implements Node, Locatable {

  private SourceLocation location;

  @Override
  public SourceLocation getLocation() {
    return location;
  }

  /**
   * Sets the source location for this node.
   *
   * <p>Typically called once during AST construction by the parser.
   *
   * @param location the source location (may be null for synthetic nodes)
   */
  public void setLocation(SourceLocation location) {
    this.location = location;
  }

  @Override
  public String toString() {
    if (location != null) {
      return getName() + " at " + location;
    }
    return getName();
  }
}
