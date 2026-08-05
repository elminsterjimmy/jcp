package com.elminster.jcp.module.base.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Simple stdio operations for the JCP base module.
 *
 * <p>All methods are static. {@code print}/{@code println} expose typed overloads for
 * {@code int}, {@code double}, {@code boolean}, and {@code String} so the exact-match
 * overload resolver (see {@link com.elminster.jcp.module.base.math.Math}) picks the
 * right Java overload without widening. {@code readLine()} returns the next line from
 * stdin as a {@code String}, or {@code ""} at end-of-stream.
 *
 * <p>A single lazily-initialised {@link BufferedReader} wraps {@code System.in}. Creating
 * a new reader per call would buffer ahead and silently discard characters between
 * successive {@code readLine()} calls. This class assumes single-threaded stdin access.
 *
 * <p>Callable from JCP programs as:
 * <pre>
 *   IO.print(42)          // writes "42" to stdout, no newline
 *   IO.println("hello")   // writes "hello\n" to stdout
 *   IO.readLine()         // reads one line from stdin
 * </pre>
 */
public class IO {

    private IO() {}

    private static BufferedReader reader;

    private static BufferedReader reader() {
        if (reader == null) {
            reader = new BufferedReader(new InputStreamReader(System.in));
        }
        return reader;
    }

    /** For tests only — resets the shared stdin reader so a new System.in is picked up. */
    public static void resetReaderForTest() {
        reader = null;
    }

    public static void print(int v)     { System.out.print(v); }
    public static void print(double v)  { System.out.print(v); }
    public static void print(boolean v) { System.out.print(v); }
    public static void print(String v)  { System.out.print(v); }

    public static void println(int v)     { System.out.println(v); }
    public static void println(double v)  { System.out.println(v); }
    public static void println(boolean v) { System.out.println(v); }
    public static void println(String v)  { System.out.println(v); }

    public static String readLine() {
        try {
            String line = reader().readLine();
            return line == null ? "" : line;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
