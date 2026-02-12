package com.elminster.jcp.debug;

import com.elminster.jcp.ast.Locatable;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.SourceLocation;

import java.util.Objects;

/**
 * Represents a breakpoint location using hybrid identification.
 *
 * <p>Supports two identification modes:
 * <ul>
 *   <li>Source location (line/column) - primary, for user-defined breakpoints</li>
 *   <li>AST node reference - fallback, for programmatic use</li>
 * </ul>
 *
 * <p>Matching against nodes checks source location if available,
 * or node identity for node-based breakpoints.
 */
public final class BreakpointLocation {

  private final String filepath;
  private final int line;
  private final int column;
  private final Node nodeReference;

  private BreakpointLocation(String filepath, int line, int column, Node nodeReference) {
    this.filepath = filepath;
    this.line = line;
    this.column = column;
    this.nodeReference = nodeReference;
  }

  /**
   * Creates a breakpoint location at a specific line.
   * Column defaults to 1 (start of line).
   *
   * @param line line number (1-based)
   * @return new BreakpointLocation
   */
  public static BreakpointLocation at(int line) {
    return new BreakpointLocation(null, line, 1, null);
  }

  /**
   * Creates a breakpoint location at a specific line and column.
   *
   * @param line   line number (1-based)
   * @param column column number (1-based)
   * @return new BreakpointLocation
   */
  public static BreakpointLocation at(int line, int column) {
    return new BreakpointLocation(null, line, column, null);
  }

  /**
   * Creates a breakpoint location at a specific file, line, and column.
   *
   * @param filepath source file path
   * @param line     line number (1-based)
   * @param column   column number (1-based)
   * @return new BreakpointLocation
   */
  public static BreakpointLocation at(String filepath, int line, int column) {
    return new BreakpointLocation(filepath, line, column, null);
  }

  /**
   * Creates a breakpoint location at a specific AST node.
   * Uses node's source location if available, otherwise uses identity matching.
   *
   * @param node the AST node to break on
   * @return new BreakpointLocation
   */
  public static BreakpointLocation at(Node node) {
    if (node instanceof Locatable) {
      Locatable locatable = (Locatable) node;
      if (locatable.getLocation() != null) {
        SourceLocation loc = locatable.getLocation();
        return new BreakpointLocation(loc.getFilepath(), loc.getStartLine(), loc.getStartColumn(), node);
      }
    }
    return new BreakpointLocation(null, 0, 0, node);
  }

  /**
   * Returns the filepath, if set.
   *
   * @return filepath or null
   */
  public String getFilepath() {
    return filepath;
  }

  /**
   * Returns the line number.
   *
   * @return line number (1-based, or 0 if node-only breakpoint)
   */
  public int getLine() {
    return line;
  }

  /**
   * Returns the column number.
   *
   * @return column number (1-based, or 0 if node-only breakpoint)
   */
  public int getColumn() {
    return column;
  }

  /**
   * Checks if this is a source-location-based breakpoint.
   *
   * @return true if line number is set
   */
  public boolean hasSourceLocation() {
    return line > 0;
  }

  /**
   * Checks if the given node matches this breakpoint location.
   *
   * <p>Matching rules:
   * <ol>
   *   <li>If node reference is set and matches, return true</li>
   *   <li>If node has source location, compare line (and column if set)</li>
   *   <li>Column matching is optional: if breakpoint column is 1, matches any column on the line</li>
   * </ol>
   *
   * @param node the node to check
   * @return true if node matches this breakpoint location
   */
  public boolean matches(Node node) {
    // Node reference match (identity)
    if (nodeReference != null && nodeReference == node) {
      return true;
    }

    // Source location match
    if (node instanceof Locatable) {
      Locatable locatable = (Locatable) node;
      if (locatable.getLocation() != null) {
        SourceLocation nodeLoc = locatable.getLocation();

        // Filepath match (if specified)
        if (filepath != null && !filepath.equals(nodeLoc.getFilepath())) {
          return false;
        }

        // Line must match
        if (nodeLoc.getStartLine() != line) {
          return false;
        }

        // Column match: if breakpoint column is 1, match any column on the line
        // Otherwise, exact column match required
        if (column > 1 && nodeLoc.getStartColumn() != column) {
          return false;
        }

        return true;
      }
    }

    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BreakpointLocation)) {
      return false;
    }
    BreakpointLocation that = (BreakpointLocation) o;
    return line == that.line
        && column == that.column
        && Objects.equals(filepath, that.filepath)
        && Objects.equals(nodeReference, that.nodeReference);
  }

  @Override
  public int hashCode() {
    return Objects.hash(filepath, line, column);
  }

  @Override
  public String toString() {
    if (nodeReference != null && !hasSourceLocation()) {
      return "node:" + nodeReference.getName();
    }
    if (filepath != null) {
      return filepath + ":" + line + ":" + column;
    }
    return line + ":" + column;
  }
}
