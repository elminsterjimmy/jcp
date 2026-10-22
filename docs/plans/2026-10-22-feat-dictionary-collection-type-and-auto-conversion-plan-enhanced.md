---
title: "feat: Dictionary collection type and Java collection auto-conversion (Enhanced)"
type: feat
date: 2026-10-22
issue: 84
review_date: 2026-10-22
original_plan: docs/plans/2026-10-22-feat-dictionary-collection-type-and-auto-conversion-plan.md
---

# feat: Dictionary collection type and Java collection auto-conversion (Enhanced)

> **Note:** This is an enhanced version of the original plan, incorporating feedback from the plan review conducted on 2026-10-22.

## Overview

Add a `Dictionary` class to the base module as JCP's key-value collection type (wrapping `HashMap<Object, Object>`), and implement automatic conversion between Java collections (`Map`/`List`) and their JCP counterparts (`Dictionary`/`ValueBuffer`) at module-function boundaries.

This fills a gap: `ValueBuffer` exists for tabular data, but there is no native key-value collection. When Java methods return a `Map`, it currently falls through to `ANY` or becomes an opaque `ExternalClassType` — users cannot call `get(key)` or `put(key, value)` on it.

## Technical Approach

### Dictionary class design

Follow the same pattern as `ValueBuffer` — a plain Java class in `module/base/dict/` that wraps a `HashMap<Object, Object>`:

**Location:** `core/src/main/java/com/elminster/jcp/module/base/dict/Dictionary.java`

**Constructors:**
- `Dictionary()` — creates an empty dictionary (no-arg, registered as `Dictionary.new()`)
- `Dictionary(Map<?, ?> source)` — creates a dictionary from an existing Java Map (used by auto-conversion; copies entries rather than holding a reference, to avoid aliasing surprises)

**Instance methods:**
- `put(Object key, Object value)` — store a value under a key
- `get(Object key)` — retrieve a value by key (O(1)); returns null if key absent
- `remove(Object key)` — remove an entry by key
- `size()` — number of entries (returns `int`)
- `containsKey(Object key)` — check if a key exists (returns `boolean`)
- `keys()` — returns `ValueBuffer` (1 column: header `"key"`) — represents the key set without exposing Java's `Set<K>`
- `values()` — returns `ValueBuffer` (1 column: header `"value"`) — represents the value collection without exposing Java's `Collection<V>`
- `entries()` — returns `ValueBuffer` (2 columns: headers `"key"`, `"value"`) — represents the entry set without exposing Java's `Map.Entry`
- `toMap()` — returns a **copy** of the backing `HashMap<Object, Object>` (for outbound Java interop; copy prevents aliasing)

**Design decisions:**
- `keys()`, `values()`, and `entries()` all return `ValueBuffer` — Java's `Set<K>`, `Collection<V>`, and `Set<Map.Entry>` have no JCP equivalents. ValueBuffer is the natural JCP collection type for tabular data, and these methods produce tabular views of the dictionary's contents.
- `toMap()` returns a copy, not the backing map, so Java code cannot mutate the Dictionary's internal state behind its back.
- Dictionary wraps HashMap, does NOT extend it — extending would cascade registration of `Set<K>`, `Collection<V>`, `Map.Entry`, `BiConsumer`, etc. through `CompileModeClassConverter`/`ClassConverter`. No Java Map interfaces leak into JCP.

### Auto-conversion at Java/JCP boundaries (eval mode)

#### Return-value wrapping

**Location:** Inside the anonymous `doFunction()` closures in `ClassConverter.registerStaticMethod()` (line ~159) and `registerNonStaticMethod()` (line ~262).

Currently these return `new AnyData(result, returnDataType)`. Add conversion before wrapping:

```
// Inside doFunction, after ReflectUtil.invoke / method.invoke:
if (result instanceof java.util.Map && !(result instanceof Dictionary)) {
    Dictionary dict = new Dictionary((Map<?, ?>) result);
    // Look up the Dictionary DataType from the context
    return new AnyData(dict, dictionaryDataType);
}
if (result instanceof java.util.List) {
    ValueBuffer vb = ValueBuffer.fromList((List<?>) result);
    return new AnyData(vb, valueBufferDataType);
}
return new AnyData(result, returnDataType);
```

