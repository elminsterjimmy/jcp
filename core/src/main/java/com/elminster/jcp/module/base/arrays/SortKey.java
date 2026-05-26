package com.elminster.jcp.module.base.arrays;

import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.StructData;

import java.util.Comparator;

/**
 * A sort key for struct array sorting — specifies a field path and direction.
 *
 * <p>Used with {@link Arrays#sort(Object[], SortKey)},
 * {@link Arrays#sort(Object[], SortKey, SortKey)}, and
 * {@link Arrays#sort(Object[], SortKey, SortKey, SortKey)}.
 *
 * <p>Field paths use dot notation for nested structs: {@code "address.city"}.
 * An illegal field path (intermediate segment missing or not a struct) raises
 * {@link IllegalArgumentException} during comparison. Elements that are not
 * {@link StructData} or whose leaf value is missing/non-Comparable sort last.
 *
 * <pre>
 *   SortKey.by("age")               // ascending
 *   SortKey.by("name").desc()       // descending
 *   SortKey.by("address.city").asc()
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

    private SortKey(String fieldPath, Direction direction) {
        this.fieldPath = fieldPath;
        this.direction = direction;
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

    /** Returns a new sort key with ascending direction. */
    public SortKey asc() {
        return new SortKey(this.fieldPath, Direction.ASC);
    }

    /** Returns a new sort key with descending direction. */
    public SortKey desc() {
        return new SortKey(this.fieldPath, Direction.DESC);
    }

    /**
     * Returns a {@link Comparator} that compares two elements by this key's field path.
     *
     * <p>Elements that are not {@link StructData} sort last. Elements whose leaf field is
     * missing or not {@link Comparable} sort last. An illegal field path — one that
     * traverses through a non-struct or references a missing intermediate segment —
     * raises {@link IllegalArgumentException}.
     */
    Comparator<Object> toComparator() {
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

    /**
     * Resolves the field path from a struct element.
     * Returns {@code null} if the element is not a {@link StructData} or the leaf value is
     * missing/non-{@link Comparable}. Throws {@link IllegalArgumentException} if any
     * intermediate segment is missing or not a {@link StructData} — that path is illegal
     * for this element's schema.
     */
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
