---
title: "feat: Dictionary collection type and Java collection auto-conversion"
type: feat
date: 2026-10-22
issue: 84
---

# feat: Dictionary collection type and Java collection auto-conversion

## Overview

Add a `Dictionary` class to the base module as JCP's key-value collection type (wrapping `HashMap<Object, Object>`), and implement automatic conversion between Java collections (`Map`/`List`) and their JCP counterparts (`Dictionary`/`ValueBuffer`) at module-function boundaries.

This fills a gap: `ValueBuffer` exists for tabular data, but there is no native key-value collection. When Java methods return a `Map`, it currently falls through to `ANY` or becomes an opaque `ExternalClassType` — users cannot call `get(key)` or `put(key, value)` on it.

## Technical Approach

### Dictionary class design

Follow the same pattern as `ValueBuffer`:
- Plain Java class in `module/base/` package, registered via `BaseModuleRegister.classToRegister()`
- Instance methods: `put(Object, Object)`, `get(Object)`, `remove(Object)`, `size()`, `containsKey(Object)`, `keys()`, `values()`
- `keys()` returns `String[]` (most dictionaries are string-keyed), `values()` returns `Object[]` — consistent with `ValueBuffer.getHeader()` returning `String[]` and keeping things simple. If the key is not a String, the caller passes the raw key to `get(key)`.
- Constructor: no-arg (`Dictionary.new()`) and optionally a copy-constructor from a `java.util.Map`

### Auto-conversion at Java/JCP boundaries

Two interception points exist, mirrored across eval and compile modes:

**Eval mode — return-value wrapping in `ClassConverter.registerStaticMethod` / `registerNonStaticMethod`:**
The anonymous `AbstractModuleFunction.doFunction()` currently wraps all return values as `new AnyData(result, returnDataType)`. Inject conversion logic here:
- If `result instanceof java.util.Map` and returnDataType is `ExternalClassType` for Map (or ANY): wrap the Map into a new `Dictionary`, then return `AnyData(dictionary, dictionaryDataType)`.
- If `result instanceof java.util.List`: wrap into a `ValueBuffer` (single-column, header `["value"]`), then return `AnyData(valueBuffer, valueBufferDataType)`.

**Eval mode — argument unwrapping in `ClassConverter.getArgumentValues`:**
When a JCP `Dictionary` is passed to a Java method expecting `java.util.Map`, unwrap via `dictionary.toMap()`. Likewise `ValueBuffer` → `java.util.List` via a new `toList()` method.

**Compile mode — bytecode-level conversion is NOT needed for phase 1:**
Since `Dictionary` and `ValueBuffer` are `ExternalClassType` (Java classes loaded into the JVM), their instances cross the Java boundary naturally — `Dictionary` already *is* a Java object. The conversion is only needed when Java methods return raw `Map`/`List` and we want JCP code to interact with them via Dictionary/ValueBuffer methods. In compile mode, the returned object is already on the JVM stack; we would need to emit a `CHECKCAST` + conversion factory call. This is deferred to a later phase — phase 1 focuses on eval mode and the Dictionary class itself.

### Registration order in BaseModuleRegister

`Dictionary` must be added to `classToRegister()` **before** any module that might return a `Map` or accept a `Dictionary`. Place it right after `ValueBuffer` in the list.

### Open questions (resolved by this plan)

1. **`keys()` and `values()` return types:** `String[]` and `Object[]` respectively — these are JCP array types (`STRING_ARRAY`, `ANY_ARRAY`) already supported by SystemDataType.
2. **Null keys/values in Map:** Null values pass through as-is (`Dictionary.get()` returns null for missing keys, same as HashMap). Null keys are allowed by HashMap and pass through.
3. **Dictionary iteration:** Out of scope for phase 1. Users access keys via `keys()` and loop manually.
4. **List → ValueBuffer column headers:** Single column with header `"value"`.

## Implementation Phases

### Phase 1: Dictionary class and registration

- [ ] Create `Dictionary.java` in `core/src/main/java/com/elminster/jcp/module/base/dict/Dictionary.java`
- [ ] Methods: no-arg constructor, `put(Object, Object)`, `get(Object)`, `remove(Object)`, `size()`, `containsKey(Object)`, `keys()`, `values()`, `toMap()` (returns backing `HashMap` for Java interop)
- [ ] Add `Dictionary.class` to `BaseModuleRegister.classToRegister()` (after ValueBuffer)
- [ ] Write eval-mode tests: `DictionaryEvalTest` — construct, put, get, remove, size, containsKey, keys, values
- [ ] Write compile-mode tests: `DictionaryCompileTest` — construct, put, get, size (mirror ValueBufferCompileTest pattern)

### Phase 2: Auto-conversion in eval mode (Map ↔ Dictionary)

- [ ] Add `toMap()` method to `Dictionary` — returns the backing `HashMap<Object, Object>` (for outbound conversion)
- [ ] In `ClassConverter.registerStaticMethod` / `registerNonStaticMethod`: after `ReflectUtil.invoke()`, check if `result instanceof java.util.Map` and the returnDataType resolves to a Map-family type or ANY → wrap in `Dictionary`
- [ ] In `ClassConverter.getArgumentValues`: if an argument's `Data.get()` returns a `Dictionary` and the target Java param is `java.util.Map`, unwrap via `toMap()`
- [ ] Write tests: Java helper class with `Map<String, Integer> getScores()` and `void processMap(Map<String, Integer>)` — verify round-trip Dictionary ↔ Map