The `dictionaryDataType` and `valueBufferDataType` are looked up from the `evalContext` at call time (not registration time) to avoid ordering issues.

**Null handling:** If the Java method returns `null`, no conversion is applied — `new AnyData(null, returnDataType)` is returned as-is. A null Map does not become an empty Dictionary.

#### Argument unwrapping

**Critical fix from review:** The plan originally proposed modifying `ClassConverter.getArgumentValues()`, but that static utility has no access to the Java Method's parameter types. Instead, argument conversion happens inside each `doFunction` closure where the captured `method` reference is available:

```
// Inside doFunction, before invoking the Java method:
Class<?>[] javaParamTypes = method.getParameterTypes();
Object[] argValues = new Object[arguments.length];
for (int i = 0; i < arguments.length; i++) {
    Object val = arguments[i].get();
    if (val instanceof Dictionary && Map.class.isAssignableFrom(javaParamTypes[i])) {
        argValues[i] = ((Dictionary) val).toMap();
    } else if (val instanceof ValueBuffer && List.class.isAssignableFrom(javaParamTypes[i])) {
        argValues[i] = ((ValueBuffer) val).toList();
    } else {
        argValues[i] = val;
    }
}
```

This replaces the current `getArgumentValues(arguments)` call. The existing `getArgumentValues` remains for backwards compatibility with any internal callers, but the `doFunction` closures use inline conversion.

### Compile mode — Dictionary as ExternalClassType (no auto-conversion needed)

Since `Dictionary` is a plain Java class loaded into the JVM, its instances cross the Java boundary naturally in compile mode. A JCP program can call `Dictionary.new()`, `dict.put(key, value)`, etc. via the standard `ExternalClassType` method compilation path.

Bytecode-level auto-conversion (emitting `CHECKCAST` + factory calls when Java methods return `Map`/`List`) is deferred to a future issue. For now, compile-mode users must manually wrap/unwrap if needed.

### Registration order in BaseModuleRegister

Add `Dictionary.class` to `BaseModuleRegister.classToRegister()` right after `ValueBuffer.class`. Dictionary has no dependencies on other base-module types (all methods take/return `Object` → `ANY`), so ordering is low-risk.

### Resolved open questions

| Question | Resolution | Rationale |
|----------|-----------|-----------|
| `keys()` / `values()` return types | `ValueBuffer` (1 col) / `ValueBuffer` (1 col) | Reuses JCP's existing collection type; avoids exposing Java Set/Collection |
| `entries()` return type | `ValueBuffer` (2 cols: key, value) | Tabular representation of entry set; avoids exposing Java Map.Entry |
| Null keys/values in Map | Pass through as-is | HashMap allows null; Dictionary inherits this |
| Dictionary iteration | Out of scope | Users call `keys()` + loop over ValueBuffer; for-each requires AST changes |
| List → ValueBuffer headers | Single column: `["value"]` | Consistent, simple; users rename headers if needed |
| Null return from Java Map method | Returns null, not empty Dictionary | Preserve Java semantics; null means "no result" |

## Implementation Phases

### Phase 1: Dictionary class and registration

**Goal:** Dictionary usable as a standalone collection in both eval and compile modes.

**Deliverables:**
- [ ] Create `Dictionary.java` at `core/src/main/java/com/elminster/jcp/module/base/dict/Dictionary.java`
- [ ] Implement: no-arg constructor, `Dictionary(Map<?, ?>)`, `put(Object, Object)`, `get(Object)`, `remove(Object)`, `size()`, `containsKey(Object)`, `keys()`, `values()`, `entries()`, `toMap()`
- [ ] Add `Dictionary.class` to `BaseModuleRegister.classToRegister()` after `ValueBuffer.class`
- [ ] Write eval-mode tests: `core/src/test/java/com/elminster/jcp/eval/function/DictionaryEvalTest.java`
  - Construct via `Dictionary.new()`
  - `put` + `get` round-trip
  - `remove` + `size` verification
  - `containsKey` true/false
  - `keys()`, `values()`, and `entries()` return ValueBuffer with correct headers and content
