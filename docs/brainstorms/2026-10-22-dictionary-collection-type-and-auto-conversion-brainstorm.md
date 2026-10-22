# Brainstorm: Dictionary Collection Type and Java Collection Auto-Conversion

**Date:** 2026-10-22
**Status:** Ready for planning

## What We're Building

A new base collection type **Dictionary** that represents a key-value store (analogous to `java.util.Map`) in JCP, alongside auto-conversion logic that bridges Java collections (`java.util.Map`, `java.util.List`) to their JCP counterparts (`Dictionary`, `ValueBuffer`) at module function boundaries.

## Why

JCP already has `ValueBuffer` for list/table data, but lacks a key-value collection. When Java methods return a `Map`, it falls through to `ANY` or gets wrapped as an opaque `ExternalClassType` — users can't interact with it naturally. Similarly, `java.util.List` returned from Java has no automatic mapping to `ValueBuffer`.

Non-programmer users need intuitive collection types. "Dictionary" is a universally understood metaphor: look up a word (key) to get its definition (value).

## Scope

### Deliverables

1. **`Dictionary` class** in `module/base/` — wraps `HashMap<Object, Object>` with user-friendly methods:
   - `put(key, value)` — store a value under a key
   - `get(key)` — retrieve a value by key (O(1))
   - `remove(key)` — remove an entry by key
   - `size()` — number of entries
   - `keys()` — all keys
   - `values()` — all values
   - `containsKey(key)` — check if a key exists

2. **Auto-conversion at Java/JCP boundaries:**
   - `java.util.Map` → `Dictionary` when Java methods return a Map
   - `Dictionary` → `java.util.Map` when passing to Java methods expecting a Map
   - `java.util.List` → `ValueBuffer` when Java methods return a List
   - `ValueBuffer` → `java.util.List` when passing to Java methods expecting a List

3. **Registration in base module** — `Dictionary` added to `BaseModuleRegister.classToRegister()` alongside `ValueBuffer`

4. **Tests for both eval and compile modes**

### Out of Scope

- Changing `Dictionary`/`ValueBuffer` to `SystemDataType` — they stay as `ExternalClassType`
- Generic/typed collections (e.g., `Dictionary<String, Int>`) — keys and values are untyped (`Object`)
- New List type — `ValueBuffer` remains the list representation since lists are always used as tables in the real world
- Nested collection auto-conversion (e.g., `Map<String, List<...>>`) — first iteration handles flat collections only

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Name for key-value type | `Dictionary` | Non-programmer friendly; universally understood metaphor |
| Preserve O(1) lookup | Yes, dedicated type wrapping HashMap | Wrapping Map into ValueBuffer would lose O(1), defeating the purpose |
| List representation | Reuse `ValueBuffer` | Users always use lists as tables; no need for a separate simple list type |
| Type system integration | `ExternalClassType` | Consistent with how ValueBuffer works today; simpler than adding to `SystemDataType` |
| Auto-conversion location | Module function boundaries (ClassConverter / TypeCompatibilityUtil) | Natural interception point where Java ↔ JCP values cross |

## Open Questions

1. Should `keys()` and `values()` return JCP arrays (`String[]`, `Any[]`) or `ValueBuffer`?
2. How should auto-conversion handle `null` keys or values in a Map?
3. Should Dictionary support iteration (e.g., for-each over entries)?
4. For `List` → `ValueBuffer` conversion: how to set column headers? Default to index-based (`"0"`, `"1"`, ...`) or single-column (`"value"`)?

## Next Steps

Run `/gw-plan` to create implementation plan.