### Phase 3: Auto-conversion in eval mode (List ↔ ValueBuffer)

- [ ] Add `toList()` method to `ValueBuffer` — extracts first column into a `List<Object>`
- [ ] Add factory method or constructor on `ValueBuffer`: `ValueBuffer.fromList(List<?>)` — creates single-column VB with header `["value"]`
- [ ] In `ClassConverter.registerStaticMethod` / `registerNonStaticMethod`: check if `result instanceof java.util.List` → wrap in `ValueBuffer.fromList()`
- [ ] In `ClassConverter.getArgumentValues`: if argument is `ValueBuffer` and target is `java.util.List`, unwrap via `toList()`
- [ ] Write tests: Java helper class returning `List<String>` and accepting `List<String>` — verify round-trip

### Phase 4: Coverage and edge cases

- [ ] Ensure JaCoCo ≥ 85% instruction and 85% branch coverage on core module
- [ ] Test edge cases: empty Map/List, null values, large collections, Map with non-String keys
- [ ] Test that existing ValueBuffer and module function tests still pass (no regressions)
- [ ] Test module disambiguation: `base::Dictionary.new()` works

## Acceptance Criteria

### Functional Requirements

- [ ] `Dictionary.new()` creates an empty dictionary in both eval and compile modes
- [ ] `put(key, value)`, `get(key)`, `remove(key)`, `size()`, `containsKey(key)`, `keys()`, `values()` all work correctly
- [ ] Java methods returning `java.util.Map` produce a `Dictionary` in eval mode
- [ ] JCP `Dictionary` passed to Java methods expecting `Map` unwraps correctly
- [ ] Java methods returning `java.util.List` produce a `ValueBuffer` in eval mode
- [ ] JCP `ValueBuffer` passed to Java methods expecting `List` unwraps correctly
- [ ] `base::Dictionary.new()` module-qualified form works

### Non-Functional Requirements

- [ ] JaCoCo ≥ 85% instruction and 85% branch coverage maintained
- [ ] No regressions in existing tests (all green)
- [ ] Dictionary backed by HashMap preserves O(1) lookup
- [ ] Auto-conversion adds negligible overhead (single instanceof check per boundary crossing)

## Dependencies

- Existing `ValueBuffer` class and its registration pattern
- `BaseModuleRegister.classToRegister()` ordering mechanism
- `ClassConverter` (eval) and `CompileModeClassConverter` (compile) registration infrastructure
- `ExternalClassType` for Dictionary's type representation in both modes

## Known Learnings Applied

- `docs/solutions/runtime-errors/50-type-bridge-isolation-report.md` — **D5 (mapJavaTypeToDataType ANY-collapse)** is relevant: when `Dictionary` methods have `Object` parameters, they'll map to `ANY`, which is correct and expected. The D5 fix (for `char`/`long`/`float` etc.) is orthogonal but the same code path runs. The auto-conversion logic must be careful NOT to interfere with the `mapJavaTypeToDataType` pipeline — conversion happens at the value level (runtime), not the type-mapping level (registration time).

- `docs/solutions/runtime-errors/50-type-bridge-isolation-report.md` — **D6 (ClassConverter NPE on unknown param types)** reminds us that `Dictionary`'s `Object`-typed parameters will go through `getDataType(Object.class, ...)` which correctly returns `SystemDataType.ANY` via the fast-path. No risk here, but the pattern confirms that `Object`-param methods are safe to register.

- The overload resolution memory (issue #43 / PR #63) is relevant: `Dictionary.get(Object)` and `Dictionary.put(Object, Object)` use `ANY`-typed parameters, so there's no overload ambiguity risk. However, if future phases add typed overloads (e.g., `get(String)`), the same `allExactMatch` preference must apply.

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Auto-conversion wrapping breaks existing module functions that intentionally return raw Map/List | Medium | High | Only convert when the return type maps to ANY or a Map/List ExternalClassType — not when the function explicitly declares a different return type. Gate behind `instanceof` check on the runtime value, not the declared type. |
| Dictionary registration order causes circular dependency during `BaseModuleRegister` scan | Low | Medium | Dictionary has no dependencies on other base-module types. Its methods take/return `Object`, which maps to `ANY`. Place it after `ValueBuffer` but before modules that might use it. |
| Compile-mode auto-conversion (deferred) requires complex bytecode emission | Medium | Low | Explicitly deferred to a future phase. Phase 1-3 focus on eval mode and the Dictionary class itself, which works in compile mode as a plain ExternalClassType. |
| ValueBuffer.toList() loses multi-column structure | Low | Medium | Document that `toList()` extracts the first column only. Users with multi-column VBs should access rows directly. |
| JaCoCo coverage drops below 85% with new untested branches | Medium | High | Write tests for every method and conversion path. Phase 4 is dedicated to coverage and edge cases. |
