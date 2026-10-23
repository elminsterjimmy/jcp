package com.elminster.jcp.module.base.arrays;

import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.StructData;

import java.lang.reflect.Method;
import java.util.Comparator;

/**
 * A sort key for struct array sorting — specifies a field path and direction,
 * or a named comparator function.
 *
 * <p>Used with {@link Arrays#sort(Object[], SortKey)},
 * {@link Arrays#sort(Object[], SortKey, SortKey)}, and
 * {@link Arrays#sort(Object[], SortKey, SortKey, SortKey)}.
 *
 * <p><strong>Field-path mode</strong> (existing): dot notation for nested structs.
 * <pre>
 *   SortKey.by("age")               // ascending
 *   SortKey.by("name").desc()       // descending
 *   SortKey.by("address.city").asc()
 * </pre>
 *
 * <p><strong>Function-name mode</strong> (new): delegates comparison to a named function.
 * <pre>
 *   SortKey.sortByFunc("myCompare")                    // eval mode
 *   SortKey.sortByFunc("myCompare", MyClass.class)     // compile mode
 * </pre>
 *
 * @author jgu
 * @version 1.0
 */
public class SortKey {

    /** Sort direction. */
    public enum Direction {
        ASC, DESC
    }

    private final String fieldPath;
    private final Direction direction;
    private final String functionName;
    private final Class<?> declaringClass;
    private final Comparator<Object> resolvedComparator;

    private SortKey(String fieldPath, Direction direction,
                    String functionName, Class<?> declaringClass,
                    Comparator<Object> resolvedComparator) {
        this.fieldPath = fieldPath;
        this.direction = direction;
        this.functionName = functionName;
        this.declaringClass = declaringClass;
        this.resolvedComparator = resolvedComparator;
    }

    private SortKey(String fieldPath, Direction direction) {
        this(fieldPath, direction, null, null, null);
    }

    /**
     * Creates a sort key for the given field path, ascending by default.
     *
     * @throws IllegalArgumentException if {@code fieldPath} is null or empty
     */
    public static SortKey by(String fieldPath) {
        if (fieldPath == null || fieldPath.isEmpty()) {
            throw new IllegalArgumentException("fieldPath must not be null or empty");
        }
        return new SortKey(fieldPath, Direction.ASC);
    }

    /**
     * Creates a function-name mode sort key (eval mode — resolved later via EvalContext).
     *
     * @param functionName the name of the comparator function
     * @throws IllegalArgumentException if {@code functionName} is null or empty
     */
    public static SortKey sortByFunc(String functionName) {
        if (functionName == null || functionName.isEmpty()) {
            throw new IllegalArgumentException("functionName must not be null or empty");
        }
        return new SortKey(null, Direction.ASC, functionName, null, null);
    }

    /**
     * Creates a function-name mode sort key (compile mode — resolved via reflection at runtime).
     *
     * @param functionName   the name of the comparator function
     * @param declaringClass the class containing the comparator method
     * @throws IllegalArgumentException if either argument is null
     */
    public static SortKey sortByFunc(String functionName, Class<?> declaringClass) {
        if (functionName == null || functionName.isEmpty()) {
            throw new IllegalArgumentException("functionName must not be null or empty");
        }
        if (declaringClass == null) {
            throw new IllegalArgumentException("declaringClass must not be null");
        }
        return new SortKey(null, Direction.ASC, functionName, declaringClass, null);
    }

    /** Returns a new sort key with ascending direction. */
    public SortKey asc() {
        return new SortKey(this.fieldPath, Direction.ASC,
                this.functionName, this.declaringClass, this.resolvedComparator);
    }

    /** Returns a new sort key with descending direction. */
    public SortKey desc() {
        return new SortKey(this.fieldPath, Direction.DESC,
                this.functionName, this.declaringClass, this.resolvedComparator);
    }

    /** Returns the function name, or {@code null} if this is a field-path mode key. */
    public String getFunctionName() {
        return functionName;
    }

    /**
     * Returns a copy of this sort key with a pre-resolved comparator (eval mode).
     * The original instance is not modified.
     */
    public SortKey withResolvedComparator(Comparator<Object> comparator) {
        return new SortKey(this.fieldPath, this.direction,
                this.functionName, this.declaringClass, comparator);
    }

    /**
     * Returns a {@link Comparator} for this sort key.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Pre-resolved comparator (eval mode, set via {@link #withResolvedComparator})</li>
     *   <li>Reflection-based resolution (compile mode, requires {@code declaringClass})</li>
     *   <li>Field-path comparator (existing behavior)</li>
     * </ol>
     */
    public Comparator<Object> toComparator() {
        if (resolvedComparator != null) {
            return resolvedComparator;
        }
        if (functionName != null && declaringClass != null) {
            return buildReflectionComparator();
        }
        if (functionName != null) {
            throw new IllegalStateException(
                "Function-name SortKey not resolved — use withResolvedComparator or provide declaringClass");
        }
        return buildFieldPathComparator();
    }

    private Comparator<Object> buildReflectionComparator() {
        Method method;
        try {
            method = declaringClass.getMethod(functionName, Object.class, Object.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                "Comparator function '" + functionName + "' not found in " + declaringClass.getName(), e);
        }
        if (method.getReturnType() != int.class && method.getReturnType() != Integer.class) {
            throw new IllegalStateException(
                "Comparator function '" + functionName + "' must return int, got: " + method.getReturnType().getName());
        }
        return (a, b) -> {
            try {
                return (int) method.invoke(null, a, b);
            } catch (Exception e) {
                throw new RuntimeException("Error invoking comparator function '" + functionName + "'", e);
            }
        };
    }

    private Comparator<Object> buildFieldPathComparator() {
        return (a, b) -> {
            Comparable<Object> va = resolveField(a);
            Comparable<Object> vb = resolveField(b);

            if (va == null && vb == null) return 0;
            if (va == null) return 1;   // nulls last
            if (vb == null) return -1;  // nulls last

            int cmp = va.compareTo(vb);
            return direction == Direction.DESC ? -cmp : cmp;
        };
    }

    @SuppressWarnings("unchecked")
    private Comparable<Object> resolveField(Object element) {
        if (!(element instanceof StructData)) {
            return null;
        }
        String[] segments = fieldPath.split("\\.");
        StructData current = (StructData) element;

        for (int i = 0; i < segments.length - 1; i++) {
            Data field = current.getField(segments[i]);
            if (field == null || !(field.get() instanceof StructData)) {
                throw new IllegalArgumentException(
                    "Illegal field path '" + fieldPath + "': segment '" + segments[i]
                        + "' is missing or not a struct");
            }
            current = (StructData) field.get();
        }

        Data leaf = current.getField(segments[segments.length - 1]);
        if (leaf == null || !(leaf.get() instanceof Comparable)) {
            return null;
        }
        return (Comparable<Object>) leaf.get();
    }
}
