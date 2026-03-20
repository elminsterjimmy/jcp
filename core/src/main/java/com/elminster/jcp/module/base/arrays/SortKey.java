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
 * Elements whose field is missing or non-navigable sort last regardless of direction.
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

    private final String fieldPath;
    private boolean descending;

    private SortKey(String fieldPath) {
        this.fieldPath = fieldPath;
        this.descending = false;
    }

    /** Creates a sort key for the given field path, ascending by default. */
    public static SortKey by(String fieldPath) {
        return new SortKey(fieldPath);
    }

    /** Sets direction to ascending. Returns {@code this} for chaining. */
    public SortKey asc() {
        this.descending = false;
        return this;
    }

    /** Sets direction to descending. Returns {@code this} for chaining. */
    public SortKey desc() {
        this.descending = true;
        return this;
    }

    /**
     * Returns a {@link Comparator} that compares two elements by this key's field path.
     * Null values (missing or non-navigable fields) sort last in both directions.
     */
    Comparator<Object> toComparator() {
        return (a, b) -> {
            Comparable<Object> va = resolveField(a);
            Comparable<Object> vb = resolveField(b);

            if (va == null && vb == null) return 0;
            if (va == null) return 1;   // nulls last
            if (vb == null) return -1;  // nulls last

            int cmp = va.compareTo(vb);
            return descending ? -cmp : cmp;
        };
    }

    /**
     * Resolves the field path from a struct element.
     * Returns {@code null} if the element is not a {@link StructData}, any path segment is
     * missing, any intermediate node is not a {@link StructData}, or the leaf value is not
     * {@link Comparable}.
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
                return null;
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