- [ ] Write compile-mode tests: `core/src/test/java/com/elminster/jcp/compile/module/DictionaryCompileTest.java`
  - Construct via `Dictionary.new()`
  - `put` + `get` + `size` (mirror `ValueBufferCompileTest` pattern)
- [ ] Verify `base::Dictionary.new()` module-qualified form works in eval mode

**Phase acceptance:** All Dictionary methods work in both eval and compile modes; existing tests pass.

### Phase 2: Auto-conversion — Map ↔ Dictionary (eval mode)

**Goal:** Java methods returning `Map` produce a `Dictionary`; JCP `Dictionary` passed to Java methods expecting `Map` unwraps.

**Deliverables:**
- [ ] Create test fixture: `core/src/test/java/com/elminster/jcp/eval/function/DictionaryAutoConvertFixture.java`
  - `static Map<String, Integer> getScores()` — returns a populated Map
  - `static Map<Integer, String> getIndexedNames()` — returns a Map with non-String keys
  - `static String describeMap(Map<String, Integer> m)` — accepts a Map, returns a summary string
  - `static Map<String, Integer> emptyMap()` — returns empty Map
  - `static Map<String, Integer> nullMap()` — returns null
- [ ] Modify `ClassConverter.registerStaticMethod` `doFunction` closure: add return-value conversion (`Map` → `Dictionary`)
- [ ] Modify `ClassConverter.registerNonStaticMethod` `doFunction` closure: add return-value conversion
- [ ] Modify both `doFunction` closures: add argument unwrapping (`Dictionary` → `Map`) using captured `method.getParameterTypes()`
- [ ] Write tests: `core/src/test/java/com/elminster/jcp/eval/function/DictionaryAutoConvertTest.java`
  - Java Map return → Dictionary (verify `get`, `size`)
  - Dictionary → Java Map argument (verify round-trip)
  - Non-String keys preserved correctly
  - Empty Map → empty Dictionary
  - Null Map return → null (not empty Dictionary)
  - Existing module functions not affected (regression test)

**Phase acceptance:** Map ↔ Dictionary auto-conversion works transparently in eval mode.

### Phase 3: Auto-conversion — List ↔ ValueBuffer (eval mode)

**Goal:** Java methods returning `List` produce a `ValueBuffer`; JCP `ValueBuffer` passed to Java methods expecting `List` unwraps.

**Deliverables:**
- [ ] Add `ValueBuffer.fromList(List<?>)` static factory: creates single-column VB with header `["value"]`
- [ ] Add `ValueBuffer.toList()` instance method: extracts first column values into `List<Object>`
- [ ] Create test fixture: `core/src/test/java/com/elminster/jcp/eval/function/ValueBufferAutoConvertFixture.java`
  - `static List<String> getNames()` — returns a populated List
  - `static String describeList(List<String> items)` — accepts a List, returns a summary
  - `static List<String> emptyList()` — returns empty List
  - `static List<String> nullList()` — returns null
- [ ] Modify `ClassConverter.registerStaticMethod` / `registerNonStaticMethod` `doFunction` closures: add `List` → `ValueBuffer` return conversion
- [ ] Modify both `doFunction` closures: add `ValueBuffer` → `List` argument unwrapping
- [ ] Write tests: `core/src/test/java/com/elminster/jcp/eval/function/ValueBufferAutoConvertTest.java`
  - Java List return → ValueBuffer (verify row count, header)
  - ValueBuffer → Java List argument (verify round-trip)
  - Empty List → empty ValueBuffer
  - Null List return → null
  - Multi-column VB passed to List param: `toList()` extracts first column

**Phase acceptance:** List ↔ ValueBuffer auto-conversion works transparently in eval mode.

### Phase 4: Coverage, edge cases, and regression verification

**Goal:** Maintain JaCoCo ≥ 85% instruction and 85% branch coverage; ensure no regressions.

