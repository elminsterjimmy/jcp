---
title: "refactor: determine Literal type at Literal.of() creation time"
type: refactor
date: 2026-03-10
issue: 38
---

# refactor: determine Literal type at Literal.of() creation time

## Overview

Currently `Literal.of(value)` always produces a `GenericLiteral<T>`, deferring type
determination to `LiteralCompiler.resolveType()` and to the `else` branch of
`compileLiteral()` at compile time.  This means the compiler must perform
runtime type-inspection (`instanceof Integer`, `instanceof Double`, …) every
time it processes a literal, and `null` can only be typed as `ANY`.

The fix is simple: make `Literal.of(value)` a type-dispatch factory that returns
the most-specific typed subclass (`IntLiteral`, `DoubleLiteral`, `BooleanLiteral`,
`StringLiteral`), falling back to `GenericLiteral` only for truly unknown types.
A parallel `Literal.ofNull()` (or a dedicated `NullLiteral` subclass) removes the
`ANY`-typed null ambiguity.

After the change:
- `Literal.of(42)` → `IntLiteral`
- `Literal.of(3.14)` → `DoubleLiteral`
- `Literal.of(true)` → `BooleanLiteral`
- `Literal.of("hi")` → `StringLiteral`
- `Literal.of(null)` → `NullLiteral` (compiles as `ACONST_NULL`, type `ANY`)
- `Literal.of(someUnknown)` → `GenericLiteral` (unchanged behaviour)

`LiteralCompiler` can then remove the `else` generic-inspection branch and
`resolveType()` can remove its duplicate `instanceof` checks, since the subclass
alone carries all necessary information.

## Technical Approach

### 1. Add `NullLiteral` subclass

```java
public class NullLiteral extends Literal<Void> {
    public static final NullLiteral INSTANCE = new NullLiteral();
    private NullLiteral() { super(null); }
    @Override public String getName() { return "NullLiteral"; }
}
```

A singleton is fine because null literals carry no value.

### 2. Update `Literal.of()` to dispatch

```java
public static <T> Literal<T> of(T value) {
    if (value == null)          return (Literal<T>) NullLiteral.INSTANCE;
    if (value instanceof Integer) return (Literal<T>) IntLiteral.of((Integer) value);
    if (value instanceof Double)  return (Literal<T>) DoubleLiteral.of((Double) value);
    if (value instanceof Boolean) return (Literal<T>) BooleanLiteral.of((Boolean) value);
    if (value instanceof String)  return (Literal<T>) StringLiteral.of((String) value);
    return GenericLiteral.of(value);   // safety net – should not be reached in practice
}
```

### 3. Simplify `LiteralCompiler`

Remove the entire `else` branch in `compileLiteral()`:

```java
private void compileLiteral(Literal literal, MethodVisitor mv) {
    if (literal instanceof IntLiteral)     compileInt(((IntLiteral) literal).getValue(), mv);
    else if (literal instanceof DoubleLiteral)  compileDouble(((DoubleLiteral) literal).getValue(), mv);
    else if (literal instanceof BooleanLiteral) compileBoolean(((BooleanLiteral) literal).getValue(), mv);
    else if (literal instanceof StringLiteral)  compileString(((StringLiteral) literal).getValue(), mv);
    else if (literal instanceof NullLiteral)    mv.visitInsn(Opcodes.ACONST_NULL);
    else throw new UnsupportedOperationException(
            "Unsupported literal type: " + literal.getClass().getSimpleName());
}
```

`resolveType()` gains a single `NullLiteral` arm and drops the value-inspection
`if`-chain:

```java
@Override
public DataType resolveType(CompileContext ctx) {
    Literal literal = ((LiteralExpression) astNode).getLiteral();
    if (literal instanceof IntLiteral)     return SystemDataType.INT;
    if (literal instanceof DoubleLiteral)  return SystemDataType.DOUBLE;
    if (literal instanceof BooleanLiteral) return SystemDataType.BOOLEAN;
    if (literal instanceof StringLiteral)  return SystemDataType.STRING;
    if (literal instanceof NullLiteral)    return SystemDataType.ANY;
    throw new CompileException("unsupported literal type: " + literal.getClass().getName());
}
```

