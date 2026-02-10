package com.elminster.jcp.ast;

import java.util.Objects;

/**
 * Immutable source location for AST nodes.
 *
 * <p>Stores complete position range (start to end) plus source content
 * for rich error messages and debugging support.
 *
 * <p>Uses 1-based line and column indexing.
 * Thread-safe due to immutability.
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Single position
 * SourceLocation loc = SourceLocation.of("file.jcp", 10, 5);
 *
 * // Position with source content
 * SourceLocation loc = SourceLocation.of("file.jcp", 10, 5, "int x = 5;");
 *
 * // Full range with source content
 * SourceLocation loc = SourceLocation.span("file.jcp", 10, 5, 10, 15, "int x = 5;");
 *
 * // Simple format for logs
 * System.out.println(loc);  // "file.jcp:10:5"
 *
 * // Detailed format with source context
 * System.out.println(loc.formatWithSource());
 * // file.jcp:10:5
 * //   10 | int x = 5;
 * //            ^~~~~
 * }</pre>
 *
 * <h2>ANTLR Integration</h2>
 * <pre>{@code
 * @Override
 * public Expression visitBinaryExpr(BinaryExprContext ctx) {
 *     Expression expr = new Plus(left, right);
 *     Token start = ctx.getStart();
 *     Token stop = ctx.getStop() != null ? ctx.getStop() : start;
 *     String sourceLine = sourceLines.get(start.getLine() - 1);
 *
 *     // ANTLR columns are 0-based, convert to 1-based
 *     expr.setLocation(SourceLocation.span(
 *         currentFile,
 *         start.getLine(),
 *         start.getCharPositionInLine() + 1,
 *         stop.getLine(),
 *         stop.getCharPositionInLine() + stop.getText().length(),
 *         sourceLine
 *     ));
 *     return expr;
 * }
 * }</pre>
 */
public final class SourceLocation {

  private final String filepath;
  private final int startLine;
  private final int startColumn;
  private final int endLine;
  private final int endColumn;
  private final String sourceLineContent;

  private SourceLocation(String filepath, int startLine, int startColumn,
                         int endLine, int endColumn, String sourceLineContent) {
    if (startLine < 1) {
      throw new IllegalArgumentException("startLine must be >= 1, got: " + startLine);
    }
    if (startColumn < 1) {
      throw new IllegalArgumentException("startColumn must be >= 1, got: " + startColumn);
    }
    if (endLine < startLine) {
      throw new IllegalArgumentException(
          "endLine (" + endLine + ") must be >= startLine (" + startLine + ")");
    }
    if (endLine == startLine && endColumn < startColumn) {
      throw new IllegalArgumentException(
          "endColumn (" + endColumn + ") must be >= startColumn (" + startColumn + ") on same line");
    }

    this.filepath = filepath == null ? null : filepath.intern();
    this.startLine = startLine;
    this.startColumn = startColumn;
    this.endLine = endLine;
    this.endColumn = endColumn;
    this.sourceLineContent = sourceLineContent;
  }

  /**
   * Creates a location for a single position.
   *
   * @param filepath source file path (may be null for synthetic nodes)
   * @param line     line number (1-based)
   * @param column   column number (1-based)
   * @return new SourceLocation instance
   */
  public static SourceLocation of(String filepath, int line, int column) {
    return new SourceLocation(filepath, line, column, line, column, null);
  }

  /**
   * Creates a location for a single position with source content.
   *
   * @param filepath      source file path (may be null for synthetic nodes)
   * @param line          line number (1-based)
   * @param column        column number (1-based)
   * @param sourceContent the source line content for error display
   * @return new SourceLocation instance
   */
  public static SourceLocation of(String filepath, int line, int column, String sourceContent) {
    return new SourceLocation(filepath, line, column, line, column, sourceContent);
  }

  /**
   * Creates a location spanning a range with source content.
   *
   * @param filepath      source file path (may be null for synthetic nodes)
   * @param startLine     start line number (1-based)
   * @param startColumn   start column number (1-based)
   * @param endLine       end line number (1-based, must be >= startLine)
   * @param endColumn     end column number (1-based)
   * @param sourceContent the source line content for error display
   * @return new SourceLocation instance
   */
  public static SourceLocation span(String filepath, int startLine, int startColumn,
                                    int endLine, int endColumn, String sourceContent) {
    return new SourceLocation(filepath, startLine, startColumn, endLine, endColumn, sourceContent);
  }

  public String getFilepath() {
    return filepath;
  }

  public int getStartLine() {
    return startLine;
  }

  public int getStartColumn() {
    return startColumn;
  }

  public int getEndLine() {
    return endLine;
  }

  public int getEndColumn() {
    return endColumn;
  }

  public String getSourceLineContent() {
    return sourceLineContent;
  }

  /**
   * Checks if this location represents a range (multi-character or multi-line).
   *
   * @return true if end position differs from start position
   */
  public boolean hasRange() {
    return endLine > startLine || endColumn > startColumn;
  }

  /**
   * Returns simple GCC-style format: "file:line:column".
   *
   * @return location string in format "filepath:line:column" or "line:column" if no filepath
   */
  @Override
  public String toString() {
    if (filepath == null) {
      return startLine + ":" + startColumn;
    }
    return filepath + ":" + startLine + ":" + startColumn;
  }

  /**
   * Formats source context for display.
   *
   * <p>Returns GCC-style location string plus source line with caret indicator.
   * Does not include error level or message - those belong to error handling.
   *
   * <p>Example output:
   * <pre>
   * math.jcp:15:8
   *   15 |   return a / b;
   *               ^~~~~
   * </pre>
   *
   * @return formatted location string with source context, or just location if no source
   */
  public String formatWithSource() {
    StringBuilder sb = new StringBuilder();

    // GCC-style location: file:line:col
    sb.append(toString());

    // Source context if available
    if (sourceLineContent != null) {
      sb.append("\n");

      // Line number gutter
      String lineNum = String.format("%4d | ", startLine);
      sb.append(lineNum).append(sourceLineContent);

      // Caret indicator
      sb.append("\n");
      sb.append(" ".repeat(lineNum.length()));
      sb.append(" ".repeat(startColumn - 1));
      sb.append("^");

      // Extend caret for range on same line
      if (hasRange() && endLine == startLine) {
        int caretLength = endColumn - startColumn - 1;
        if (caretLength > 0) {
          sb.append("~".repeat(caretLength));
        }
      }
    }

    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SourceLocation)) {
      return false;
    }
    SourceLocation that = (SourceLocation) o;
    return startLine == that.startLine
        && startColumn == that.startColumn
        && endLine == that.endLine
        && endColumn == that.endColumn
        && Objects.equals(filepath, that.filepath)
        && Objects.equals(sourceLineContent, that.sourceLineContent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(filepath, startLine, startColumn, endLine, endColumn);
  }
}
