# Issue #55: Custom Comparator Sort via SortKey.sortByFunc()

## Context

JCP users can sort struct arrays by field path (`SortKey.by("age")`), but cannot sort by
**computed comparison** (e.g. age difference, product of fields). Issue #55 asks for custom
comparator support.

**Approach**: Extend `SortKey` with `sortByFunc(String functionName)`. The user declares a
comparison function and passes its name as a string. At sort-time, the runtime resolves and
invokes that function for each comparison. No new AST nodes, no new types, no language changes.

## User-Facing API

```
func myCompare(a: Any, b: Any) -> int {
    return a.age - b.age
}

let k = SortKey.sortByFunc("myCompare")
Arrays.sort(persons, k)
```

Uses the existing `Arrays.sort(Object[], SortKey)` overload — no new overload needed.

---

## Design: Two-Mode Resolution

`SortKey` gains a second internal mode: **field-path** (existing) vs **function-name** (new).

### The Challenge

`SortKey.toComparator()` runs as plain Java. In eval mode, `EvalContext` holds the named
function but isn't accessible from `SortKey`. In compile mode, the named function is a
`public static` method on the generated class, but `SortKey` can't see that class (different
classloader).

### Solution

**Eval mode**: Intercept `SortKey` arguments in `ClassConverter.doFunction()` — where
`EvalContext` is available — and pre-resolve the function into a `Comparator<Object>` before
the reflective call to `Arrays.sort`.

**Compile mode**: `SortKey.sortByFunc(String, Class<?>)` two-arg factory takes the declaring
class. `FunCallCompiler` detects the one-arg `SortKey.sortByFunc("name")` pattern and emits
the two-arg version with `LDC` for the current class. At runtime, `toComparator()` uses
reflection on the provided `Class<?>` to invoke the method.

---

## Modified Files

### 1. `core/.../module/base/arrays/SortKey.java`

Add function-name mode alongside existing field-path mode:

**New fields:**
- `String functionName` — null for field-path mode
- `Class<?> declaringClass` — the class containing the named function (compile mode)
- `Comparator<Object> resolvedComparator` — pre-built comparator (eval mode)

**New factories:**
- `sortByFunc(String functionName)` — eval mode (resolved later via `EvalContext`)
- `sortByFunc(String functionName, Class<?> declaringClass)` — compile mode

**New methods:**
- `getFunctionName()` — returns function name or null
- `withResolvedComparator(Comparator<Object>)` — returns copy with pre-built comparator

**`toComparator()` changes:**
1. If `resolvedComparator != null` → return it (eval mode, pre-resolved)
2. If `functionName != null && declaringClass != null` → resolve via reflection:
   `declaringClass.getMethod(functionName, Object.class, Object.class)`, wrap in `Comparator`
3. If `functionName != null && declaringClass == null` → throw `IllegalStateException`
4. Otherwise → existing field-path logic (unchanged)

### 2. `core/.../util/ClassConverter.java`

In `convertArgumentsForJava()` (line ~196), add SortKey function-name resolution alongside
the existing `Dictionary→Map` / `ValueBuffer→List` conversions:

```java
if (val instanceof SortKey && ((SortKey) val).getFunctionName() != null) {
    // Look up named function from evalContext, build Comparator adapter
    // Replace argValues[i] with sortKey.withResolvedComparator(adapter)
}
```

`convertArgumentsForJava` currently doesn't have access to `EvalContext`. Add a new overload:
`convertArgumentsForJava(Data[] arguments, Method method, EvalContext evalContext)`.
The `doFunction` in `registerStaticMethod` (line 161) already has `evalContext` — pass it through.

The comparator adapter wraps function invocation using the same pattern as `FunctionEvaluator`:
- Wrap `a`, `b` as `AnyData(SystemDataType.ANY)`
- Call `function.setArguments(...)` then `AstEvaluatorFactory.getEvaluator(function).eval(evalContext)`
- Unwrap: `((Number) result.get()).intValue()`

### 3. `core/.../compile/function/FunCallCompiler.java`

In `compileModuleFunctionCall()`, add a targeted pattern match after discovering the method:

When the resolved method is `SortKey.sortByFunc(String)`:
- Instead of emitting `INVOKESTATIC SortKey.sortByFunc(String)`, emit:
  - Compile the string argument (already on stack)
  - `LDC Type.getType("L" + className + ";")` → pushes `Class<?>` reference
  - `INVOKESTATIC SortKey.sortByFunc(String, Class)` → two-arg version

This is a tightly scoped pattern match — only applies to this one method.

### 4. Cleanup

- Delete `core/.../eval/data/FunctionType.java` (created in rejected approach)

---

## New Test Files

### `core/.../eval/function/ArraysSortByFuncTest.java`

Eval-mode integration tests (mirrors `ArraysSortStructTest` pattern):
- Sort struct array descending by age via comparator function
- Sort with multiple SortKeys mixing field-path and function-name modes
- Unknown function name → exception
- Wrong function arity → exception

### `core/.../compile/function/ArraysSortByFuncCompileTest.java`

Compile-mode tests:
- Sort struct array by compiled comparator function (end-to-end `JcpCompiler`)
- Verify `SortKey.sortByFunc(String, Class)` resolves correctly at runtime

---

## End-to-End Flows

### Eval Mode
1. User declares `func myCompare(a: Any, b: Any) -> int { ... }`
2. `SortKey.sortByFunc("myCompare")` → SortKey with `functionName = "myCompare"`
3. `Arrays.sort(persons, k)` dispatches to auto-registered module function
4. `doFunction(args, evalContext)` → `convertArgumentsForJava` detects SortKey function name
5. Looks up `"myCompare"` in `evalContext.getFunctions()` → builds Comparator adapter
6. Replaces arg with `sortKey.withResolvedComparator(adapter)`
7. `ReflectUtil.invoke` → `Arrays.sort(arr, resolvedKey)` → `toComparator()` returns adapter
8. Each comparison invokes the JCP function body via `FunctionEvaluator`

### Compile Mode
1. `func myCompare(a: Any, b: Any) -> int` compiles as `public static int myCompare(Object, Object)`
2. `SortKey.sortByFunc("myCompare")` → compiler emits two-arg `sortByFunc("myCompare", MyClass.class)`
3. `Arrays.sort(persons, k)` → `INVOKESTATIC Arrays.sort(Object[], SortKey)`
4. Runtime: `toComparator()` → reflectively finds `MyClass.myCompare(Object, Object)` → wraps in Comparator

---

## Verification

```bash
mvn verify -pl core
# ≥ 85% instruction and branch coverage
```

## Risks

| Risk | Mitigation |
|------|------------|
| Reflection perf in compile mode | Method resolved once, cached in Comparator lambda — not per comparison |
| Function name/arity mismatch | Eval: validated in interceptor. Compile: `NoSuchMethodException` with clear message |
| Breaks existing field-path sorts | New fields default to null; existing `toComparator()` path unchanged; all existing tests must pass |
| FunCallCompiler special case | Tightly scoped; only matches `SortKey.sortByFunc(String)` |
| Classloader visibility | `declaringClass` passed at construction — no cross-classloader lookup |