### 4. Update `LiteralEvaluator` (minor)

Add a `NullLiteral` arm that returns `NullData` (or equivalent), remove the
generic-fallback path that calls `DataFactory.createConstValue(null, …)`.

### 5. `GenericLiteral` / `LiteralExpression`

`GenericLiteral` is kept as a safety-net subclass for extension by library users
(no behaviour change).  `LiteralExpression.of(T value)` already delegates to
`Literal.of(value)` so it requires no change.

## Implementation Phases

### Phase 1: Add `NullLiteral`

- [ ] Create `NullLiteral.java` in `ast/expression/literal/`
- [ ] Singleton with `getName()` → `"NullLiteral"`

### Phase 2: Update `Literal.of()` dispatch

- [ ] Add dispatching `if`-chain in `Literal.of()`
- [ ] Keep `GenericLiteral.of()` as the final fallback

### Phase 3: Simplify `LiteralCompiler`

- [ ] Remove `else` generic-inspection branch in `compileLiteral()`
- [ ] Add `NullLiteral` arm
- [ ] Remove value-inspection `if`-chain from `resolveType()`
- [ ] Add `NullLiteral` → `SystemDataType.ANY` arm in `resolveType()`

### Phase 4: Update `LiteralEvaluator`

- [ ] Add `NullLiteral` arm returning appropriate `Data` object
- [ ] Remove generic `DataFactory.createConstValue(null, …)` fallback

### Phase 5: Tests

- [ ] Unit test: `Literal.of(42)` returns `IntLiteral`
- [ ] Unit test: `Literal.of(3.14)` returns `DoubleLiteral`
- [ ] Unit test: `Literal.of(true)` returns `BooleanLiteral`
- [ ] Unit test: `Literal.of("x")` returns `StringLiteral`
- [ ] Unit test: `Literal.of(null)` returns `NullLiteral`
- [ ] Compiler test: null literal compiles to `ACONST_NULL` and resolves as `ANY`
- [ ] Eval test: null literal evaluates without NPE
- [ ] Verify existing `LiteralCompilerTest` suite still passes (no `GenericLiteral` regressions)

## Acceptance Criteria

### Functional Requirements

- [ ] `Literal.of(Integer)` returns `IntLiteral`
- [ ] `Literal.of(Double)` returns `DoubleLiteral`
- [ ] `Literal.of(Boolean)` returns `BooleanLiteral`
- [ ] `Literal.of(String)` returns `StringLiteral`
- [ ] `Literal.of(null)` returns `NullLiteral` (not `GenericLiteral<null>`)
- [ ] Null literal compiles to `ACONST_NULL` bytecode instruction
- [ ] Null literal `resolveType()` returns `SystemDataType.ANY`
- [ ] All existing literal tests pass without modification
- [ ] `LiteralEvaluator` handles `NullLiteral` without NPE

### Non-Functional Requirements

- [ ] `LiteralCompiler.compileLiteral()` has no generic value-inspection branch
- [ ] `LiteralCompiler.resolveType()` has no generic value-inspection branch
- [ ] `GenericLiteral` is retained as a fallback extension point (not removed)
- [ ] Core module JaCoCo coverage stays ≥ 80% instruction and ≥ 80% branch

## Dependencies

- None — this is a self-contained refactor within `ast/expression/literal/`,
  `compile/base/LiteralCompiler`, and `eval/base/LiteralEvaluator`.

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Unchecked cast warning in `Literal.of()` | H | L | Add `@SuppressWarnings("unchecked")` with comment explaining the dispatch is type-safe |
| DSL user called `GenericLiteral.of(null)` directly | L | L | `GenericLiteral` is still present; null dispatch now goes through `NullLiteral` from `Literal.of()` only |
| `LiteralEvaluator` null handling unclear | M | M | Check how `DataFactory.createConstValue(null, …)` behaves; provide explicit `NullData` or equivalent |
| Coverage drop from removing the generic-fallback branch | L | M | The removed branches were effectively dead code once dispatch is in `Literal.of()`; new `NullLiteral` tests cover the new path |