**Deliverables:**
- [ ] Run `mvn verify -pl core` and confirm JaCoCo thresholds pass
- [ ] Test edge cases not covered in phases 1-3:
  - Large collections (1000+ entries) — performance sanity check
  - Map with null values: `put("key", null)` → `get("key")` returns null, not missing
  - Dictionary `toMap()` returns a copy: mutating the copy does not affect the Dictionary
  - Concurrent access (document as unsupported, no synchronization)
- [ ] Run full test suite (`mvn test`) and verify zero regressions
- [ ] Run compile-mode auto-conversion smoke test (manual): compile-mode users see Dictionary as a normal ExternalClassType

**Phase acceptance:** JaCoCo ≥ 85% both metrics; full test suite green; edge cases documented.

## Acceptance Criteria

### Functional Requirements

- [ ] `Dictionary.new()` creates an empty dictionary in both eval and compile modes
- [ ] `Dictionary(Map)` constructor copies entries from the source Map
- [ ] `put(key, value)`, `get(key)`, `remove(key)`, `size()`, `containsKey(key)`, `keys()`, `values()`, `entries()` all work correctly
- [ ] `keys()` returns `ValueBuffer` (1 col: `"key"`), `values()` returns `ValueBuffer` (1 col: `"value"`), `entries()` returns `ValueBuffer` (2 cols: `"key"`, `"value"`)
- [ ] `toMap()` returns a copy of the backing HashMap
- [ ] Java methods returning `java.util.Map` produce a `Dictionary` in eval mode
- [ ] Java methods returning `null` for a Map type produce null (not empty Dictionary)
- [ ] JCP `Dictionary` passed to Java methods expecting `Map` unwraps via `toMap()`
- [ ] Java methods returning `java.util.List` produce a `ValueBuffer` in eval mode
- [ ] JCP `ValueBuffer` passed to Java methods expecting `List` unwraps via `toList()`
- [ ] `base::Dictionary.new()` module-qualified form works
- [ ] Existing module functions (Strings, Math, Arrays, etc.) not affected by auto-conversion

### Non-Functional Requirements

- [ ] JaCoCo ≥ 85% instruction and 85% branch coverage maintained
- [ ] No regressions in existing tests (all green after `mvn test`)
- [ ] Dictionary backed by HashMap preserves O(1) lookup
- [ ] Auto-conversion overhead: one `instanceof` check per boundary crossing

## Dependencies

- `ValueBuffer` class and its registration pattern (`core/.../module/base/vb/ValueBuffer.java`)
- `BaseModuleRegister.classToRegister()` ordering mechanism
- `ClassConverter` (eval mode) — specifically the `doFunction` closures in `registerStaticMethod` and `registerNonStaticMethod`
- `ExternalClassType` for Dictionary's type representation
- `ValueBuffer` is a return type of `keys()`, `values()`, and `entries()` — Dictionary depends on ValueBuffer, so registration order matters (ValueBuffer before Dictionary in `BaseModuleRegister`)

## Testing Strategy

### Test fixture classes (Java helpers for eval-mode auto-conversion tests)

| Fixture | Location | Purpose |
|---------|----------|---------|
| `DictionaryAutoConvertFixture` | `core/src/test/java/.../eval/function/` | Java methods returning/accepting `Map` |
| `ValueBufferAutoConvertFixture` | `core/src/test/java/.../eval/function/` | Java methods returning/accepting `List` |

### Test classes

| Test class | Mode | Coverage |
|------------|------|----------|
| `DictionaryEvalTest` | Eval | Dictionary construction, all methods |
| `DictionaryCompileTest` | Compile | Dictionary construction, put/get/size |
| `DictionaryAutoConvertTest` | Eval | Map ↔ Dictionary auto-conversion |
| `ValueBufferAutoConvertTest` | Eval | List ↔ ValueBuffer auto-conversion |

### Regression strategy

Run the full existing test suite before and after each phase. Key regression risk areas:
- `ValueBufferCompileTest` / `ValueBufferTest` — ValueBuffer behavior unchanged
- `StaticMethodCallCompilerTest` — compile-mode external calls unaffected
- `FunCallEvaluatorTest` — eval-mode function resolution unchanged

## Known Learnings Applied

