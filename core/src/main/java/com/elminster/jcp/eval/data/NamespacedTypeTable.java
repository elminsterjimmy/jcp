package com.elminster.jcp.eval.data;

import com.elminster.jcp.eval.excpetion.AmbiguousTypeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Per-scope type registry that stores types by their fully-qualified name (FQN) and maintains
 * a simple-name alias index for JCP-visible resolution.
 *
 * <p>This allows two classes with the same simple name but different packages
 * (e.g. {@code java.util.Date} and {@code java.sql.Date}) to coexist without collision.
 *
 * <p>Resolution rules:
 * <ul>
 *   <li>FQN lookup: always returns the exact registered type.</li>
 *   <li>Simple-name lookup: returns the type when exactly one FQN maps to that simple name;
 *       throws {@link AmbiguousTypeException} when multiple FQNs share the name.</li>
 *   <li>Re-registering the same FQN is an idempotent no-op.</li>
 *   <li>System/struct types (no Java class backing) use the simple name as their FQN,
 *       so they register identically in both indexes.</li>
 * </ul>
 */
public final class NamespacedTypeTable {

    /** FQN → DataType; exact, collision-free. */
    private final Map<String, DataType> byFqn = new HashMap<>();

    /** simple name → set of FQNs that map to it; used for ambiguity detection. */
    private final Map<String, Set<String>> aliasBySimpleName = new HashMap<>();

    /**
     * Register a type. Idempotent for same-FQN re-registration.
     * For {@link ExternalClassType}, the FQN is the Java class's dotted name.
     * For all other types, the FQN equals the simple name.
     *
     * @return {@code true} if the type was newly registered; {@code false} if it was already
     *         present under the same FQN (no-op / duplicate).
     */
    public boolean register(DataType type) {
        String fqn = fqnOf(type);
        String simpleName = type.getName();

        // Idempotent: same FQN already present → no-op
        if (byFqn.containsKey(fqn)) {
            return false;
        }

        byFqn.put(fqn, type);
        aliasBySimpleName.computeIfAbsent(simpleName, k -> new HashSet<>()).add(fqn);
        return true;
    }

    /**
     * Look up a type by its FQN.
     *
     * @return the DataType, or {@code null} if not found
     */
    public DataType getByFqn(String fqn) {
        return byFqn.get(fqn);
    }

    /**
     * Look up a type by its simple name.
     *
     * <p>Resolution rules when multiple FQNs share a simple name:
     * <ol>
     *   <li>If exactly one candidate is a non-{@link ExternalClassType} (struct/system type
     *       declared in JCP source), it wins — JCP-source types shadow imported Java classes.</li>
     *   <li>If all candidates are {@link ExternalClassType}s from different packages, the lookup
     *       is genuinely ambiguous and {@link AmbiguousTypeException} is thrown.</li>
     * </ol>
     *
     * @return the DataType when unambiguously resolvable, or {@code null} if not found
     * @throws AmbiguousTypeException when multiple external classes share the simple name
     */
    public DataType getBySimpleName(String simpleName) {
        Set<String> fqns = aliasBySimpleName.getOrDefault(simpleName, Collections.emptySet());
        if (fqns.isEmpty()) {
            return null;
        }
        if (fqns.size() == 1) {
            return byFqn.get(fqns.iterator().next());
        }
        // Multiple FQNs: prefer a non-ExternalClassType (struct/system) over external Java classes.
        List<DataType> nonExternal = new ArrayList<>();
        List<DataType> external = new ArrayList<>();
        for (String fqn : fqns) {
            DataType dt = byFqn.get(fqn);
            if (dt instanceof ExternalClassType) {
                external.add(dt);
            } else {
                nonExternal.add(dt);
            }
        }
        if (nonExternal.size() == 1) {
            return nonExternal.get(0);
        }
        // All are ExternalClassType from different packages — genuinely ambiguous.
        StringJoiner candidates = new StringJoiner(", ");
        fqns.forEach(candidates::add);
        throw new AmbiguousTypeException(simpleName, candidates.toString());
    }

    /** Whether this table contains any registration for the given simple name. */
    public boolean containsSimpleName(String simpleName) {
        Set<String> fqns = aliasBySimpleName.get(simpleName);
        return fqns != null && !fqns.isEmpty();
    }

    // -----------------------------------------------------------------------

    private static String fqnOf(DataType type) {
        return type.getFqn();
    }
}
