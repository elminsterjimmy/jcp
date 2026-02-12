package com.elminster.jcp.debug;

import com.elminster.jcp.ast.Locatable;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.SourceLocation;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a breakpoint with unique ID and location.
 *
 * <p>Combines identification and location in a single class:
 * <ul>
 *   <li>Unique ID for safe removal operations</li>
 *   <li>Location for matching against AST nodes</li>
 * </ul>
 *
 * <p>Supports two location modes:
 * <ul>
 *   <li>Source location (filepath, line, column) - for user-defined breakpoints</li>
 *   <li>AST node reference - for programmatic use</li>
 * </ul>
 *
 * <p>Thread-safe: Uses atomic counter for ID generation.
 */
public final class Breakpoint {

  private static final AtomicLong counter = new AtomicLong(0);

  private final long id;
  private final String filepath;
  private final int line;
  private final int column;
  private final Node nodeReference;

  private Breakpoint(long id, String filepath, int line, int column, Node nodeReference) {
    this.id = id;
    this.filepath = filepath;
    this.line = line;
    this.column = column;
    this.nodeReference = nodeReference;
  }

  /**
   * Creates a breakpoint at a specific line.
   * Column defaults to 1 (start of line).
   *
   * @param line line number (1-based)
   * @return new Breakpoint
   */
  public static Breakpoint at(int line) {
    return new Breakpoint(counter.incrementAndGet(), null, line, 1, null);
  }

  /**
   * Creates a breakpoint at a specific line and column.
   *
   * @param line   line number (1-based)
   * @param column column number (1-based)
   * @return new Breakpoint
   */
  public static Breakpoint at(int line, int column) {
    return new Breakpoint(counter.incrementAndGet(), null, line, column, null);
  }

  /**
   * Creates a breakpoint at a specific file, line, and column.
   *
   * @param filepath source file path
   * @param line     line number (1-based)
   * @param column   column number (1-based)
   * @return new Breakpoint
   */
  public static Breakpoint at(String filepath, int line, int column) {
    return new Breakpoint(counter.incrementAndGet(), filepath, line, column, null);
  }

  /**
   * Creates a breakpoint at a specific AST node.
   * Uses node's source location if available, otherwise uses identity matching.
   *
   * @param node the AST node to break on
   * @return new Breakpoint
   */
  public static Breakpoint at(Node node) {
    if (node instanceof Locatable) {
      Locatable locatable = (Locatable) node;
      if (locatable.getLocation() != null) {
        SourceLocation loc = locatable.getLocation();
        return new Breakpoint(
            counter.incrementAndGet(),
            loc.getFilepath(),
            loc.getStartLine(),
            loc.getStartColumn(),
            node);
      }
    }
    return new Breakpoint(counter.incrementAndGet(), null, 0, 0, node);
  }

  /**
   * Returns the unique breakpoint ID.
   *
   * @return the ID value
   */
  public long getId() {
    return id;
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
   * Checks if the given node matches this breakpoint.
   *
   * <p>Matching rules:
   * <ol>
   *   <li>If node reference is set and matches, return true</li>
   *   <li>If node has source location, compare filepath (if set), line, and column</li>
   *   <li>Column matching is optional: if breakpoint column is 1, matches any column</li>
   * </ol>
   *
   * @param node the node to check
   * @return true if node matches this breakpoint
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

        // Filepath match (if specified in breakpoint)
        if (filepath != null && !filepath.equals(nodeLoc.getFilepath())) {
          return false;
        }

        // Line must match
        if (nodeLoc.getStartLine() != line) {
          return false;
        }

        // Column match: if breakpoint column is 1, match any column on the line
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
    if (!(o instanceof Breakpoint)) {
      return false;
    }
    Breakpoint that = (Breakpoint) o;
    return id == that.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Breakpoint#").append(id);
    if (nodeReference != null && !hasSourceLocation()) {
      sb.append(" at node:").append(nodeReference.getName());
    } else if (filepath != null) {
      sb.append(" at ").append(filepath).append(":").append(line).append(":").append(column);
    } else if (line > 0) {
      sb.append(" at ").append(line).append(":").append(column);
    }
    return sb.toString();
  }
}