- `docs/solutions/runtime-errors/50-type-bridge-isolation-report.md` — **D5 (mapJavaTypeToDataType ANY-collapse):** Dictionary methods with `Object` parameters correctly map to `ANY` via the `Object.class` fast-path. Auto-conversion operates at the runtime value level, not the type-mapping level.

- **D6 (ClassConverter NPE on unknown param types):** Dictionary's `Object`-typed parameters safely resolve via `getDataType(Object.class, ...)` → `SystemDataType.ANY`.

- **Overload resolution (issue #43 / PR #63):** `Dictionary.get(Object)` and `Dictionary.put(Object, Object)` use `ANY`-typed parameters, so no overload ambiguity risk.

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Auto-conversion breaks existing module functions that intentionally return raw Map/List | Medium | High | Only convert when result is `instanceof Map`/`List` and is NOT already a Dictionary/ValueBuffer. Existing base-module functions (Strings, Math, etc.) don't return Map/List, so no clash. Add regression tests for all base-module function classes. |
| Dictionary registration order causes circular dependency | Low | Low | Dictionary methods take/return `Object` (→ `ANY`); no class-type dependencies. Confirmed by reviewing `ClassConverter` circular-ref guard. |
| Non-String keys in auto-converted Maps | Low | Low | `keys()` returns `ValueBuffer` wrapping `Object` values — no ClassCastException. Users retrieve keys from ValueBuffer rows. |
| Map subclass ordering lost (LinkedHashMap → HashMap copy) | Low | Medium | `Dictionary(Map)` constructor uses `new HashMap<>(source)`, which loses insertion order. Document this. If ordering matters, users should access the raw Map via a non-converting path. |
| Argument unwrapping in `doFunction` adds complexity to ClassConverter closures | Low | Medium | Extract conversion logic into a private static helper method `convertArgumentsForJava(Data[], Method)` inside ClassConverter, keeping closures readable. |
| `ValueBuffer.toList()` lossy for multi-column VBs | Low | Medium | Document that `toList()` extracts first column only. Multi-column VBs should not be passed to methods expecting `List` — the type system won't prevent it, but the behavior is documented. |
| JaCoCo coverage drops below 85% | Medium | High | Phase 4 is dedicated to coverage. Write tests for every method and every conversion branch (including null, empty, and error paths). |

## Changes from Original Plan

**Improvements made based on review feedback:**

1. **Fixed argument-unwrapping approach** — Moved conversion from `getArgumentValues()` (which lacks access to Java param types) into `doFunction` closures where the captured `method` reference provides `method.getParameterTypes()`.
2. **Changed `keys()`, `values()` return types to `ValueBuffer`** — Instead of `Object[]`/`String[]`, all collection-returning methods (`keys()`, `values()`, `entries()`) return `ValueBuffer` with appropriate column headers. This reuses JCP's existing collection type and avoids exposing Java's `Set`, `Collection`, or `Map.Entry` into the JCP type system.
3. **Added `entries()` method** — Returns a 2-column ValueBuffer (`"key"`, `"value"`) as the JCP equivalent of Java's `entrySet()`.
4. **Added `Dictionary(Map<?, ?>)` constructor** — Enables efficient auto-conversion without re-inserting entries one by one.
5. **Added testing strategy section** — Specific test class names, locations, and fixture classes for auto-conversion tests.
6. **Added null-return handling** — Documented that null Map/List returns are NOT converted to empty Dictionary/ValueBuffer.
7. **Added risks for Map-subclass ordering loss** — Risk missed in original plan.
8. **Clarified `toMap()` returns a copy** — Prevents aliasing bugs where Java code mutates Dictionary internals.
9. **Consolidated `toMap()` deliverable** — Was listed in both Phase 1 and Phase 2; now only in Phase 1.

## References

- Original plan: `docs/plans/2026-10-22-feat-dictionary-collection-type-and-auto-conversion-plan.md`
- Plan review: `docs/plans/2026-10-22-feat-dictionary-collection-type-and-auto-conversion-plan-review.md`
- Issue: https://github.com/elminsterjimmy/jcp/issues/84
